package org.example.order.domain.order;

/**
 * 订单 PII 访问策略。
 *
 * @author guohao.lu
 */
public class OrderPiiAccessPolicy {

    /**
     * 判断当前上下文是否允许读取明文 PII。
     *
     * @param context PII 读取上下文
     * @return 允许读取返回 true
     */
    public boolean canRead(OrderPiiReadContext context) {
        return context.authorized() && context.auditEnabled();
    }

    /**
     * 对 PII 聚合进行脱敏处理。
     *
     * @param piiAggregate 原始 PII 聚合
     * @return 脱敏后的 PII 聚合
     */
    public OrderPiiAggregate mask(OrderPiiAggregate piiAggregate) {
        return new OrderPiiAggregate(
                maskName(piiAggregate.buyerName()),
                maskName(piiAggregate.recipientName()),
                maskPhone(piiAggregate.phone()),
                maskAddress(piiAggregate.addressLine1()),
                piiAggregate.city(),
                piiAggregate.countryCode());
    }

    /**
     * 脱敏姓名。
     *
     * @param name 原始姓名
     * @return 脱敏后的姓名
     */
    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        return name.substring(0, 1) + "***";
    }

    /**
     * 脱敏电话号码。
     *
     * @param phone 原始电话号码
     * @return 脱敏后的电话号码
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 脱敏地址第一行。
     *
     * @param addressLine1 原始地址
     * @return 脱敏后的地址
     */
    private String maskAddress(String addressLine1) {
        if (addressLine1 == null || addressLine1.isBlank()) {
            return addressLine1;
        }
        int prefixLength = Math.min(3, addressLine1.length());
        return addressLine1.substring(0, prefixLength) + "***";
    }
}
