package org.example.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 简单批量读取-监听器
 * 用于处理Excel数据导入，通过监听事件进行批量处理数据
 *
 * @param <T> 原始数据类型
 * @author guohao.lu
 */
@Slf4j
@Getter
public class SimpleBatchListener<T> extends AnalysisEventListener<T> {
    // 批量处理的大小，即每批处理的记录数
    private static final int BATCH_SIZE = 1500;

    // 记录已处理的数据总数
    private Integer total = 0;

    // 缓存转换后的实体对象，用于批量处理
    private final List<T> cachedList = new ArrayList<>();

    // 消费者，用于处理一批转换后的实体对象
    private final Consumer<List<T>> consumer;
    // 原始数据的类类型
    private final Class<T> clazz;

    /**
     * 构造函数
     *
     * @param consumer  批量处理实体对象的消费者
     * @param clazz     原始数据的类类型
     */
    public SimpleBatchListener(Class<T> clazz, Consumer<List<T>> consumer) {
        this.consumer = consumer;
        this.clazz = clazz;
    }

    /**
     * 处理单个数据对象的回调方法
     *
     * @param data    单个原始数据对象
     * @param context 上下文对象，包含解析过程中的上下文信息
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        // 将转换后的实体对象添加到缓存列表中
        cachedList.add(data);
        // 增加总数计数
        total++;

        // 当缓存列表达到批量处理大小时，执行批量保存并清空缓存列表
        if (cachedList.size() >= BATCH_SIZE) {
            saveBatch();
            cachedList.clear();
        }
    }

    /**
     * 所有数据解析完毕后的回调方法
     *
     * @param context 上下文对象，包含解析过程中的上下文信息
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 最后一批数据可能不足一批，因此在这里进行最终的批量保存并清空缓存列表
        saveBatch();
        cachedList.clear();
    }

    /**
     * 执行批量保存操作
     * 将缓存的实体对象列表传递给消费者进行处理
     */
    private void saveBatch() {
        consumer.accept(cachedList);
    }
}
