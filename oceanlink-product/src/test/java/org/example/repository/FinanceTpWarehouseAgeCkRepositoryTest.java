package org.example.repository;

import com.google.common.collect.Lists;
import org.example.handler.AsyncBatchProcessorScheduled;
import org.example.mapper.FinanceTpWarehouseAgeCkMapper;
import org.example.mybatis.UniqueDateTime64;
import org.example.pojo.entity.FinanceTpWarehouseAgeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SpringBootTest
class FinanceTpWarehouseAgeCkRepositoryTest {
    @Autowired
    private FinanceTpWarehouseAgeCkRepository financeTpWarehouseAgeCkRepository;

    @Test
    void testInsertAsync() {
        Supplier<FinanceTpWarehouseAgeEntity> entitySupplier = () -> {
            FinanceTpWarehouseAgeEntity entity = new FinanceTpWarehouseAgeEntity();
            // 模拟数据
            entity.setReportDate(LocalDate.now());
            entity.setTripartiteProviderName("Test Provider");
            entity.setTripartiteWhCode("WH001");
            entity.setTripartiteSkuCode("SKU001");
            entity.setStockQuantity(100);
            entity.setInTransitQuantity(50);
            entity.setAge(10);
            entity.setCreateBy("testUser");
            entity.setCreateByName("测试用户");
            entity.setUpdateBy("testUser");
            entity.setUpdateByName("测试用户");
            return entity;
        };
        List<FinanceTpWarehouseAgeEntity> entities = Stream.generate(entitySupplier).limit(5000).peek(entity -> {
            LocalDateTime dt = UniqueDateTime64.nextDateTime64();
            entity.setCreateTime(dt);
            entity.setUpdateTime(dt);
        }).toList();

        financeTpWarehouseAgeCkRepository.getBaseMapper().asyncInsertClickhouse(entities);
    }

    @Test
    void testBatchInsert() {
        AsyncBatchProcessorScheduled<FinanceTpWarehouseAgeCkMapper, FinanceTpWarehouseAgeEntity> asyncBatchProcessorScheduled =
                new AsyncBatchProcessorScheduled<>(financeTpWarehouseAgeCkRepository);
        asyncBatchProcessorScheduled.registerShutdownHook();
        asyncBatchProcessorScheduled.addData(new FinanceTpWarehouseAgeEntity());
        asyncBatchProcessorScheduled.forceFlush();
    }
}