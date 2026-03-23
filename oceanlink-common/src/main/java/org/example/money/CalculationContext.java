package org.example.money;

import lombok.Getter;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 计算上下文
 *
 * @author guohao.lu
 */
@Getter
public class CalculationContext {
    /**
     * 最终金额精度（如2）
     */
    private final int finalScale;
    private final RoundingMode finalRoundingMode;

    /**
     * 中间计算精度
     */
    private final MathContext intermediateContext;

    public CalculationContext(int finalScale, RoundingMode finalRoundingMode, int intermediatePrecision) {
        this.finalScale = finalScale;
        this.finalRoundingMode = finalRoundingMode;
        this.intermediateContext = new MathContext(intermediatePrecision, RoundingMode.HALF_EVEN);
    }
}
