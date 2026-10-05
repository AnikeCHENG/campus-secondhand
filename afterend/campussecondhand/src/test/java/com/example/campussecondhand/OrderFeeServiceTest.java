package com.example.campussecondhand;

import com.example.campussecondhand.service.OrderFeeService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderFeeServiceTest {
    private final OrderFeeService service = new OrderFeeService();

    @Test
    void normal_user_100_fee_030() {
        BigDecimal fee = service.calculateServiceFee(new BigDecimal("100"), false);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("0.30"));
    }

    @Test
    void normal_user_4000_fee_capped_10() {
        BigDecimal fee = service.calculateServiceFee(new BigDecimal("4000"), false);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void verified_student_100_fee_0() {
        BigDecimal fee = service.calculateServiceFee(new BigDecimal("100"), true);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void normal_user_9_9_fee_0_03() {
        BigDecimal fee = service.calculateServiceFee(new BigDecimal("9.9"), false);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("0.03"));
    }
}
