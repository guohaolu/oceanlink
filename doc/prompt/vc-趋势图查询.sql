WITH
    formatDateTime(today(), '%Y-%m-%d') AS today_str,
    formatDateTime(today() - 1, '%Y-%m-%d') AS yesterday_str,
    formatDateTime(today() - 6, '%Y-%m-%d') AS last_7_days_start,
    formatDateTime(today() - 29, '%Y-%m-%d') AS last_30_days_start,
    formatDateTime(today(), '%Y-%m-01') AS month_start,
    formatDateTime(today(), '%Y-01-01') AS year_start,

    formatDateTime(today() - 2, '%Y-%m-%d') AS yesterday_previous_day,
    formatDateTime(today() - 13, '%Y-%m-%d') AS last_7_days_previous_start,
    formatDateTime(today() - 59, '%Y-%m-%d') AS last_30_days_previous_start,
    toYear(today()) - 2 AS last_year_previous_year,

    'USD' AS selected_currency,
    'thirty' AS selected_period, -- 改为30天以显示更多数据点
    '' AS custom_start_date,
    '' AS custom_end_date,

    current_period_range AS (
        SELECT
            multiIf(
                selected_period = 'today', today_str,
                selected_period = 'week', last_7_days_start,
                selected_period = 'thirty', last_30_days_start,
                selected_period = 'month', month_start,
                selected_period = 'year', year_start,
                selected_period = 'custom', custom_start_date,
                today_str
            ) as start_date,
            multiIf(
                selected_period = 'custom', custom_end_date,
                today_str
            ) as end_date
    ),

    exchange_rates AS (
        SELECT
            month,
            currency_code,
            coalesce(average_median_price, 1) as rate
        FROM sys_currency_rate_mysql_t
        WHERE currency_code = selected_currency
            AND del_flag = '0'
            AND month >= '0000-00'
            AND month <= '9999-99'
            AND month like '____-__'
        ORDER BY month DESC
        LIMIT 1 BY month, currency_code
    ),

    -- 获取日期范围内的所有日期
    date_series AS (
        SELECT
            toDate(start_date) + number as date
        FROM
            current_period_range,
            numbers(toDate(end_date) - toDate(start_date) + 1)
    ),

    -- 按日期聚合销售数据
    daily_sales AS (
        SELECT
            date_time as date,
            toString(toYYYYMM(toDate(date_time))) as month_key,
            sum(ordered_units) as ordered_units,
            sum(ordered_revenue_amount) as ordered_revenue_amount,
            sum(shipped_revenue_amount) as shipped_revenue_amount,
            sum(shipped_cogs_amount) as shipped_cogs_amount,
            sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
        FROM ods_sale_vc_monitor_t final
        WHERE date_time BETWEEN (SELECT start_date FROM current_period_range)
            AND (SELECT end_date FROM current_period_range)
            AND multiIf(
                date_time = (SELECT today_str) AND data_type = 1, 1,
                date_time < (SELECT today_str) AND data_type = 2, 1,
                0
            ) = 1
        GROUP BY date_time, month_key
    ),

    -- 应用汇率转换
    daily_sales_with_exchange AS (
        SELECT
            ds.date,
            ds.ordered_units,
            ds.ordered_revenue_amount / coalesce(e.rate, 1) as ordered_revenue_amount,
            ds.shipped_revenue_amount / coalesce(e.rate, 1) as shipped_revenue_amount,
            ds.shipped_cogs_amount / coalesce(e.rate, 1) as shipped_cogs_amount,
            ds.profit / coalesce(e.rate, 1) as profit
        FROM daily_sales ds
        LEFT JOIN exchange_rates e ON e.month = ds.month_key
    ),

    -- 生成完整的日期序列（包括没有数据的日期）
    complete_date_series AS (
        SELECT
            ds.date,
            coalesce(dsw.ordered_units, 0) as ordered_units,
            coalesce(dsw.ordered_revenue_amount, 0) as ordered_revenue_amount,
            coalesce(dsw.shipped_revenue_amount, 0) as shipped_revenue_amount,
            coalesce(dsw.shipped_cogs_amount, 0) as shipped_cogs_amount,
            coalesce(dsw.profit, 0) as profit
        FROM date_series ds
        LEFT JOIN daily_sales_with_exchange dsw ON ds.date = dsw.date
    )

-- 返回曲线图所需的数据格式
SELECT
    date as x_date, -- x轴：日期
    ordered_units as y_ordered_units, -- y轴：订购数量
    ordered_revenue_amount as y_ordered_revenue, -- y轴：订购收入
    shipped_revenue_amount as y_shipped_revenue, -- y轴：发货收入
    shipped_cogs_amount as y_shipped_cogs, -- y轴：发货成本
    profit as y_profit -- y轴：利润
FROM complete_date_series
ORDER BY date ASC;

-- 或者如果你只需要一个指标，可以选择其中一个：
/*
SELECT
    date as x_date,
    ordered_revenue_amount as y_value, -- 选择你需要的指标
    'ordered_revenue' as metric_name -- 指标名称
FROM complete_date_series
ORDER BY date ASC;
*/