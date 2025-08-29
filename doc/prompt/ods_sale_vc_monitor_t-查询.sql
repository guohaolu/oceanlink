WITH
    -- 获取当前日期和参数
    formatDateTime(today(), '%Y-%m-%d') AS today_str,
    formatDateTime(today() - 1, '%Y-%m-%d') AS yesterday_str,
    formatDateTime(today() - 6, '%Y-%m-%d') AS last_7_days_start,
    formatDateTime(today() - 29, '%Y-%m-%d') AS last_30_days_start,
    formatDateTime(today(), '%Y-%m-01') AS month_start,
    formatDateTime(today(), '%Y-01-01') AS year_start,

    'USD' AS selected_currency,
    'today' AS selected_period,
    '' AS custom_start_date,
    '' AS custom_end_date,

-- 确定本期时间范围
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

-- 确定上期时间范围（相同长度往前推）
previous_period_range AS (
    SELECT
        multiIf(
            selected_period = 'month', toString(toStartOfMonth(subDate(today(), INTERVAL 1 MONTH))),
            selected_period = 'year', toString(toStartOfYear(subDate(today(), INTERVAL 1 YEAR))),
            formatDateTime(toDate(start_date) - (toDate(end_date) - toDate(start_date) + 1), '%Y-%m-%d')
        ) as start_date,
        multiIf(
            selected_period = 'month', toString(toLastDayOfMonth(subDate(today(), INTERVAL 1 MONTH))),
            selected_period = 'year', toString(toLastDayOfMonth(subDate(toStartOfYear(today()), INTERVAL 1 MONTH))),
            formatDateTime(toDate(start_date) - 1, '%Y-%m-%d')
        ) as end_date
    FROM current_period_range
),

-- 获取汇率数据（按月分组）
exchange_rates AS (
    SELECT
        month,
        currency_code,
        coalesce(average_median_price, 1) as rate
    FROM sys_currency_rate_mysql_t
    WHERE currency_code = selected_currency
      AND del_flag = '0' AND month >= '0000-00' AND month <= '9999-99' AND month like '____-__'
    ORDER BY month DESC
    LIMIT 1 BY month, currency_code
),

-- 本期销售数据
current_sales AS (
    SELECT
        toString(toYYYYMM(toDate(date_time))) as month_key,
        sum(ordered_units) as ordered_units,
        sum(ordered_revenue_amount) as ordered_revenue_amount,
        sum(shipped_revenue_amount) as shipped_revenue_amount,
        sum(shipped_cogs_amount) as shipped_cogs_amount,
        sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
    FROM ods_sale_vc_monitor_t
    WHERE date_time BETWEEN (SELECT start_date FROM current_period_range)

                        AND (SELECT end_date FROM current_period_range)
      AND multiIf(
          date_time = (SELECT today_str) AND data_type = 1, 1,
          date_time < (SELECT today_str) AND data_type = 2, 1,
          0
      ) = 1
    GROUP BY month_key
),

-- 上期销售数据
previous_sales AS (
    SELECT
        toString(toYYYYMM(toDate(date_time))) as month_key,
        sum(ordered_units) as ordered_units,
        sum(ordered_revenue_amount) as ordered_revenue_amount,
        sum(shipped_revenue_amount) as shipped_revenue_amount,
        sum(shipped_cogs_amount) as shipped_cogs_amount,
        sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
    FROM ods_sale_vc_monitor_t
    WHERE date_time BETWEEN (SELECT start_date FROM previous_period_range)
                        AND (SELECT end_date FROM previous_period_range)
      AND multiIf(
          date_time = formatDateTime(toDate((SELECT start_date FROM previous_period_range)), '%Y-%m-%d') AND data_type = 1, 1,
          date_time < formatDateTime(toDate((SELECT start_date FROM previous_period_range)), '%Y-%m-%d') AND data_type = 2, 1,
          0
      ) = 1
    GROUP BY month_key
),

-- 小数据：昨日数据
yesterday_data AS (
    SELECT
        'yesterday' as period,
        sum(ordered_units) as ordered_units,
        sum(ordered_revenue_amount) as ordered_revenue_amount,
        sum(shipped_revenue_amount) as shipped_revenue_amount,
        sum(shipped_cogs_amount) as shipped_cogs_amount,
        sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
    FROM ods_sale_vc_monitor_t
    WHERE date_time = (SELECT yesterday_str)
      AND data_type = 2
),

-- 小数据：近7日数据
last_7_days_data AS (
    SELECT
        'last_7_days' as period,
        sum(ordered_units) as ordered_units,
        sum(ordered_revenue_amount) as ordered_revenue_amount,
        sum(shipped_revenue_amount) as shipped_revenue_amount,
        sum(shipped_cogs_amount) as shipped_cogs_amount,
        sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
    FROM ods_sale_vc_monitor_t
    WHERE date_time BETWEEN (SELECT last_7_days_start) AND (SELECT yesterday_str)
      AND data_type = 2
),

-- 小数据：近30日数据
last_30_days_data AS (
    SELECT
        'last_30_days' as period,
        sum(ordered_units) as ordered_units,
        sum(ordered_revenue_amount) as ordered_revenue_amount,
        sum(shipped_revenue_amount) as shipped_revenue_amount,
        sum(shipped_cogs_amount) as shipped_cogs_amount,
        sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
    FROM ods_sale_vc_monitor_t
    WHERE date_time BETWEEN (SELECT last_30_days_start) AND (SELECT yesterday_str)
      AND data_type = 2
),

-- 小数据：去年数据
last_year_data AS (
    SELECT
        'last_year' as period,
        sum(ordered_units) as ordered_units,
        sum(ordered_revenue_amount) as ordered_revenue_amount,
        sum(shipped_revenue_amount) as shipped_revenue_amount,
        sum(shipped_cogs_amount) as shipped_cogs_amount,
        sum(ods_sale_vc_monitor_t.shipped_revenue_amount - ods_sale_vc_monitor_t.shipped_cogs_amount) as profit
    FROM ods_sale_vc_monitor_t
    WHERE toYear(toDate(date_time)) = toYear(today()) - 1
      AND data_type = 2
),

-- 合并小数据
small_data AS (
    SELECT * FROM yesterday_data
    UNION ALL
    SELECT * FROM last_7_days_data
    UNION ALL
    SELECT * FROM last_30_days_data
    UNION ALL
    SELECT * FROM last_year_data
),

-- 应用汇率转换到本期数据
current_with_exchange AS (
    SELECT
        c.month_key,
        c.ordered_units,
        c.ordered_revenue_amount / coalesce(e.rate, 1) as ordered_revenue_amount,
        c.shipped_revenue_amount / coalesce(e.rate, 1) as shipped_revenue_amount,
        c.shipped_cogs_amount / coalesce(e.rate, 1) as shipped_cogs_amount,
        c.profit / coalesce(e.rate, 1) as profit
    FROM current_sales c
    LEFT JOIN exchange_rates e ON e.month = c.month_key
),

-- 应用汇率转换到上期数据
previous_with_exchange AS (
    SELECT
        p.month_key,
        p.ordered_units,
        p.ordered_revenue_amount / coalesce(e.rate, 1) as ordered_revenue_amount,
        p.shipped_revenue_amount / coalesce(e.rate, 1) as shipped_revenue_amount,
        p.shipped_cogs_amount / coalesce(e.rate, 1) as shipped_cogs_amount,
        p.profit / coalesce(e.rate, 1) as profit
    FROM previous_sales p
    LEFT JOIN exchange_rates e ON e.month = p.month_key
),

-- 应用汇率转换到小数据
small_with_exchange AS (
    SELECT
        s.period,
        s.ordered_units,
        s.ordered_revenue_amount / coalesce(e.rate, 1) as ordered_revenue_amount,
        s.shipped_revenue_amount / coalesce(e.rate, 1) as shipped_revenue_amount,
        s.shipped_cogs_amount / coalesce(e.rate, 1) as shipped_cogs_amount,
        s.profit / coalesce(e.rate, 1) as profit
    FROM small_data s
    CROSS JOIN (SELECT rate FROM exchange_rates ORDER BY month DESC LIMIT 1) e
)

-- 最终结果
SELECT
    -- 本期大数据
    'current_period' as data_type,
    sum(ordered_units) as ordered_units,
    sum(ordered_revenue_amount) as ordered_revenue_amount,
    sum(shipped_revenue_amount) as shipped_revenue_amount,
    sum(shipped_cogs_amount) as shipped_cogs_amount,
    sum(profit) as profit
FROM current_with_exchange

UNION ALL

SELECT
    -- 上期大数据
    'previous_period' as data_type,
    sum(ordered_units) as ordered_units,
    sum(ordered_revenue_amount) as ordered_revenue_amount,
    sum(shipped_revenue_amount) as shipped_revenue_amount,
    sum(shipped_cogs_amount) as shipped_cogs_amount,
    sum(profit) as profit
FROM previous_with_exchange

UNION ALL

SELECT
    -- 小数据
    period as data_type,
    ordered_units,
    ordered_revenue_amount,
    shipped_revenue_amount,
    shipped_cogs_amount,
    profit
FROM small_with_exchange;