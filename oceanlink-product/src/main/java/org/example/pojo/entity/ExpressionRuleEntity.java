package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "expression_rule", autoResultMap = true)
public class ExpressionRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String ruleName;
    private String reportType;

    private Integer priority;

    /**
     * 命中结果配置
     * 例如：{"category1": "电子", "category2": "手机", "tags": ["新品", "促销"]}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> resultConfig;

    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
