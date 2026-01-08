# 基于JSR-354的货币与金额系统设计

好，这里我不再复述你已经写得**非常扎实**的 JSR-354 原理解析，而是**在你这篇材料之上，给你一套“可落地的系统设计”**。目标很明确：

> **在跨境电商 + 虚拟货币（积分/钻石/鼓励金）场景下，构建一套可扩展、可演进、可治理的货币与金额系统。**

下面这套设计，你可以直接作为 **系统设计章节 / 技术方案** 使用。

---

# 一、总体设计目标（System Goals）

1. **统一货币抽象**

    * 法币（CNY / USD / EUR）
    * 虚拟货币（积分、钻石、鼓励金）
    * 内部清结算币（记账币、结算币）

2. **金额语义不丢失**

    * 金额 ≠ BigDecimal
    * 金额 = 数值 + 货币 + 精度 + 上下文

3. **跨域解耦**

    * 业务系统不关心汇率来源
    * 不关心虚拟币是否“能兑换法币”

4. **动态可配置**

    * 新货币无需发版
    * 汇率可动态加载 / 回溯历史

5. **金融级安全**

    * 精度可控
    * 舍入策略可审计
    * 转换路径可追踪

---

# 二、系统整体架构

```
┌────────────────────────────────────┐
│           业务系统层                │
│                                    │
│  订单 / 支付 / 结算 / 账户 / 报表   │
│        （只用 Money API）           │
└───────────────▲────────────────────┘
                │
┌───────────────┴────────────────────┐
│        Monetary Facade 层            │
│  MoneyService / ConversionService   │
│  RoundingPolicy / PricingPolicy     │
└───────────────▲────────────────────┘
                │
┌───────────────┴────────────────────┐
│         JavaMoney(JSR-354)           │
│   CurrencyUnit / MonetaryAmount     │
│   CurrencyConversion / Rounding     │
│   Format                            │
└───────────────▲────────────────────┘
                │ SPI
┌───────────────┴────────────────────┐
│      扩展实现层（你的核心）         │
│                                    │
│  CurrencyProviderSpi                │
│  ExchangeRateProvider               │
│  RoundingProviderSpi                │
│  MonetaryAmountFactoryProviderSpi   │
└───────────────▲────────────────────┘
                │
┌───────────────┴────────────────────┐
│        数据加载 & 治理层             │
│  DB / Config / HTTP / MQ / Cache    │
│  版本 / 生效期 / 回滚               │
└────────────────────────────────────┘
```

**关键思想：**

> JSR-354 是“货币计算内核”，
> 你要做的是**治理、约束、编排它**。

---

# 三、核心设计拆解

## 3.1 CurrencyUnit 设计（货币建模）

### 设计原则

* **所有币种一视同仁**
* 法币 / 虚拟币 / 内部币 → 都是 `CurrencyUnit`
* 禁止业务直接用字符串 `"CNY"` / `"VBE"`

### 货币分类（推荐元数据）

```json
{
  "currencyCode": "VBE",
  "category": "VIRTUAL",
  "exchangeable": true,
  "settleable": false,
  "defaultFractionDigits": 2
}
```

| 字段                  | 说明                        |
| ------------------- | ------------------------- |
| category            | FIAT / VIRTUAL / INTERNAL |
| exchangeable        | 是否允许兑换                    |
| settleable          | 是否可结算                     |
| validFrom / validTo | 生效期                       |

➡️ **这些不是 JSR-354 的事，是你的治理层的事**

---

## 3.2 MonetaryAmount 选型策略

| 场景           | 实现             |
| ------------ | -------------- |
| 支付 / 结算 / 对账 | `Money`        |
| 报表 / 聚合      | `FastMoney`    |
| 计费引擎         | `RoundedMoney` |

统一约束：

* **禁止直接 new BigDecimal**
* 强制使用工厂 + Context

```java
Money amount = Money.of(
    100,
    "CNY",
    MonetaryContextBuilder.of()
        .set(MathContext.DECIMAL128)
        .build()
);
```

---

## 3.3 汇率系统设计（核心）

### 汇率 ≠ 一个数字

你至少要支持：

* 多 Provider
* 多日期
* 多用途

### ExchangeRateProvider 分层

```
┌─────────────────────────┐
│ BusinessRateProvider    │  ← 虚拟币 / 内部定价
├─────────────────────────┤
│ FinanceRateProvider     │  ← 财务清算汇率
├─────────────────────────┤
│ MarketRateProvider      │  ← ECB / 外部
└─────────────────────────┘
```

通过 **CompoundRateProvider** 组合。

### ConversionQuery 扩展

```java
ConversionQueryBuilder.of()
  .setBaseCurrency("VBE")
  .setTermCurrency("CNY")
  .set("purpose", "RECHARGE")
  .set("date", LocalDate.now())
```

---

## 3.4 舍入与精度策略（金融红线）

**舍入不是技术问题，是制度问题。**

你至少要支持：

* 币种舍入
* 业务舍入
* 场景舍入

```java
@Priority(100)
class VirtualCurrencyRoundingProvider
```

规则示例：

| 场景 | 规则        |
| -- | --------- |
| 充值 | 向下取整      |
| 退款 | 原路精度      |
| 清算 | HALF_EVEN |

---

## 3.5 虚拟货币与法币的边界

**最重要的一条：**

> ❌ 虚拟币 ≠ 法币
> ✅ 虚拟币 = 可定价资产

所以：

* 不允许任意双向兑换
* 汇率是**单向的**
* 可能有“锚定币”（如 CNY）

```text
VBE -> CNY ✅
CNY -> VBE ❌（除非业务允许）
```

---

# 四、数据加载与治理设计

你已经写了 Moneta 的加载机制，这里补上 **企业级治理层**：

### 数据来源

| 数据   | 来源          |
| ---- | ----------- |
| 货币定义 | DB / Config |
| 汇率   | DB + MQ     |
| 生效期  | DB          |
| 历史   | 归档表         |

### 推荐策略

* NEVER：法币定义
* ONSTARTUP：虚拟币
* SCHEDULED：汇率
* LAZY：非核心币种

---

# 五、典型业务落地示例

## 充值校验（你给的例子，工程化版）

```java
Money recharge = Money.of(100, "VBE");
Money paid = Money.of(1, "VZU");

Money converted = paid.with(
    conversionService.forPurpose("RECHARGE")
                     .to("VBE")
);

assert recharge.isEqualTo(converted);
```

**注意：**

* 业务不关心汇率来源
* 不关心精度
* 不关心舍入

---

# 六、你这套设计的真正价值

你现在做的不是：

> “支持多币种”

而是：

1. 把**货币从字符串提升为领域模型**
2. 把**金额计算从工具方法提升为制度**
3. 把**虚拟货币从特例拉回统一抽象**
4. 为跨境、电商、财务、审计留出空间

这是**金融系统成熟度的分水岭设计**。

---

## 下一步如果你愿意，我们可以继续拆：

* Money 在 DB 中如何持久化（金额 + 币种 + 精度）
* 如何做金额的**幂等校验**
* 汇率变更如何影响历史账务
* 如何避免“积分=人民币”的合规风险
* 如何在 ClickHouse / 数仓中重建 MonetaryAmount

这套东西，一旦打稳，后面十年都不塌。
