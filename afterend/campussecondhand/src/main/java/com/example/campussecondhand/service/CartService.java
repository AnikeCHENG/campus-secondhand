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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务。
 *
 * <p><b>结算的事务边界是本类的关键设计</b>：{@code checkout} 刻意不加
 * {@code @Transactional}。批量下单时若把整个循环包在一个事务里，
 * 第 3 件商品校验失败会让前 2 件已创建的订单一起回滚——
 * 用户看到的 {@code orderIds} 为空、什么都没买到，而前两件商品
 * 却已被 {@code createOrder} 锁定为已售出，状态错乱且难以排查。</p>
 *
 * <p>因此这里逐件调用 {@link OrderService#createOrder}，让每笔订单各自
 * 在自己的事务里提交；失败件记入 {@code skipped} 并继续处理其余商品。
 * 同理，结算完成后才批量清理购物车，而不是每成功一件就删一次。</p>
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Autowired
    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       OrderService orderService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    /**
     * 加入购物车。
     *
     * @throws BadRequestException 商品不存在、已下架/已售出、是自己的商品、或已在购物车
     */
    @Transactional
    public void add(Long userId, Long productId) {
        if (productId == null) {
            throw new BadRequestException("请选择商品");
        }
        Product product = productRepository.selectById(productId);
        if (product == null) {
            throw new BadRequestException("商品不存在");
        }
        // 只有在售（status=1）能加购；下架与已售出都不该进购物车
        if (product.getStatus() == null
                || product.getStatus() != ProductStatus.ON_SALE.getCode()) {
            throw new BadRequestException("该商品不可购买");
        }
        if (product.getUserId() != null && product.getUserId().equals(userId)) {
            throw new BadRequestException("不能购买自己的商品");
        }

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        try {
            cartRepository.insert(cart);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 幂等：已存在时给"已在购物车"的友好提示，而不是抛数据库异常
            throw new BadRequestException("已在购物车");
        }
    }

    /** 移出购物车；未加入过也算成功，保持幂等 */
    @Transactional
    public void remove(Long userId, Long productId) {
        cartRepository.deleteOne(userId, productId);
    }

    /** 购物车件数 */
    public long count(Long userId) {
        return cartRepository.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Cart>()
                        .eq("user_id", userId));
    }

    /**
     * 购物车列表，附带商品信息与有效性标记。
     *
     * <p>商品被物理删除后购物车会留下悬空行，此时 {@code valid=false}
     * 且标题为"商品已删除"，而不是把这条记录悄悄丢掉。</p>
     */
    public List<Map<String, Object>> listItems(Long userId) {
        List<Long> productIds = cartRepository.findProductIdsByUserId(userId);
        if (productIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = productRepository.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Long> sellerIds = products.values().stream()
                .map(Product::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, User> sellers = sellerIds.isEmpty() ? Map.of()
                : userRepository.selectBatchIds(sellerIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map<String, Object>> items = new ArrayList<>(productIds.size());
        for (Long productId : productIds) {
            Product product = products.get(productId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", productId);
            if (product == null) {
                item.put("title", "商品已删除");
                item.put("image", null);
                item.put("price", BigDecimal.ZERO);
                item.put("status", null);
                item.put("sellerName", "");
                item.put("valid", false);
                item.put("invalidReason", "商品已删除");
                items.add(item);
                continue;
            }
            item.put("title", product.getTitle());
            item.put("image", product.getImages());
            item.put("price", product.getPrice());
            item.put("status", product.getStatus());
            User seller = sellers.get(product.getUserId());
            item.put("sellerName", seller != null ? seller.getUsername() : "未知卖家");
            boolean valid = product.getStatus() != null
                    && product.getStatus() == ProductStatus.ON_SALE.getCode();
            item.put("valid", valid);
            item.put("invalidReason", valid ? null : invalidReasonOf(product));
            items.add(item);
        }
        return items;
    }

    private String invalidReasonOf(Product product) {
        if (product.getStatus() != null && ProductStatus.SOLD.getCode() == product.getStatus()) {
            return "商品已售出";
        }
        if (product.getStatus() != null && ProductStatus.OFF_SHELF.getCode() == product.getStatus()) {
            return "商品已下架";
        }
        return "商品不可购买";
    }

    /**
     * 结算：把购物车中勾选的商品逐件下单。
     *
     * <p><b>本方法刻意不加 {@code @Transactional}</b>，理由见类注释。
     * 每件商品各自调用 {@link OrderService#createOrder}（自带事务），
     * 因此单件失败不影响其余商品。</p>
     *
     * @param productIds 勾选的商品 ID 列表
     * @return {@code {orderIds, totalAmount, skipped:[{productId, reason}]}}
     */
    public Map<String, Object> checkout(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BadRequestException("请选择要结算的商品");
        }
        List<Long> ordered = productIds.stream()
                .filter(Objects::nonNull).distinct().toList();
        if (ordered.isEmpty()) {
            throw new BadRequestException("请选择要结算的商品");
        }

        List<Long> orderIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

        for (Long productId : ordered) {
            try {
                Order order = orderService.createOrder(productId, userId);
                orderIds.add(order.getId());
                BigDecimal price = order.getPrice() == null ? BigDecimal.ZERO : order.getPrice();
                BigDecimal shipping = order.getShippingFee() == null ? BigDecimal.ZERO : order.getShippingFee();
                totalAmount = totalAmount.add(price).add(shipping);
            } catch (OrderService.OrderBusinessException e) {
                // 单件失败只跳过这一件，其余继续下单
                skipped.add(Map.of("productId", productId, "reason", e.getMessage()));
            } catch (Exception e) {
                log.warn("结算商品 {} 失败: {}", productId, e.getMessage());
                skipped.add(Map.of("productId", productId, "reason", "该商品暂时无法下单"));
            }
        }

        if (orderIds.isEmpty()) {
            log.warn("用户 {} 结算 {} 件商品全部失败", userId, ordered.size());
        } else {
            // 只清理真正下单成功的商品，失败件留在购物车里供用户重试
            cartRepository.deleteByProductIds(userId, succeededIds(ordered, skipped));
            log.info("用户 {} 结算成功 {} 件，跳过 {} 件", userId, orderIds.size(), skipped.size());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderIds", orderIds);
        result.put("totalAmount", totalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        result.put("skipped", skipped);
        return result;
    }

    /**
     * 从勾选列表里剔除下单失败的商品，只清成功的那些。
     *
     * <p>失败件必须留在购物车：它们可能只是被别人抢先下单了，
     * 商品恢复在售后用户还应该能买到。</p>
     */
    private List<Long> succeededIds(List<Long> ordered, List<Map<String, Object>> skipped) {
        List<Long> skippedIds = skipped.stream()
                .map(s -> (Long) s.get("productId")).toList();
        return ordered.stream().filter(id -> !skippedIds.contains(id)).toList();
    }
}
