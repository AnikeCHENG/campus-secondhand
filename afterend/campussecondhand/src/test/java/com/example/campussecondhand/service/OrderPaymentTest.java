package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.PaymentRecord;
import com.example.campussecondhand.enums.OrderStatus;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.PaymentRecordRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.PaymentService.PayResult;
import com.example.campussecondhand.service.impl.MockPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付链路契约测试。
 *
 * <p>重点覆盖三条最容易出错的约束：</p>
 * <ol>
 *   <li>业务失败绝不抛异常——否则事务回滚会连带撤销「超时取消 + 商品释放」</li>
 *   <li>重复支付不产生第二条流水</li>
 *   <li>流水的 amount 是买家实付，不含卖家承担的服务费</li>
 * </ol>
 */
class OrderPaymentTest {

    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    private OrderRepository orderRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private ProductService productService;
    private OrderService orderService;

    private final List<PaymentRecord> insertedRecords = new ArrayList<>();

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        productService = mock(ProductService.class);
        orderService = new OrderService(orderRepository, paymentRecordRepository, productRepository,
                userRepository, productService, new OrderFeeService(),
                new MockPaymentServiceImpl(),
                new ResourcelessTransactionTemplate(),
                30);

        insertedRecords.clear();
        when(paymentRecordRepository.insert(any(PaymentRecord.class))).thenAnswer(inv -> {
            insertedRecords.add(inv.getArgument(0));
            return 1;
        });
    }

    private Order pendingOrder(LocalDateTime expireTime) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("20251006143022001");
        order.setProductId(PRODUCT_ID);
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setPrice(new BigDecimal("1500.00"));
        order.setShippingFee(new BigDecimal("0.00"));
        order.setServiceFee(new BigDecimal("4.50"));
        order.setSellerIncome(new BigDecimal("1495.50"));
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCreatedTime(LocalDateTime.now());
        order.setExpireTime(expireTime);
        return order;
    }

    @Test
    void pay_success_writes_record_and_advances_status() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        when(orderRepository.selectById(1L)).thenReturn(order);

        PayResult result = orderService.payOrder(1L, "支付宝", BUYER_ID);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTradeNo()).startsWith("MOCK");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT.getCode());
        assertThat(order.getPaidTime()).isNotNull();
        assertThat(order.getPaymentMethod()).isEqualTo("支付宝");
        assertThat(order.getTransactionId()).isEqualTo(result.getTradeNo());

        assertThat(insertedRecords).hasSize(1);
        PaymentRecord record = insertedRecords.get(0);
        assertThat(record.getOrderId()).isEqualTo(1L);
        assertThat(record.getOrderNo()).isEqualTo("20251006143022001");
        assertThat(record.getPayMethod()).isEqualTo("支付宝");
        assertThat(record.getTradeNo()).isEqualTo(result.getTradeNo());
    }

    @Test
    void record_amount_is_buyer_total_excluding_seller_borne_service_fee() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        order.setPrice(new BigDecimal("380.00"));
        order.setShippingFee(new BigDecimal("0.00"));
        order.setServiceFee(new BigDecimal("1.14"));
        when(orderRepository.selectById(1L)).thenReturn(order);

        orderService.payOrder(1L, "微信", BUYER_ID);

        // 买家实付 = 380.00，不含卖家承担的 1.14 服务费
        assertThat(insertedRecords.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("380.00"));
    }

    @Test
    void repeated_pay_returns_failure_without_writing_second_record() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        when(orderRepository.selectById(1L)).thenReturn(order);

        // 第一次支付成功
        orderService.payOrder(1L, "支付宝", BUYER_ID);
        assertThat(insertedRecords).hasSize(1);

        // 第二次：状态已是待发货
        PayResult second = orderService.payOrder(1L, "支付宝", BUYER_ID);

        assertThat(second.isSuccess()).isFalse();
        assertThat(second.getMessage()).isEqualTo("当前订单状态不可支付");
        // 关键断言：不得产生第二条流水
        assertThat(insertedRecords).hasSize(1);
    }

    @Test
    void expired_order_is_cancelled_and_product_released() {
        Order order = pendingOrder(LocalDateTime.now().minusSeconds(1));
        when(orderRepository.selectById(1L)).thenReturn(order);

        PayResult result = orderService.payOrder(1L, "支付宝", BUYER_ID);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("订单已超时取消");
        // 取消与商品释放必须落库（依赖「返回失败对象」而非抛异常）
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());
        verify(productService).markAsOnSale(PRODUCT_ID);
        // 超时订单不得产生流水
        assertThat(insertedRecords).isEmpty();
    }

    @Test
    void non_buyer_cannot_pay() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        when(orderRepository.selectById(1L)).thenReturn(order);

        PayResult result = orderService.payOrder(1L, "支付宝", SELLER_ID);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(403);
        assertThat(insertedRecords).isEmpty();
    }

    @Test
    void blank_pay_method_is_rejected() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        when(orderRepository.selectById(1L)).thenReturn(order);

        assertThat(orderService.payOrder(1L, "  ", BUYER_ID).isSuccess()).isFalse();
        assertThat(orderService.payOrder(1L, null, BUYER_ID).isSuccess()).isFalse();
        assertThat(insertedRecords).isEmpty();
    }

    @Test
    void arbitrary_pay_method_is_stored_verbatim_without_whitelist_check() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        when(orderRepository.selectById(1L)).thenReturn(order);

        // 后端不做白名单校验，前端传什么记什么
        PayResult result = orderService.payOrder(1L, "银行卡快捷支付", BUYER_ID);

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getPaymentMethod()).isEqualTo("银行卡快捷支付");
        assertThat(insertedRecords.get(0).getPayMethod()).isEqualTo("银行卡快捷支付");
    }

    @Test
    void trade_no_is_unique_across_calls() {
        Order order = pendingOrder(LocalDateTime.now().plusMinutes(30));
        when(orderRepository.selectById(1L)).thenReturn(order);

        List<String> tradeNos = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
            tradeNos.add(orderService.payOrder(1L, "余额", BUYER_ID).getTradeNo());
        }

        // payment_record.trade_no 有唯一索引，撞号会导致入库失败
        assertThat(tradeNos).doesNotHaveDuplicates();
        assertThat(tradeNos).allMatch(no -> no.startsWith("MOCK"));
    }

    @Test
    void payment_channel_is_reported_for_observability() {
        assertThat(orderService.getPaymentChannel()).isEqualTo("mock");
    }

    @Test
    void batch_cancel_skips_orders_that_are_no_longer_pending() {
        Order expired = pendingOrder(LocalDateTime.now().minusMinutes(5));
        Order justPaid = pendingOrder(LocalDateTime.now().minusMinutes(5));
        justPaid.setId(2L);
        justPaid.setStatus(OrderStatus.PENDING_SHIPMENT.getCode());

        when(orderRepository.selectList(any())).thenReturn(List.of(expired, justPaid));
        // selectById 在事务内被重新确认状态
        when(orderRepository.selectById(1L)).thenReturn(expired);
        when(orderRepository.selectById(2L)).thenReturn(justPaid);

        int cancelled = orderService.cancelTimeoutOrdersBatch();

        // 只取消仍处于待支付的过期订单，避免与用户支付并发导致误取消
        assertThat(cancelled).isEqualTo(1);
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(productService).markAsOnSale(captor.capture());
        assertThat(captor.getValue()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void batch_cancel_continues_after_single_order_failure() {
        Order ok = pendingOrder(LocalDateTime.now().minusMinutes(5));
        ok.setId(1L);
        Order bad = pendingOrder(LocalDateTime.now().minusMinutes(5));
        bad.setId(2L);

        when(orderRepository.selectList(any())).thenReturn(List.of(ok, bad));
        when(orderRepository.selectById(1L)).thenReturn(ok);
        when(orderRepository.selectById(2L)).thenThrow(new RuntimeException("模拟数据库故障"));

        // 单条失败不得影响其余订单
        assertThat(orderService.cancelTimeoutOrdersBatch()).isEqualTo(1);
    }

    @Test
    void batch_cancel_returns_zero_when_nothing_expired() {
        when(orderRepository.selectList(any())).thenReturn(List.of());
        assertThat(orderService.cancelTimeoutOrdersBatch()).isZero();
        verify(productService, never()).markAsOnSale(any());
    }
}
