package org.example.order.domain.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 验证履约主体到履约模式的映射规则。
 *
 * @author guohao.lu
 */
class OrderFulfillmentModeMapperTest {

    /**
     * 验证 Amazon 与 Merchant 履约主体的标准映射。
     */
    @Test
    void shouldMapAmazonAndMerchantToBusinessModes() {
        Assertions.assertEquals(OrderFulfillmentMode.FBA, OrderFulfillmentModeMapper.map("AMAZON"));
        Assertions.assertEquals(OrderFulfillmentMode.FBM, OrderFulfillmentModeMapper.map("MERCHANT"));
    }

    /**
     * 验证未知履约主体会被归类为未知模式。
     */
    @Test
    void shouldFallbackToUnknownForUnsupportedValue() {
        Assertions.assertEquals(OrderFulfillmentMode.UNKNOWN, OrderFulfillmentModeMapper.map("UNKNOWN"));
        Assertions.assertEquals(OrderFulfillmentMode.UNKNOWN, OrderFulfillmentModeMapper.map(null));
    }
}
