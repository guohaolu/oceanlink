package org.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务三方仓库库龄报表实体类
 * 对应数据库表：finance_tp_warehouse_age_t
 *
 * @author guohao.lu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "finance_tp_warehouse_age_t")
public class FinanceTpWarehouseAgeEntity {
    /**
     * 报表日期
     */
    private LocalDate reportDate;

    /**
     * 三方仓服务商名称，来自wms，表：wms_warehouse
     */
    private String tripartiteProviderName;

    /**
     * 三方仓编码，来自wms，表：wms_warehouse
     */
    private String tripartiteWhCode;

    /**
     * 三方仓SKU编码，来自openapi
     */
    private String tripartiteSkuCode;

    /**
     * 在库数量，默认值：0
     */
    private Integer stockQuantity;

    /**
     * 在途数量，默认值：0
     */
    private Integer inTransitQuantity;

    /**
     * 库龄，默认值：0
     */
    private Integer age;

    /**
     * 创建人账号，审计字段，系统自动填充，默认值：sysDefaultUser
     */
    private String createBy;

    /**
     * 创建人姓名，审计字段，系统自动填充，默认值：系统默认用户
     */
    private String createByName;

//    /**
//     * 创建时间，审计字段，系统自动填充
//     */
//    @TableField(insertStrategy = FieldStrategy.NEVER)
//    private LocalDateTime createTime;

    /**
     * 更新人账号，审计字段，系统自动填充，默认值：sysDefaultUser
     */
    private String updateBy;

    /**
     * 更新人姓名，审计字段，系统自动填充，默认值：系统默认用户
     */
    private String updateByName;

//    /**
//     * 更新时间，审计字段，系统自动填充
//     */
//    @TableField(insertStrategy = FieldStrategy.NEVER)
//    private LocalDateTime updateTime;
}
