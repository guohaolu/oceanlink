-- auto-generated definition
create table finance_tp_warehouse_age_t
(
    report_date              Date                   default now() comment '报表日期',
    tripartite_provider_name LowCardinality(String) default '' comment '三方仓服务商名称',
    tripartite_wh_code       LowCardinality(String) default '' comment '三方仓编码',
    tripartite_sku_code      String comment '三方仓SKU编码',
    stock_quantity           UInt32                 default 0 comment '在库数量',
    in_transit_quantity      UInt32                 default 0 comment '在途数量',
    age                      UInt32                 default 0 comment '库龄',
    create_by                LowCardinality(String) default 'sysDefaultUser' comment '创建人账号，审计字段，系统自动填充',
    create_by_name           LowCardinality(String) default '系统默认用户' comment '创建人姓名，审计字段，系统自动填充',
    create_time              DateTime64(6)          default now64(6) comment '创建时间，审计字段，系统自动填充',
    update_by                LowCardinality(String) default 'sysDefaultUser' comment '更新人账号，审计字段，系统自动填充',
    update_by_name           LowCardinality(String) default '系统默认用户' comment '更新人姓名，审计字段，系统自动填充',
    update_time              DateTime64(6)          default now64(6) comment '更新时间，审计字段，系统自动填充'
)
    engine = MergeTree PARTITION BY toYYYYMM(report_date)
    ORDER BY (report_date, tripartite_provider_name, tripartite_wh_code)
    SETTINGS index_granularity = 8192;

