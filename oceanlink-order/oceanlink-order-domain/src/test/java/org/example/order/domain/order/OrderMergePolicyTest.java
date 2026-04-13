package org.example.order.domain.order;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 验证订单合并策略。
 *
 * @author guohao.lu
 */
class OrderMergePolicyTest {

    /**
     * 验证较新的订单快照会覆盖状态类字段。
     */
    @Test
    void shouldPreferStatusFromNewerSnapshot() {
        OrderAggregate current = new OrderAggregate(
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                "Unshipped",
                "AMAZON",
                OrderFulfillmentMode.FBA,
                Instant.parse("2026-04-13T10:00:00Z"),
                null,
                null,
                List.of("PRIME"));
        OrderAggregate incoming = new OrderAggregate(
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                "Shipped",
                "AMAZON",
                OrderFulfillmentMode.FBA,
                Instant.parse("2026-04-13T10:05:00Z"),
                "buyer@example.com",
                null,
                List.of("PRIME", "AMAZON_BUSINESS"));

        OrderAggregate merged = new OrderMergePolicy().merge(current, incoming);

        Assertions.assertEquals("Shipped", merged.orderStatus());
        Assertions.assertEquals(Instant.parse("2026-04-13T10:05:00Z"), merged.lastUpdatedTimeAmazon());
        Assertions.assertEquals(List.of("PRIME", "AMAZON_BUSINESS"), merged.programs());
    }

    /**
     * 验证较老快照不能回写新状态，但可以补齐原本缺失的增强字段。
     */
    @Test
    void shouldKeepNewerStatusAndBackfillMissingEnhancedFields() {
        OrderAggregate current = new OrderAggregate(
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                "Shipped",
                "MERCHANT",
                OrderFulfillmentMode.FBM,
                Instant.parse("2026-04-13T10:05:00Z"),
                null,
                null,
                List.of());
        OrderAggregate incoming = new OrderAggregate(
                "seller-1",
                "ATVPDKIKX0DER",
                "112-1234567-1234567",
                "Unshipped",
                "MERCHANT",
                OrderFulfillmentMode.FBM,
                Instant.parse("2026-04-13T10:00:00Z"),
                "buyer@example.com",
                "OceanLink LLC",
                List.of("AMAZON_BUSINESS"));

        OrderAggregate merged = new OrderMergePolicy().merge(current, incoming);

        Assertions.assertEquals("Shipped", merged.orderStatus());
        Assertions.assertEquals("buyer@example.com", merged.buyerEmail());
        Assertions.assertEquals("OceanLink LLC", merged.buyerCompanyName());
        Assertions.assertEquals(List.of("AMAZON_BUSINESS"), merged.programs());
    }
}
