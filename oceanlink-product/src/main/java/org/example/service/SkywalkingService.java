package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.mybatis.UniqueDateTime64;
import org.example.pojo.entity.FinanceTpWarehouseAgeEntity;
import org.example.repository.impl.FinanceTpWarehouseAgeCkRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author guohao.lu
 */
@Slf4j
@Service
public class SkywalkingService {
    @Resource
    private FinanceTpWarehouseAgeCkRepository financeTpWarehouseAgeCkRepository;

    public String saveTpWarehouseAge() {
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

        log.info("sdasdadsaads");

        financeTpWarehouseAgeCkRepository.getBaseMapper().insert(entities);

        return "success";
    }
}
