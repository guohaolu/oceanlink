# 基于gitlab的平台级流水线

这是一个**非常标准、但又很容易被“半吊子流水线”坑惨的企业级场景**。你问的不是“怎么配 CI”，而是：

> **如何把“代码质量 → 合并安全 → 发版可靠 → 可观测反馈”串成一条不会骗人的链路**

我们来把这件事拆成**可验证的工程系统**，而不是 YAML 魔法。

---

## 一、先给你一张「整体设计蓝图」（心智模型）

你要的不是一个流水线，而是 **三段式控制系统**：

```
MR 提交阶段（质量闸门）
        ↓
MR 合并阶段（唯一发版入口）
        ↓
发布后验证阶段（运行态真相）
```

每一段都解决一个核心问题：

| 阶段    | 解决什么           |
| ----- | -------------- |
| MR 提交 | 这代码“值不值得被合并”   |
| MR 合并 | “只允许一次、确定性的发版” |
| 发布后   | “它真的活着吗，还是假启动” |

---

## 二、MR 提交阶段：**质量闸门（Hit Check）**

目标只有一句话：

> **不允许“我本地没问题”的代码进入主干**

### 1️⃣ 触发条件

```yaml
rules:
  - if: $CI_PIPELINE_SOURCE == "merge_request_event"
```

只在 **MR 创建 / 更新** 时触发。

---

### 2️⃣ Hit Check 你要做哪些（强烈推荐这四类）

#### ✅ ① 是否基于最新目标分支

这是很多团队忽略的“隐性炸弹”。

做法：

```bash
git fetch origin target-branch
git merge-base --is-ancestor origin/target-branch HEAD
```

失败就直接 **CI fail**，提示：

> “请 rebase 到最新 main 后再提交 MR”

---

#### ✅ ② Maven 编译 & 单测

```bash
mvn clean verify -DskipTests=false
```

不要跳测试。
跳测试的流水线等于“装饰品”。

---

#### ✅ ③ 代码规范校验（必须 fail pipeline）

推荐组合：

* `maven-checkstyle-plugin`
* `spotbugs-maven-plugin`
* `pmd-maven-plugin`

策略建议：

> **MR 阶段严格，主干阶段宽松**

也就是：

* MR：违规 = 失败
* main：违规 = warning

---

#### ✅ ④ 静态依赖 / 风险扫描（可选但很值）

比如：

* 依赖冲突
* 已知漏洞（OWASP Dependency Check）

---

### 3️⃣ MR 阶段的本质

这是一个**拒绝系统，不是通过系统**。

> 能 fail 的地方，越早 fail 越好。

---

## 三、MR 合并阶段：**唯一发版入口**

这是整个设计中最重要的一条原则：

> **禁止任何“手动触发发版”**

### 1️⃣ 触发条件（只认 main）

```yaml
rules:
  - if: $CI_COMMIT_BRANCH == "main"
```

并且：

* main 分支 **禁止直接 push**
* 只能 MR merge

这样你天然获得：

> **“每一次发版 = 一次 MR”**

---

### 2️⃣ Maven 发版策略（推荐）

不要用 `mvn release:prepare` 那套老古董。

推荐：

* **语义版本号由 CI 生成**
* 基于：

    * Git tag
    * 或 MR label（feat / fix / hotfix）

例如：

```bash
VERSION=1.2.${CI_PIPELINE_ID}
mvn clean package -Drevision=$VERSION
```

---

### 3️⃣ 产物管理

* Jar / Docker Image
* 推送到：

    * Nexus / Artifactory
    * 或 Docker Registry

这一步 **只要构建成功即可**
**不要在这一步启动服务**

---

## 四、发布阶段：Spring 启动 ≠ 成功（这是重点）

你已经点到关键痛点了 👍

> **Spring Boot 能启动 ≠ 服务是健康的**

### 1️⃣ 启动 ≠ 可用 的原因

* 数据库连不上但没 fail fast
* MQ consumer 起了但订阅失败
* 配置中心没拉到正确配置
* 端口监听成功但业务依赖没 ready

---

### 2️⃣ 正确的发布模型（两阶段）

```
阶段一：部署 & 启动
阶段二：健康验证
```

#### 启动阶段

* Kubernetes / Docker / VM
* 不关心日志，只关心**是否退出**

---

#### 健康检查阶段（必须独立）

你需要 **显式探测**：

```http
GET /actuator/health
```

并且：

* 必须返回 `UP`
* 且包含你关心的子系统：

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "mq": { "status": "UP" }
  }
}
```

---

### 3️⃣ 健康检查实现建议（非常关键）

* Spring Boot：

    * `spring-boot-starter-actuator`
    * 自定义 `HealthIndicator`

* CI 中轮询：

```bash
for i in {1..30}; do
  curl -sf http://service/actuator/health && exit 0
  sleep 10
done
exit 1
```

> **启动失败要 fail pipeline**

---

## 五、飞书机器人通知（成功 / 失败都要）

这是你“组织感知”的出口。

### 1️⃣ 通知时机

你需要 **三个关键节点**：

1. MR 校验失败
2. 发版成功 + 健康 OK
3. 发版失败（构建 / 启动 / 健康）

---

### 2️⃣ 消息内容设计（不要只发“成功了”）

建议结构：

```
【🚀 服务发版结果】

应用：xxx-service
版本：1.2.345
提交人：@张三
MR：!123

状态：
- 构建：✅
- 部署：✅
- 健康检查：❌（DB DOWN）

流水线：
👉 点击查看
```

---

### 3️⃣ @ 对应的人怎么来？

推荐来源顺序：

1. MR 作者
2. commit 作者
3. Code Owner（CODEOWNERS）

在 GitLab CI 里你能拿到：

```bash
$GITLAB_USER_EMAIL
$GITLAB_USER_NAME
```

飞书支持 `@email`，直接映射即可。

---

## 六、GitLab Runner 设计建议（很重要）

### Runner 类型

* **Shell / Docker Executor**
* 独立 Runner（不要共用）

### 隔离原则

* 构建 Runner ≠ 部署 Runner
* 构建环境不应该能直接 ssh 生产

---

## 七、这套设计的“工程价值”

你最终得到的是：

* ❌ 不可能 merge 一个不能编译的 MR
* ❌ 不可能发一个没通过健康检查的版本
* ❌ 不可能“我以为发了，其实没起来”
* ✅ 每一次发版都可追溯到 MR
* ✅ 出问题时，责任人自动被 @

---

## 最后一句点醒你

> **CI 解决的是“我写得对不对”
> CD 解决的是“系统有没有在骗人”**

你这个场景已经不是“小公司流水线”，而是**系统可靠性工程**了。

如果你愿意，下一步我可以直接帮你：

* 给一份 **GitLab CI YAML 结构骨架**
* 或把它拆成 **“平台级流水线模板”**，供所有 Spring 服务复用
