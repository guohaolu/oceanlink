package org.example.pojo.dto;

import lombok.Data;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 数据字典项
 * 用于表示系统中数据字典的具体项，包含字典项的基本信息和配置
 *
 * @author guohao.lu
 */
@Data
public class SysDictItemDTO implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典项主键ID
     */
    private Long id;

    /**
     * 所属字典ID
     */
    private Long dictId;

    /**
     * 字典项值
     */
    private String itemValue;

    /**
     * 字典项标签名称
     */
    private String label;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典项描述信息
     */
    private String description;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
