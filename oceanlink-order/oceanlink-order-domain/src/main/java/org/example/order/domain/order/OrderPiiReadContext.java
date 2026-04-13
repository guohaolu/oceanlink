package org.example.order.domain.order;

/**
 * PII 读取上下文。
 *
 * @param authorized 是否具备受限字段访问授权
 * @param auditEnabled 是否开启访问审计
 * @author guohao.lu
 */
public record OrderPiiReadContext(
        boolean authorized,
        boolean auditEnabled) {
}
