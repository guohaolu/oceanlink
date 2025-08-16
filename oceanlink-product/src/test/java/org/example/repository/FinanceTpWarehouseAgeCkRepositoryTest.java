package org.example.repository;

import com.google.common.collect.Lists;
import org.example.pojo.entity.FinanceTpWarehouseAgeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FinanceTpWarehouseAgeCkRepositoryTest {
    @Autowired
    private FinanceTpWarehouseAgeCkRepository financeTpWarehouseAgeCkRepository;

    @Test
    void testInsertAsync() {
        FinanceTpWarehouseAgeEntity entity1 = new FinanceTpWarehouseAgeEntity();
        // 设置属性值
//        entity1.setReportDate(LocalDate.now());
        entity1.setTripartiteProviderName("Test Provider");
        entity1.setTripartiteWhCode("WH001");
        entity1.setTripartiteSkuCode("SKU001");
        entity1.setStockQuantity(100);
        entity1.setInTransitQuantity(50);
        entity1.setAge(10);
        entity1.setCreateBy("testUser");
        entity1.setCreateByName("测试用户");
        entity1.setUpdateBy("testUser");
        entity1.setUpdateByName("测试用户");

        financeTpWarehouseAgeCkRepository.getBaseMapper().asyncInsertClickhouse(Lists.newArrayList(entity1));
    }
}