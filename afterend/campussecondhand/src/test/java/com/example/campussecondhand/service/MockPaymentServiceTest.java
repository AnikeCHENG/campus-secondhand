package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.PaymentRecord;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.PaymentRecordRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.PaymentService.PayResult;
import com.example.campussecondhand.service.impl.MockPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MockPaymentServiceImpl} 契约测试。
 *
 * <p>该实现是当前唯一的支付渠道，行为契约必须稳定：
 * 永远成功、流水号以 MOCK 开头、格式合法。</p>
 */
class MockPaymentServiceTest {

    private MockPaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new MockPaymentServiceImpl();
    }

    private Order sampleOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20251006143022001");
        order.setPrice(new BigDecimal("1500.00"));
        return order;
    }

    @Test
    void always_succeeds() {
        for (int i = 0; i < 20; i++) {
            assertThat(paymentService.pay(sampleOrder(), "支付宝").isSuccess()).isTrue();
        }
    }

    @Test
    void trade_no_has_expected_format() {
        String tradeNo = paymentService.pay(sampleOrder(), "余额").getTradeNo();
        // MOCK + yyyyMMddHHmmssSSS(17位) + 12位随机 = 29 位数字
        assertThat(tradeNo).startsWith("MOCK").matches("^MOCK\\d{29}$");
        // 总长度 33，须落在 payment_record.trade_no 的 varchar(100) 内
        assertThat(tradeNo).hasSize(33);
    }

    @Test
    void trade_no_is_unique_under_rapid_calls() {
        // trade_no 上有唯一索引，同毫秒内多次调用必须仍能区分
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            assertThat(seen.add(paymentService.pay(sampleOrder(), "微信").getTradeNo())).isTrue();
        }
    }

    @Test
    void accepts_arbitrary_pay_method_verbatim() {
        // 后端不做白名单校验
        assertThat(paymentService.pay(sampleOrder(), "银行卡快捷支付").isSuccess()).isTrue();
    }

    @Test
    void does_not_write_any_database_state() {
        // 渠道只负责返回结果，落库由 OrderService 负责
        OrderRepository orderRepository = mock(OrderRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        paymentService.pay(sampleOrder(), "支付宝");

        verify(orderRepository, never()).updateById(any());
        verify(paymentRecordRepository, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void channel_name_is_mock() {
        assertThat(paymentService.channelName()).isEqualTo("mock");
    }

    @Test
    void pay_result_factories_carry_expected_payload() {
        PayResult ok = PayResult.ok("MOCK123");
        assertThat(ok.isSuccess()).isTrue();
        assertThat(ok.getTradeNo()).isEqualTo("MOCK123");
        assertThat(ok.getCode()).isEqualTo(200);

        PayResult fail = PayResult.fail("当前订单状态不可支付");
        assertThat(fail.isSuccess()).isFalse();
        assertThat(fail.getMessage()).isEqualTo("当前订单状态不可支付");
        assertThat(fail.getTradeNo()).isNull();

        PayResult forbidden = PayResult.fail(403, "只有买家可以支付该订单");
        assertThat(forbidden.getCode()).isEqualTo(403);
    }
}
