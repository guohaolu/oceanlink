package org.example;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 跨线程ThreadLocal实现
 *
 * @author guohao.lu
 */
public class TransmissibleThreadLocal<T> extends InheritableThreadLocal<T> {
//    public static class Transmitter {
//        public static Object capture() {
//            Map<TransmissibleThreadLocal<?>, Object> captured = new HashMap<TransmissibleThreadLocal<?>, Object>();
//            //获取所有存储在holder中的变量
//            for (TransmissibleThreadLocal<?> threadLocal : holder.get().keySet()) {
//                captured.put(threadLocal, threadLocal.copyValue());
//            }
//            return captured;
//        }
//
//        public static Object replay(Object captured) {
//            @SuppressWarnings("unchecked")
//            Map<TransmissibleThreadLocal<?>, Object> capturedMap = (Map<TransmissibleThreadLocal<?>, Object>) captured;
//            Map<TransmissibleThreadLocal<?>, Object> backup = new HashMap<TransmissibleThreadLocal<?>, Object>();
//            for (Iterator<? extends Map.Entry<TransmissibleThreadLocal<?>, ?>> iterator = holder.get().entrySet().iterator(); iterator.hasNext(); ) {
//                Map.Entry<TransmissibleThreadLocal<?>, ?> next = iterator.next();
//                TransmissibleThreadLocal<?> threadLocal = next.getKey();
//                // backup
//                backup.put(threadLocal, threadLocal.get());
//                // clear the TTL value only in captured
//                // avoid extra TTL value in captured, when run task.
//                //过滤非传递的变量
//                if (!capturedMap.containsKey(threadLocal)) {
//                    iterator.remove();
//                    // TODO threadLocal.superRemove();
//                }
//            }
//            // set value to captured TTL
//            for (Map.Entry<TransmissibleThreadLocal<?>, Object> entry : capturedMap.entrySet()) {
//                @SuppressWarnings("unchecked")
//                TransmissibleThreadLocal<Object> threadLocal = (TransmissibleThreadLocal<Object>) entry.getKey();
//                threadLocal.set(entry.getValue());
//            }
//            // call beforeExecute callback
//            // TODO doExecuteCallback(true);
//            return backup;
//        }
//
//        public static void restore(Object backup) {
//            @SuppressWarnings("unchecked")
//            Map<TransmissibleThreadLocal<?>, Object> backupMap = (Map<TransmissibleThreadLocal<?>, Object>) backup;
//            // call afterExecute callback
//            // TODO doExecuteCallback(false);
//            for (Iterator<? extends Map.Entry<TransmissibleThreadLocal<?>, ?>> iterator = holder.get().entrySet().iterator();
//                 iterator.hasNext(); ) {
//                Map.Entry<TransmissibleThreadLocal<?>, ?> next = iterator.next();
//                TransmissibleThreadLocal<?> threadLocal = next.getKey();
//                // clear the TTL value only in backup
//                // avoid the extra value of backup after restore
//                if (!backupMap.containsKey(threadLocal)) {
//                    iterator.remove();
//                    // TODO threadLocal.superRemove();
//                }
//            }
//            // restore TTL value
//            for (Map.Entry<TransmissibleThreadLocal<?>, Object> entry : backupMap.entrySet()) {
//                @SuppressWarnings("unchecked")
//                TransmissibleThreadLocal<Object> threadLocal = (TransmissibleThreadLocal<Object>) entry.getKey();
//                threadLocal.set(entry.getValue());
//            }
//        }
//    }
}
