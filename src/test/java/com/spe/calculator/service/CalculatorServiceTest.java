package com.spe.calculator.service;

import com.spe.calculator.exception.DivisionByZeroException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private final CalculatorService calculator = new CalculatorService();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({"2, 3, 5", "-4, 1.5, -2.5", "0.1, 0.2, 0.3", "0, 0, 0"})
    void add(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.add(bd(a), bd(b)));
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({"10, 4, 6", "3, 7, -4", "1.5, 1.5, 0"})
    void subtract(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.subtract(bd(a), bd(b)));
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({"6, 7, 42", "-2, 3, -6", "2.5, 0.4, 1", "9, 0, 0"})
    void multiply(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.multiply(bd(a), bd(b)));
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({"10, 4, 2.5", "-9, 3, -3", "0, 5, 0", "1, 3, 0.3333333333333333"})
    void divide(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.divide(bd(a), bd(b)));
    }

    @Test
    @DisplayName("divide by zero throws DivisionByZeroException")
    void divideByZero() {
        DivisionByZeroException ex = assertThrows(DivisionByZeroException.class,
                () -> calculator.divide(bd("5"), bd("0")));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }

    @Test
    @DisplayName("divide by 0.0 is also rejected")
    void divideByZeroWithScale() {
        assertThrows(DivisionByZeroException.class, () -> calculator.divide(bd("5"), bd("0.0")));
    }
}
