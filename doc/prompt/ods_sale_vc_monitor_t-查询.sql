WITH
    #{query.currency} AS selected_currency,
    formatDateTime(today(), '%Y-%m-%d') AS today_str,

        exchange_rates AS (
        SELECT
        month,
        currency_code,
        coalesce(average_median_price, 1) as rate
        FROM sys_currency_rate_mysql_t
        WHERE currency_code = selected_currency
        AND del_flag = '0'
        AND month >= '0000-00'
        AND month &lt;= '9999-99'
        AND month like '____-__'
        ORDER BY month DESC
        LIMIT 1 BY month, currency_code
        ),

        base_data AS (
        SELECT
        sku_id,
        sku_code,
        team_id,
        store_id,
        market_code,
        sale_duty_uid,
        sku_category_id,
        sku_name,
        team_name,
        store_name,
        market_name,
        asin,
        parent_asin,
        msku_code,
        sale_duty_uname,
        product_title,
        sku_category_name,

        month_date,
        date_time,

        ordered_units,
        shipped_units,
        customer_returns,
        glance_views,

        ordered_revenue_amount / coalesce(r.rate, 1.0) AS ordered_revenue_converted,
        shipped_revenue_amount / coalesce(r.rate, 1.0) AS shipped_revenue_converted,
        shipped_cogs_amount / coalesce(r.rate, 1.0) AS shipped_cogs_converted,
        sales_cost / coalesce(r.rate, 1.0) AS sales_cost_converted,
        inventory_cost / coalesce(r.rate, 1.0) AS inventory_cost_converted,
        procurement_cost / coalesce(r.rate, 1.0) AS procurement_cost_converted,
        sales_commission / coalesce(r.rate, 1.0) AS sales_commission_converted,
        advertising_fee / coalesce(r.rate, 1.0) AS advertising_fee_converted,
        storage_fee / coalesce(r.rate, 1.0) AS storage_fee_converted,
        compliance_fee / coalesce(r.rate, 1.0) AS compliance_fee_converted,
        shipping_fee / coalesce(r.rate, 1.0) AS shipping_fee_converted,
        net_ppm,

        ordered_revenue_amount,
        shipped_revenue_amount,
        shipped_cogs_amount

        FROM ods_sale_vc_monitor_t t
        LEFT JOIN exchange_rates r ON r.month = t.month_date
        WHERE multiIf(
        date_time = (SELECT today_str) AND data_type = 1, 1,
        date_time &lt; (SELECT today_str) AND data_type = 2, 1,
        0
        ) = 1
        ),

        date_series AS (
        SELECT
        toDate(today_str) - 27 + number AS date
        FROM numbers(28)
        ),

        group_table AS (select
        sku_id,
        sku_code,
        team_id,
        store_id,
        market_code,
        sale_duty_uid,
        sku_category_id,
        sku_name,
        team_name,
        store_name,
        market_name,
        asin,
        parent_asin,
        msku_code,
        sale_duty_uname,
        product_title,
        sku_category_name,

        ifNull(sum(ordered_units), 0) AS ordered_units,
        ifNull(sum(shipped_units), 0) AS shipped_units,
        ifNull(sum(customer_returns), 0) AS customer_returns,
        ifNull(sum(glance_views), 0) AS glance_views,
        ifNull(sum(ordered_revenue_converted), 0) AS ordered_revenue_amount,
        ifNull(sum(shipped_revenue_converted), 0) AS shipped_revenue_amount,
        ifNull(sum(shipped_cogs_converted), 0) AS shipped_cogs_amount,
        ifNull(sum(sales_cost_converted), 0) AS sales_cost,
        ifNull(sum(inventory_cost_converted), 0) AS inventory_cost,
        ifNull(sum(procurement_cost_converted), 0) AS procurement_cost,
        ifNull(sum(sales_commission_converted), 0) AS sales_commission,
        ifNull(sum(advertising_fee_converted), 0) AS advertising_fee,
        ifNull(sum(storage_fee_converted), 0) AS storage_fee,
        ifNull(sum(compliance_fee_converted), 0) AS compliance_fee,
        ifNull(sum(shipping_fee_converted), 0) AS shipping_fee,
        ifNull(sum(net_ppm), 0) AS net_ppm,
        multiIf(ifNull(sum(base_data.ordered_units), 0) == 0, 0, ifNull(sum(base_data.ordered_revenue_converted), 0) / ifNull(sum(base_data.ordered_units), 0)) AS averageUnitPrice,
        multiIf(ifNull(sum(base_data.glance_views), 0) == 0, 0, ifNull(sum(base_data.ordered_units), 0) / ifNull(sum(base_data.glance_views), 0)) AS conversionRate,
        sales_commission + ordered_revenue_amount + compliance_fee AS amazonVCRevenue,
        procurement_cost + inventory_cost AS productCost,
        sales_commission + advertising_fee + compliance_fee AS platformFee,
        amazonVCRevenue + productCost + shipping_fee + storage_fee AS totalCost,
        shipped_revenue_amount - totalCost AS grossProfit,
        multiIf(shipped_revenue_amount == 0, 0, grossProfit / shipped_revenue_amount) AS grossProfitMargin

        from base_data
        group by
        sku_id,
        sku_code,
        team_id,
        store_id,
        market_code,
        sale_duty_uid,
        sku_category_id,
        sku_name, team_name, store_name, market_name, asin, parent_asin,
        msku_code, sale_duty_uname, product_title, sku_category_name)

select * from group_table;

-- auto-generated definition
create table ods_sale_vc_monitor_t
(
    id                     UInt64 comment '主键ID',
    store_id               UInt64 comment '店铺ID',
    store_name             LowCardinality(String) comment '店铺名称',
    market_id              Int8 comment '市场ID',
    market_code            LowCardinality(String) comment '市场编码',
    market_name            LowCardinality(String) comment '市场名称/平台名称',
    asin                   String comment '产品ASIN',
    distributor_view       LowCardinality(String) comment '视图类型 MANUFACTURING, SOURCING',
    parent_asin            String comment '父ASIN',
    pic_url                String comment '产品图片URL',
    msku_code              String comment 'MSKU编码',
    product_title          String comment '产品标题',
    sku_id                 Nullable(UInt64) comment 'SKU ID',
    sku_code               String comment 'SKU编码',
    sku_name               String comment 'SKU名称',
    sku_category_id        Nullable(UInt64) comment 'SKU分类ID',
    sku_category_name      LowCardinality(String) comment 'SKU分类名称',
    team_id                Nullable(UInt64) comment '团队ID',
    team_name              LowCardinality(String) comment '团队名称',
    sale_duty_uid          UInt64 comment '销售负责人ID',
    sale_duty_uname        String comment '销售负责人姓名',
    ordered_units          Int32 comment '销量',
    shipped_units          Int32 comment '出货数量',
    customer_returns       Int32 comment '退货数量',
    glance_views           Int32 comment '访问量',
    ordered_revenue_amount Decimal(20, 8) comment '销售额',
    shipped_revenue_amount Decimal(20, 8) comment '收入',
    shipped_cogs_amount    Decimal(20, 8) comment '出货成本金额',
    sales_cost             Decimal(20, 8) comment '销售成本',
    inventory_cost         Decimal(20, 8) comment '到库成本',
    procurement_cost       Decimal(20, 8) comment '采购成本',
    sales_commission       Decimal(20, 8) comment '销售佣金',
    advertising_fee        Decimal(20, 8) comment '广告费',
    storage_fee            Decimal(20, 8) comment '仓储费',
    compliance_fee         Decimal(20, 8) comment '活动费',
    shipping_fee           Decimal(20, 8) comment '配送费',
    net_ppm                Decimal(10, 6) comment 'NET PPM指标',
    hour_timestamp         Nullable(Int64) comment '小时时间戳(秒级)',
    hour_time              String comment '小时时间字符串',
    date_timestamp         Int64 comment '日期时间戳(秒级)',
    date_time              String comment '日期时间字符串',
    month_date             String comment '月份字符串',
    calc_date              String comment '统计日期_站点时间',
    calc_start_time_site   String comment '统计开始时间_站点时间',
    calc_end_time_site     String comment '统计结束时间_站点时间',
    calc_start_time_bj     String comment '统计开始时间_北京时间',
    calc_end_time_bj       String comment '统计结束时间_北京时间',
    start_time_utc         Nullable(String) comment '统计结束时间_UTC时间',
    end_time_utc           Nullable(String) comment '统计结束时间_UTC时间',
    data_type              UInt8 comment '数据维度类型: 1=小时维度, 2=日维度',
    has_hourly_data        UInt8                  default 0 comment '当天是否有小时数据: 0=无, 1=有',
    has_daily_data         UInt8                  default 0 comment '当天是否有日数据: 0=无, 1=有',
    data_priority          UInt8                  default 1 comment '数据优先级: 1=小时优先, 2=日优先',
    data_completeness      LowCardinality(String) default 'COMPLETE' comment '数据完整性: COMPLETE=完整, PARTIAL=部分, ESTIMATED=估算',
    source_table           LowCardinality(String) comment '来源表: hours/daily',
    create_time            DateTime comment '创建时间',

    update_time            DateTime comment '更新时间',
    create_by              String comment '创建人',
    update_by              String comment '更新人',
    tenant_id              UInt64                 default 1 comment '租户ID',
    _version               UInt64                 default toUnixTimestamp64Milli(now64()) comment '数据版本号(毫秒时间戳)，用于ReplacingMergeTree去重',
    _is_deleted            UInt8                  default 0 comment '逻辑删除标记：0-有效 1-删除'
)
    engine = ReplacingMergeTree(_version, _is_deleted)
    PARTITION BY (tenant_id, toYYYYMM(toDate(date_timestamp / 1000)))
        ORDER BY (data_type, distributor_view, store_id, market_id, asin, calc_end_time_site)
        SETTINGS index_granularity = 8192, allow_nullable_key = 1
        COMMENT 'VC销售数据监控表(MSKU级别)-本地物理表' comment 'VC销售数据监控表(MSKU级别)-本地物理表';



prompt：这个sql是用于对ods_sale_vc_monitor_t分组聚合，其中金额汇总的时候需要做汇率转换，我觉得这种汇率转换的方案可行但是有缺陷，
我的理解是应该按月分组先聚合一下，然后把不同月份的转换后的金额合并在一起。另一个问题是，我有一个date_series，用来表示最近28天的日期，格式为yyyy-MM，
我希望分组聚合后的数据可以带有一个array字段，用于展示最近28天的销量。