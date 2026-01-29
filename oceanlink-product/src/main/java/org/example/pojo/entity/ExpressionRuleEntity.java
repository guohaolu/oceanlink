package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("expression_rule")
public class ExpressionRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String ruleName;
    private String reportType;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
