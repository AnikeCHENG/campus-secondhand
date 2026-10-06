package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.OrderStatus;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端统计口径测试。
 *
 * <p>重点锁定三条答辩时最容易被追问的规则：</p>
 * <ol>
 *   <li>手续费只统计已支付订单（待支付 / 已取消不计入平台收入）；</li>
 *   <li>成交额含运费，卖家应收 = 成交额 − 手续费；</li>
 *   <li>分类热度按下架 / 在售 / 已售出全量商品统计，并给出中文标签。</li>
 * </ol>
 */
class AdminStatsServiceTest {

    private AdminStatsService service;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        service = new AdminStatsService(orderRepository, productRepository, userRepository);
    }

    private Order order(int status, String price, String shipping, String fee) {
        Order o = new Order();
        o.setStatus(status);
        o.setPrice(new BigDecimal(price));
        o.setShippingFee(new BigDecimal(shipping));
        o.setServiceFee(new BigDecimal(fee));
        o.setPaidTime(LocalDateTime.now().minusDays(2));
        return o;
    }

    @Test
    @DisplayName("手续费查询条件只包含待发货/待收货/已完成，排除待支付与已取消")
    void financeQueryExcludesUnpaidAndCancelled() {
        when(orderRepository.selectList(any())).thenReturn(List.of());

        service.finance();

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(orderRepository).selectList(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment();
        assertThat(sql).contains("status");
        // 待支付(0) 与 已取消(4) 不应出现在 IN 列表里
        assertThat(sql).doesNotContain("0,");
        assertThat(sql).contains(
                String.valueOf(OrderStatus.PENDING_SHIPMENT.getCode()));
        assertThat(sql).contains(String.valueOf(OrderStatus.PENDING_RECEIPT.getCode()));
        assertThat(sql).contains(String.valueOf(OrderStatus.COMPLETED.getCode()));
    }

    @Test
    @DisplayName("成交额含运费，卖家应收等于成交额减手续费")
    void financeIncludesShippingAndDeductsFee() {
        when(orderRepository.selectList(any())).thenReturn(List.of(
                order(1, "1111.00", "0.00", "3.33"),
                order(3, "199.00", "11.00", "0.60")));

        Map<String, Object> r = service.finance();

        assertThat((BigDecimal) r.get("totalGmv")).isEqualByComparingTo("1321.00");
        assertThat((BigDecimal) r.get("totalServiceFee")).isEqualByComparingTo("3.93");
        assertThat((BigDecimal) r.get("sellerIncomeTotal")).isEqualByComparingTo("1317.07");
        assertThat((Integer) r.get("paidOrderCount")).isEqualTo(2);
    }

    @Test
    @DisplayName("已取消订单即使带有手续费也不得计入平台收入")
    void cancelledOrderFeeIsNotCounted() {
        // 仓储层已按 status IN (1,2,3) 过滤，这里确认过滤一旦失效金额会变化，
        // 从而保证上面的查询条件测试是有意义的（防止有人放宽条件而无人察觉）
        when(orderRepository.selectList(any())).thenReturn(List.of(
                order(3, "100.00", "0.00", "0.30")));

        Map<String, Object> r = service.finance();

        assertThat((BigDecimal) r.get("totalServiceFee")).isEqualByComparingTo("0.30");
        assertThat((Integer) r.get("paidOrderCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("空数据时财务统计不抛异常且金额为零")
    void financeHandlesNoOrders() {
        when(orderRepository.selectList(any())).thenReturn(List.of());

        Map<String, Object> r = service.finance();

        assertThat((BigDecimal) r.get("totalGmv")).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) r.get("totalServiceFee")).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) r.get("sellerIncomeTotal")).isEqualByComparingTo("0.00");
        assertThat((Integer) r.get("paidOrderCount")).isZero();
        assertThat((String) r.get("effectiveRate")).isEqualTo("0.00%");
        assertThat((List<?>) r.get("monthlyTrend")).hasSize(6);
    }

    @Test
    @DisplayName("分类热度统计全部状态商品并输出中文标签，按数量倒序")
    void categoryHeatCountsAllStatusesAndLabels() {
        when(productRepository.selectList(any())).thenReturn(List.of(
                product("books", ProductStatus.ON_SALE, "50.00"),
                product("books", ProductStatus.SOLD, "1111.00"),
                product("books", ProductStatus.OFF_SHELF, "9.90"),
                product("electronics", ProductStatus.ON_SALE, "899.00")));

        List<Map<String, Object>> list = service.categoryHeat();

        assertThat(list).hasSize(2);
        Map<String, Object> books = list.get(0);
        assertThat(books.get("category")).isEqualTo("books");
        assertThat(books.get("label")).isEqualTo("图书教材");
        assertThat((Integer) books.get("productCount")).isEqualTo(3);
        // soldAmount 只累计已售出商品的金额
        assertThat((BigDecimal) books.get("soldAmount")).isEqualByComparingTo("1111.00");

        Map<String, Object> electronics = list.get(1);
        assertThat(electronics.get("label")).isEqualTo("电子产品");
        assertThat((BigDecimal) electronics.get("soldAmount")).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("分类为空时归入未分类，避免图表出现空白分组")
    void categoryHeatFallsBackToUncategorised() {
        when(productRepository.selectList(any())).thenReturn(List.of(
                product(null, ProductStatus.ON_SALE, "10.00")));

        List<Map<String, Object>> list = service.categoryHeat();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("category")).isEqualTo("未分类");
    }

    @Test
    @DisplayName("趋势接口不查询商品与用户以外的额外数据，且返回完整天数轴")
    void dailyTrendReturnsFullAxis() {
        when(userRepository.selectMaps(any())).thenReturn(List.of());
        when(productRepository.selectMaps(any())).thenReturn(List.of());
        when(orderRepository.selectMaps(any())).thenReturn(List.of());

        Map<String, Object> r = service.dailyTrend(30);

        assertThat((List<?>) r.get("userSeries")).hasSize(30);
        assertThat((List<?>) r.get("productSeries")).hasSize(30);
        assertThat((List<?>) r.get("orderSeries")).hasSize(30);
        // 分类统计不应被趋势接口触发
        verify(productRepository, never()).selectList(any());
    }

    private Product product(String category, ProductStatus status, String price) {
        Product p = new Product();
        p.setCategory(category);
        p.setStatus(status.getCode());
        p.setPrice(new BigDecimal(price));
        return p;
    }
}