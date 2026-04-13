# oceanlink-order-client 实现方案

## 当前方案

- 使用 Java `record` 表达不可变命令和 DTO
- 首批仅保留最小字段，避免过早固化接口

## 后续方案

- 与 `selling-partner-api-models` 对齐字段命名
- 根据 app/domain 实际演进扩展 DTO
