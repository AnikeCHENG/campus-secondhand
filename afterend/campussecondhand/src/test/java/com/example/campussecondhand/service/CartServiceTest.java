package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Cart;
import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.CartRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物车服务测试。
 *
 * <p>最关键的一条是 {@link #checkout_keepsEarlierOrdersWhenOneItemFails}：
 * 结算循环刻意不加事务，若哪天有人"顺手"加上 @Transactional，
 * 批量下单时一件失败会让前面已成的订单一起回滚——这条测试就是拦住那种回归。</p>
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 6L;
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private CartService service;

    private Product product(int status, Long ownerId) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setTitle("捷安特山地车");
        p.setImages("data:image/jpeg;base64,AAAA");
        p.setPrice(new BigDecimal("380.00"));
        p.setStatus(status);
        p.setUserId(ownerId);
        return p;
    }

    @SuppressWarnings("unchecked")
    private List<Long> orderIdsOf(Map<String, Object> result) {
        return (List<Long>) result.get("orderIds");
    }

    private Order order(Long id, String price) {
        Order o = new Order();
        o.setId(id);
        o.setPrice(new BigDecimal(price));
        o.setShippingFee(BigDecimal.ZERO);
        return o;
    }

    // ==================== 加购 ====================

    @Test
    @DisplayName("加购在售商品成功")
    void addsOnSaleProduct() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));

        service.add(USER_ID, PRODUCT_ID);

        verify(cartRepository).insert(any(Cart.class));
    }

    @Test
    @DisplayName("重复加购被唯一索引拦下，转成友好提示而不是数据库异常")
    void translatesDuplicateKey() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));
        when(cartRepository.insert(any(Cart.class)))
                .thenThrow(new DuplicateKeyException("uk_user_product"));

        assertThatThrownBy(() -> service.add(USER_ID, PRODUCT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("已在购物车");
    }

    @Test
    @DisplayName("已下架与已售出商品都不能加购")
    void rejectsUnavailableProduct() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.OFF_SHELF.getCode(), 99L));
        assertThatThrownBy(() -> service.add(USER_ID, PRODUCT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("该商品不可购买");

        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.SOLD.getCode(), 99L));
        assertThatThrownBy(() -> service.add(USER_ID, PRODUCT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("该商品不可购买");

        verify(cartRepository, never()).insert(any());
    }

    @Test
    @DisplayName("不能购买自己的商品")
    void rejectsOwnProduct() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), USER_ID));

        assertThatThrownBy(() -> service.add(USER_ID, PRODUCT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("不能购买自己的商品");
    }

    // ==================== 列表 ====================

    @Test
    @DisplayName("在售商品 valid=true，下架/已售出 valid=false 并给出原因")
    void marksValidityPerItem() {
        when(cartRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of(10L, 11L, 12L));
        Product onSale = product(ProductStatus.ON_SALE.getCode(), 99L);
        Product sold = product(ProductStatus.SOLD.getCode(), 99L);
        sold.setId(11L);
        Product offShelf = product(ProductStatus.OFF_SHELF.getCode(), 99L);
        offShelf.setId(12L);
        when(productRepository.selectBatchIds(any())).thenReturn(List.of(onSale, sold, offShelf));
        when(userRepository.selectBatchIds(any())).thenReturn(List.of());

        List<Map<String, Object>> items = service.listItems(USER_ID);

        assertThat(items).hasSize(3);
        assertThat(items.get(0).get("valid")).isEqualTo(true);
        assertThat(items.get(1).get("valid")).isEqualTo(false);
        assertThat(items.get(1).get("invalidReason")).isEqualTo("商品已售出");
        assertThat(items.get(2).get("invalidReason")).isEqualTo("商品已下架");
    }

    @Test
    @DisplayName("商品被物理删除时保留该行并标记无效，而不是静默丢弃")
    void keepsHardDeletedItemAsInvalid() {
        when(cartRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of(PRODUCT_ID));
        when(productRepository.selectBatchIds(any())).thenReturn(List.of());

        List<Map<String, Object>> items = service.listItems(USER_ID);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("valid")).isEqualTo(false);
        assertThat(items.get(0).get("title")).isEqualTo("商品已删除");
    }

    @Test
    @DisplayName("空购物车返回空列表且不查商品表")
    void returnsEmptyForEmptyCart() {
        when(cartRepository.findProductIdsByUserId(USER_ID)).thenReturn(List.of());

        assertThat(service.listItems(USER_ID)).isEmpty();
        verify(productRepository, never()).selectBatchIds(any());
    }

    // ==================== 结算 ====================

    @Test
    @DisplayName("全部成功：返回 orderIds 与合计金额，并清空对应购物车行")
    void checkoutAllSucceed() {
        when(orderService.createOrder(10L, USER_ID)).thenReturn(order(1L, "100.00"));
        when(orderService.createOrder(11L, USER_ID)).thenReturn(order(2L, "200.00"));

        Map<String, Object> result = service.checkout(USER_ID, List.of(10L, 11L));

        assertThat(orderIdsOf(result)).containsExactly(1L, 2L);
        assertThat((List<?>) result.get("skipped")).isEmpty();
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo("300.00");
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartRepository).deleteByProductIds(eq(USER_ID), captor.capture());
        assertThat(captor.getValue()).containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("部分失败：前面成功的订单仍然保留，失败件记入 skipped 且留在购物车")
    void checkoutKeepsEarlierOrdersWhenOneItemFails() {
        when(orderService.createOrder(10L, USER_ID)).thenReturn(order(1L, "100.00"));
        // 第 2 件被别人抢先下单
        when(orderService.createOrder(11L, USER_ID))
                .thenThrow(new OrderService.OrderBusinessException("商品已售出"));
        when(orderService.createOrder(12L, USER_ID)).thenReturn(order(3L, "50.00"));

        Map<String, Object> result = service.checkout(USER_ID, List.of(10L, 11L, 12L));

        // 关键断言：中间一件失败不能拖垮前后两件
        assertThat(orderIdsOf(result)).containsExactly(1L, 3L);
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo("150.00");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skipped = (List<Map<String, Object>>) result.get("skipped");
        assertThat(skipped).hasSize(1);
        assertThat(skipped.get(0).get("productId")).isEqualTo(11L);
        assertThat(skipped.get(0).get("reason")).isEqualTo("商品已售出");

        // 购物车只清理成功的 10 与 12，失败件 11 必须留着供重试
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartRepository).deleteByProductIds(eq(USER_ID), captor.capture());
        assertThat(captor.getValue()).containsExactly(10L, 12L);
    }

    @Test
    @DisplayName("全部失败：orderIds 为空且不清理购物车")
    void checkoutAllFail() {
        when(orderService.createOrder(10L, USER_ID))
                .thenThrow(new OrderService.OrderBusinessException("商品已售出"));

        Map<String, Object> result = service.checkout(USER_ID, List.of(10L));

        assertThat((List<?>) result.get("orderIds")).isEmpty();
        assertThat(((List<?>) result.get("skipped")).size()).isEqualTo(1);
        verify(cartRepository, never()).deleteByProductIds(anyLong(), any());
    }

    @Test
    @DisplayName("非预期异常也只跳过该件，不中断整批")
    void checkoutContinuesOnUnexpectedException() {
        when(orderService.createOrder(10L, USER_ID)).thenReturn(order(1L, "100.00"));
        when(orderService.createOrder(11L, USER_ID)).thenThrow(new RuntimeException("db down"));

        Map<String, Object> result = service.checkout(USER_ID, List.of(10L, 11L));

        assertThat(orderIdsOf(result)).containsExactly(1L);
        assertThat(((List<?>) result.get("skipped")).size()).isEqualTo(1);
    }

    @Test
    @DisplayName("空选中列表直接拒绝")
    void checkoutRejectsEmptySelection() {
        assertThatThrownBy(() -> service.checkout(USER_ID, List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("请选择要结算的商品");
        verify(orderService, never()).createOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("重复的 productId 只下单一次")
    void checkoutDeduplicatesProductIds() {
        when(orderService.createOrder(10L, USER_ID)).thenReturn(order(1L, "100.00"));

        service.checkout(USER_ID, List.of(10L, 10L, 10L));

        verify(orderService, times(1)).createOrder(eq(10L), eq(USER_ID));
    }

    @Test
    @DisplayName("移出购物车保持幂等")
    void removeIsIdempotent() {
        service.remove(USER_ID, PRODUCT_ID);
        verify(cartRepository).deleteOne(USER_ID, PRODUCT_ID);
    }
}
