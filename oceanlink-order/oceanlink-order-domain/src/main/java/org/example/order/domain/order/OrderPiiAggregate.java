package org.example.order.domain.order;

/**
 * 订单 PII 聚合。
 *
 * @param buyerName 买家姓名
 * @param recipientName 收件人姓名
 * @param phone 电话
 * @param addressLine1 地址第一行
 * @param city 城市
 * @param countryCode 国家编码
 * @author guohao.lu
 */
public record OrderPiiAggregate(
        String buyerName,
        String recipientName,
        String phone,
        String addressLine1,
        String city,
        String countryCode) {
}
