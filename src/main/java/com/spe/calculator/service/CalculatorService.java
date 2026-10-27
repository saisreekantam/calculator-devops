package com.spe.calculator.service;

import com.spe.calculator.exception.DivisionByZeroException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Pure arithmetic logic. BigDecimal is used instead of double so that
 * results like 0.1 + 0.2 come back as 0.3 rather than 0.30000000000000004.
 */
@Service
public class CalculatorService {

    // 16 significant digits, same precision class as IEEE 754 double
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return normalize(a.add(b));
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return normalize(a.subtract(b));
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return normalize(a.multiply(b));
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            throw new DivisionByZeroException();
        }
        return normalize(a.divide(b, PRECISION));
    }

    private static BigDecimal normalize(BigDecimal value) {
        // 5.00 -> 5 so JSON shows "result": 5
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
}
