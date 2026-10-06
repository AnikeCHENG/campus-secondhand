package com.example.campussecondhand.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单状态枚举契约测试：确保数据库取值与枚举严格一致。
 */
class OrderStatusTest {

    @Test
    void codes_match_database_contract() {
        assertThat(OrderStatus.PENDING_PAYMENT.getCode()).isEqualTo(0);
        assertThat(OrderStatus.PENDING_SHIPMENT.getCode()).isEqualTo(1);
        assertThat(OrderStatus.PENDING_RECEIPT.getCode()).isEqualTo(2);
        assertThat(OrderStatus.COMPLETED.getCode()).isEqualTo(3);
        assertThat(OrderStatus.CANCELLED.getCode()).isEqualTo(4);
    }

    @Test
    void labels_are_consistent() {
        assertThat(OrderStatus.PENDING_PAYMENT.getLabel()).isEqualTo("待支付");
        assertThat(OrderStatus.PENDING_SHIPMENT.getLabel()).isEqualTo("待发货");
        assertThat(OrderStatus.PENDING_RECEIPT.getLabel()).isEqualTo("待收货");
        assertThat(OrderStatus.COMPLETED.getLabel()).isEqualTo("已完成");
        assertThat(OrderStatus.CANCELLED.getLabel()).isEqualTo("已取消");
    }

    @Test
    void fromCode_round_trips() {
        for (OrderStatus status : OrderStatus.values()) {
            assertThat(OrderStatus.fromCode(status.getCode())).isSameAs(status);
        }
    }

    @Test
    void fromCode_returns_null_for_unknown_or_null() {
        assertThat(OrderStatus.fromCode(null)).isNull();
        assertThat(OrderStatus.fromCode(99)).isNull();
        assertThat(OrderStatus.fromCode(-1)).isNull();
    }

    @Test
    void only_pending_payment_is_payable_and_cancellable() {
        // 支付与取消都只对待支付开放，这是 OrderService 的状态机前提
        assertThat(OrderStatus.isPendingPayment(0)).isTrue();
        for (int code : new int[]{1, 2, 3, 4}) {
            assertThat(OrderStatus.isPendingPayment(code)).isFalse();
        }
    }

    @Test
    void helpers_identify_terminal_states() {
        assertThat(OrderStatus.isCancelled(4)).isTrue();
        assertThat(OrderStatus.isCancelled(3)).isFalse();
        assertThat(OrderStatus.isCompleted(3)).isTrue();
        assertThat(OrderStatus.isCompleted(1)).isFalse();
    }
}
