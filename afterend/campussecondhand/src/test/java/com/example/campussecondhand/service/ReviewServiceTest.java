package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Review;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单评价服务测试。
 *
 * <p>三条业务约束必须锁死：仅买家可评、仅已完成可评、一单一评。
 * 其中「一单一评」最容易在重构中被破坏——应用层的"先查后插"存在
 * 并发窗口，真正的保证在唯一索引，因此要测两次插入都被拒。</p>
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long ORDER_ID = 15L;
    private static final Long BUYER_ID = 6L;
    private static final Long SELLER_ID = 1L;

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private ReviewService service;

    private Order order(int status, Long buyerId) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setStatus(status);
        o.setBuyerId(buyerId);
        o.setSellerId(SELLER_ID);
        o.setProductId(10L);
        return o;
    }

    @Test
    @DisplayName("买家对已完成订单提交评价成功")
    void submitByBuyerOnCompletedOrder() {
        when(orderRepository.selectById(ORDER_ID)).thenReturn(order(3, BUYER_ID));
        when(reviewRepository.findByOrderId(ORDER_ID)).thenReturn(null);

        Review saved = service.submit(ORDER_ID, BUYER_ID, 5, "  描述一致，成色不错  ");

        verify(reviewRepository).insert(any(Review.class));
        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getReviewerId()).isEqualTo(BUYER_ID);
        // 被评价人取订单卖家，不是当前用户
        assertThat(saved.getTargetId()).isEqualTo(SELLER_ID);
        assertThat(saved.getRating()).isEqualTo(5);
        // 内容两侧空格被去掉
        assertThat(saved.getContent()).isEqualTo("描述一致，成色不错");
    }

    @Test
    @DisplayName("卖家不能给自己的订单评价")
    void rejectsNonBuyer() {
        when(orderRepository.selectById(ORDER_ID)).thenReturn(order(3, BUYER_ID));

        assertThatThrownBy(() -> service.submit(ORDER_ID, SELLER_ID, 5, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("无权评价");

        verify(reviewRepository, never()).insert(any());
    }

    @Test
    @DisplayName("非已完成订单不能评价：待支付、待发货、待收货、已取消一律拒绝")
    void rejectsNonCompletedStatus() {
        for (int status : new int[]{0, 1, 2, 4}) {
            when(orderRepository.selectById(ORDER_ID)).thenReturn(order(status, BUYER_ID));

            assertThatThrownBy(() -> service.submit(ORDER_ID, BUYER_ID, 5, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("订单完成后才能评价");
        }
        verify(reviewRepository, never()).insert(any());
    }

    @Test
    @DisplayName("重复评价返回该订单已评价")
    void rejectsDuplicateReview() {
        when(orderRepository.selectById(ORDER_ID)).thenReturn(order(3, BUYER_ID));
        when(reviewRepository.findByOrderId(ORDER_ID)).thenReturn(new Review());

        assertThatThrownBy(() -> service.submit(ORDER_ID, BUYER_ID, 5, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("该订单已评价");
    }

    @Test
    @DisplayName("并发双击：先查通过但唯一索引拦截，同样返回该订单已评价")
    void duplicateKeyAlsoReportedAsAlreadyReviewed() {
        when(orderRepository.selectById(ORDER_ID)).thenReturn(order(3, BUYER_ID));
        when(reviewRepository.findByOrderId(ORDER_ID)).thenReturn(null);
        when(reviewRepository.insert(any(Review.class)))
                .thenThrow(new DuplicateKeyException("uk_order_id"));

        assertThatThrownBy(() -> service.submit(ORDER_ID, BUYER_ID, 5, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("该订单已评价");
    }

    @Test
    @DisplayName("评分必须在 1~5 之间")
    void rejectsRatingOutOfRange() {
        when(orderRepository.selectById(ORDER_ID)).thenReturn(order(3, BUYER_ID));

        for (Integer bad : new Integer[]{null, 0, 6, -1}) {
            assertThatThrownBy(() -> service.submit(ORDER_ID, BUYER_ID, bad, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("1~5 星");
        }
    }

    @Test
    @DisplayName("评价内容限 50 字")
    void rejectsTooLongContent() {
        when(orderRepository.selectById(ORDER_ID)).thenReturn(order(3, BUYER_ID));

        assertThatThrownBy(() -> service.submit(ORDER_ID, BUYER_ID, 5, "字".repeat(51)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不能超过 50 字");
    }

    @Test
    @DisplayName("好评率：4 分及以上计为好评")
    void computesGoodRate() {
        // 5 条评价，其中 4 条 >=4 分
        when(reviewRepository.aggregateByTargetId(SELLER_ID)).thenReturn(row(5L, 4L, "4.0"));

        Map<String, Object> stats = service.statsOf(SELLER_ID);

        assertThat((Long) stats.get("total")).isEqualTo(5L);
        assertThat((Long) stats.get("good")).isEqualTo(4L);
        assertThat(stats.get("goodRate")).hasToString("80.0");
        assertThat(stats.get("averageRating")).hasToString("4.0");
        assertThat(stats.get("hasReview")).isEqualTo(true);
    }

    @Test
    @DisplayName("从未被评价时好评率为 0 而不是 null，且 hasReview=false")
    void handlesSellerWithoutReview() {
        when(reviewRepository.aggregateByTargetId(anyLong())).thenReturn(row(0L, 0L, "0"));

        Map<String, Object> stats = service.statsOf(SELLER_ID);

        assertThat((Long) stats.get("total")).isZero();
        assertThat(stats.get("goodRate")).hasToString("0.0");
        assertThat(stats.get("hasReview")).isEqualTo(false);
    }

    @Test
    @DisplayName("聚合查询返回 null 行时不抛异常")
    void handlesNullAggregateRow() {
        when(reviewRepository.aggregateByTargetId(SELLER_ID)).thenReturn(null);

        Map<String, Object> stats = service.statsOf(SELLER_ID);

        assertThat((Long) stats.get("total")).isZero();
        assertThat(stats.get("goodRate")).hasToString("0.0");
    }

    private Map<String, Object> row(Long total, Long good, String avg) {
        Map<String, Object> m = new HashMap<>();
        m.put("total", total);
        m.put("good", good);
        m.put("avg_rating", new java.math.BigDecimal(avg));
        return m;
    }
}