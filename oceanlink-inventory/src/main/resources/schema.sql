-- 库存主表（文档模型：sq/wq/oq/lq）
-- sq 可售库存、wq 预扣库存、oq 占用库存、lq 预锁库存（Redis 扣减计数来源）
CREATE TABLE IF NOT EXISTS `inventory` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `sku_id`       BIGINT   NOT NULL COMMENT '商品 SKU',
    `sq`           INT      NOT NULL DEFAULT 0 COMMENT '可售库存',
    `wq`           INT      NOT NULL DEFAULT 0 COMMENT '预扣库存',
    `oq`           INT      NOT NULL DEFAULT 0 COMMENT '占用库存',
    `lq`           INT      NOT NULL DEFAULT 0 COMMENT '预锁库存（用于 Redis 分桶扣减）',
    `version`      INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `gmt_create`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存主表';

CREATE TABLE IF NOT EXISTS `inventory_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `sku_id`      BIGINT       NOT NULL,
    `deduct_qty`  INT          NOT NULL COMMENT '扣减数量',
    `order_id`    VARCHAR(64)  DEFAULT NULL COMMENT '业务单号',
    `biz_type`    VARCHAR(32)  DEFAULT 'ORDER',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS',
    `gmt_create`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存扣减流水';
