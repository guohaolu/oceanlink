package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("expression_rule_node")
public class ExpressionRuleNodeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private Long parentId;
    private String nodeType; // LOGICAL, PREDICATE
    private String operator; // AND, OR, EQ, etc.
    private String fieldName;
    private String valueType; // LITERAL, REFERENCE
    private String valueContent;
    private Integer sortOrder;
}
