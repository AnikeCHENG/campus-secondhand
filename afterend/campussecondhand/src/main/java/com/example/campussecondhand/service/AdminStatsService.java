package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.OrderStatus;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台统计。
 *
 * <p>手续费口径：仅统计 {@code status IN (1,2,3)} 的订单，即已支付且未取消。
 * 待支付(0) 尚未收钱、已取消(4) 交易未成立，二者的 service_fee 都不构成平台收入；
 * 若一并累加，财务数字会在答辩时被追问且难以解释。</p>
 */
@Service
public class AdminStatsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final com.example.campussecondhand.repository.UserRepository userRepository;

    @Autowired
    public AdminStatsService(OrderRepository orderRepository,
                             ProductRepository productRepository,
                             com.example.campussecondhand.repository.UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * 平台财务概览：累计成交额、手续费总额、已支付订单数，以及按月手续费趋势。
     */
    public Map<String, Object> finance() {
        List<Order> paidOrders = selectPaidOrders();

        BigDecimal gmv = ZERO;
        BigDecimal fee = ZERO;
        for (Order order : paidOrders) {
            gmv = gmv.add(nz(order.getPrice()).add(
                    order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO));
            fee = fee.add(nz(order.getServiceFee()));
        }
        gmv = gmv.setScale(2, RoundingMode.HALF_UP);
        fee = fee.setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGmv", gmv);
        result.put("totalServiceFee", fee);
        result.put("paidOrderCount", paidOrders.size());
        result.put("serviceFeeRate", "0.3%");
        // 费率实际换算：综合费率，供对账时核对口径
        result.put("effectiveRate", gmv.signum() == 0
                ? "0.00%"
                : fee.multiply(BigDecimal.valueOf(100))
                        .divide(gmv, 2, RoundingMode.HALF_UP) + "%");
        result.put("sellerIncomeTotal", gmv.subtract(fee).setScale(2, RoundingMode.HALF_UP));
        result.put("monthlyTrend", monthlyFeeTrend(paidOrders));
        return result;
    }

    /**
     * 按月汇总手续费（近 6 个月），用于折线/柱状图。
     */
    private List<Map<String, Object>> monthlyFeeTrend(List<Order> paidOrders) {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> gmvByMonth = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            String key = firstDay.plusMonths(i).toString().substring(0, 7);
            byMonth.put(key, ZERO);
            gmvByMonth.put(key, ZERO);
        }
        for (Order order : paidOrders) {
            if (order.getPaidTime() == null) {
                continue;
            }
            String key = order.getPaidTime().toLocalDate().toString().substring(0, 7);
            if (!byMonth.containsKey(key)) {
                continue;
            }
            byMonth.put(key, byMonth.get(key).add(nz(order.getServiceFee())));
            gmvByMonth.put(key, gmvByMonth.get(key)
                    .add(nz(order.getPrice()).add(order.getShippingFee() != null
                            ? order.getShippingFee() : BigDecimal.ZERO)));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        byMonth.forEach((month, fee) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("serviceFee", fee);
            item.put("gmv", gmvByMonth.get(month));
            list.add(item);
        });
        return list;
    }

    /**
     * 近 N 天每日新增用户 / 商品 / 订单数量。
     *
     * <p>直接用 SQL 的 DATE(created_time) 分组，避免把全表拉进内存再分组。</p>
     */
    public Map<String, Object> dailyTrend(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", LocalDate.now().toString());
        result.put("userSeries", dailySeries("users", from, days));
        result.put("productSeries", dailySeries("products", from, days));
        result.put("orderSeries", dailySeries("orders", from, days));
        return result;
    }

    private List<Map<String, Object>> dailySeries(String table, LocalDate from, int days) {
        List<LocalDate> axis = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            axis.add(from.plusDays(i));
        }

        // 三张表的列名一致，用 Wrapper<?> 承载；具体类型由下面的分支收敛
        Wrapper<?> wrapper = Wrappers.<com.example.campussecondhand.entity.User>query()
                .select("DATE(created_time) AS d, COUNT(*) AS c")
                .ge("created_time", from.atStartOfDay())
                .groupBy("DATE(created_time)");

        List<Map<String, Object>> rows;
        if ("users".equals(table)) {
            rows = userRepository.selectMaps((Wrapper) wrapper);
        } else if ("products".equals(table)) {
            rows = productRepository.selectMaps((Wrapper) wrapper);
        } else {
            rows = orderRepository.selectMaps((Wrapper) wrapper);
        }

        Map<String, Long> counted = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            if (d == null) {
                continue;
            }
            String key = (d instanceof java.sql.Date) ? d.toString() : String.valueOf(d).substring(0, 10);
            counted.put(key, ((Number) row.get("c")).longValue());
        }

        List<Map<String, Object>> series = new ArrayList<>();
        for (LocalDate day : axis) {
            Map<String, Object> point = new LinkedHashMap<>();
            String key = day.toString();
            point.put("date", key);
            point.put("count", counted.getOrDefault(key, 0L));
            series.add(point);
        }
        return series;
    }

    /**
     * 商品分类交易热度。
     *
     * <p>数据源为 {@code products.category}。该字段目前由前端下拉约定取值，
     * 后端未做白名单校验（见论文「不足与展望」），因此这里对空值与未知值
     * 归入「未分类」，避免饼图出现空白分组。</p>
     */
    public List<Map<String, Object>> categoryHeat() {
        List<Product> products = productRepository.selectList(null);
        Map<String, Integer> productCount = new LinkedHashMap<>();
        Map<String, BigDecimal> soldAmount = new LinkedHashMap<>();

        for (Product p : products) {
            String key = (p.getCategory() == null || p.getCategory().isBlank())
                    ? "未分类" : p.getCategory();
            productCount.merge(key, 1, Integer::sum);
            soldAmount.merge(key, ZERO, BigDecimal::add);
            if (ProductStatus.isSold(p.getStatus())) {
                soldAmount.merge(key, nz(p.getPrice()), BigDecimal::add);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        productCount.forEach((category, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", category);
            item.put("label", categoryLabel(category));
            item.put("productCount", count);
            item.put("soldAmount", soldAmount.get(category).setScale(2, RoundingMode.HALF_UP));
            list.add(item);
        });
        list.sort((a, b) -> Integer.compare((Integer) b.get("productCount"), (Integer) a.get("productCount")));
        return list;
    }

    /** 分类英文 key 转中文标签，未知值原样返回 */
    private static String categoryLabel(String category) {
        return switch (category) {
            case "books" -> "图书教材";
            case "electronics" -> "电子产品";
            case "transport" -> "交通工具";
            case "gaming" -> "游戏设备";
            case "clothing" -> "服饰鞋包";
            case "living" -> "居家用品";
            case "other" -> "其他";
            default -> category;
        };
    }

    private List<Order> selectPaidOrders() {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.in("status", OrderStatus.PENDING_SHIPMENT.getCode(),
                OrderStatus.PENDING_RECEIPT.getCode(),
                OrderStatus.COMPLETED.getCode());
        return orderRepository.selectList(wrapper);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}