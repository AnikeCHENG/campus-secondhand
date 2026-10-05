package com.example.campussecondhand.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderFeeService {

    private static final BigDecimal RATE = new BigDecimal("0.003");
    private static final BigDecimal MAX_FEE = new BigDecimal("10.00");

    public BigDecimal calculateServiceFee(BigDecimal amount, boolean isStudentVerified) {
        if (amount == null) {
            throw new IllegalArgumentException("amount 不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount 不能为负数");
        }
        if (isStudentVerified) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fee = amount.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
        return fee.min(MAX_FEE);
    }
}
