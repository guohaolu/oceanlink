CREATE TABLE `expression_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` VARCHAR(50) NOT NULL COMMENT '租户ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `report_type` VARCHAR(50) NOT NULL COMMENT '报表类型/业务类型',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数值越大越优先匹配）',
    `result_config` JSON DEFAULT NULL COMMENT '命中后的结果配置：支持JSON对象、列表或简单值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_report` (`tenant_id`, `report_type`, `priority` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表达式规则定义表';

CREATE TABLE `expression_rule_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '所属规则ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父节点ID (逻辑节点ID)',
    `node_type` VARCHAR(20) NOT NULL COMMENT '节点类型：LOGICAL(逻辑), PREDICATE(谓词/条件)',
    `operator` VARCHAR(20) NOT NULL COMMENT '操作符：LOGICAL(AND, OR, NOT); PREDICATE(EQ, LIKE, IN, etc.)',
    `field_name` VARCHAR(50) DEFAULT NULL COMMENT '字段名 (条件节点使用)',
    `value_type` VARCHAR(20) DEFAULT NULL COMMENT '值类型：LITERAL(字面量), REFERENCE(引用量)',
    `value_content` TEXT DEFAULT NULL COMMENT '值内容',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_parent` (`rule_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表达式规则节点明细表';