package org.example.order.domain.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 验证 PII 访问策略。
 *
 * @author guohao.lu
 */
class OrderPiiAccessPolicyTest {

    /**
     * 验证授权且开启审计时允许读取明文 PII。
     */
    @Test
    void shouldAllowReadingPiiWhenAuthorizedAndAudited() {
        OrderPiiReadContext context = new OrderPiiReadContext(true, true);
        Assertions.assertTrue(new OrderPiiAccessPolicy().canRead(context));
    }

    /**
     * 验证未授权场景会返回脱敏视图。
     */
    @Test
    void shouldMaskSensitiveFieldsWhenUnauthorized() {
        OrderPiiAggregate piiAggregate = new OrderPiiAggregate(
                "Buyer Name",
                "Recipient Name",
                "13800001111",
                "1 Ocean Road",
                "Seattle",
                "US");

        OrderPiiAggregate masked = new OrderPiiAccessPolicy().mask(piiAggregate);

        Assertions.assertEquals("B***", masked.buyerName());
        Assertions.assertEquals("R***", masked.recipientName());
        Assertions.assertEquals("138****1111", masked.phone());
        Assertions.assertEquals("1 O***", masked.addressLine1());
    }
}
