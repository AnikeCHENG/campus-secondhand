package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.PaymentRecord;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.OrderStatus;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.PaymentRecordRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.PaymentService.PayResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单领域服务：负责订单状态机、支付截止时间与收银台视图组装。
 *
 * <p>状态流转约束集中在此，Controller 只做鉴权与参数校验：</p>
 * <ul>
 *   <li>仅 {@link OrderStatus***REMOVED***PENDING_PAYMENT} 可支付，且未超过 {@code expire_time}</li>
 *   <li>仅 {@link OrderStatus***REMOVED***PENDING_PAYMENT} 可取消（已支付不允许取消）</li>
 *   <li>取消必须把商品恢复为在售，否则商品会被永久锁定</li>
 * </ul>
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final DateTimeFormatter ORDER_NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final OrderFeeService orderFeeService;
    private final PaymentService paymentService;
    private final TransactionTemplate transactionTemplate;

    /** 支付有效期（分钟）。收银台倒计时、支付校验、定时任务三处共用该口径。 */
    private final int payTimeoutMinutes;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        PaymentRecordRepository paymentRecordRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        ProductService productService,
                        OrderFeeService orderFeeService,
                        PaymentService paymentService,
                        TransactionTemplate transactionTemplate,
                        @Value("${order.pay-timeout-minutes:30}") int payTimeoutMinutes) {
        this.orderRepository = orderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productService = productService;
        this.orderFeeService = orderFeeService;
        this.paymentService = paymentService;
        this.transactionTemplate = transactionTemplate;
        this.payTimeoutMinutes = payTimeoutMinutes;
    }

    public int getPayTimeoutMinutes() {
        return payTimeoutMinutes;
    }

    public String getPaymentChannel() {
        return paymentService.channelName();
    }

    /**
     * 业务异常：由 Controller 转换为 400 响应。
     */
    public static class OrderBusinessException extends RuntimeException {
        public OrderBusinessException(String message) {
            super(message);
        }
    }

    /**
     * 生成订单编号：yyyyMMddHHmmss + 3 位随机数。
     *
     * <p>order_no 上有唯一索引，同秒并发下单有极小概率撞键，
     * 此时重新生成一次即可。</p>
     */
    private String generateOrderNo() {
        return ORDER_NO_FORMAT.format(LocalDateTime.now())
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /**
     * 创建订单并锁定商品。
     *
     * @param productId 商品 ID
     * @param buyerId   买家 ID
     * @return 已落库并回填主键的订单
     */
    @Transactional
    public Order createOrder(Long productId, Long buyerId) {
        Product product = productRepository.selectById(productId);
        if (product == null) {
            throw new OrderBusinessException("商品不存在");
        }
        // 仅在售商品允许下单（语义以 ProductStatus 为准）
        if (product.getStatus() == null || ProductStatus.isSold(product.getStatus())) {
            throw new OrderBusinessException("商品已售出");
        }
        if (ProductStatus.isOffShelf(product.getStatus())) {
            throw new OrderBusinessException("商品已下架");
        }
        if (product.getUserId() != null && product.getUserId().equals(buyerId)) {
            throw new OrderBusinessException("不能购买自己的商品");
        }

        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setProductId(productId);
        order.setOrderTitle(product.getTitle());
        order.setOrderImage(firstImage(product.getImages()));
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId() != null ? product.getUserId() : buyerId);
        order.setPrice(nz(product.getPrice()));
        // 校内自提免运费，保留字段以便扩展
        order.setShippingFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        order.setExpireTime(now.plusMinutes(payTimeoutMinutes));

        // 服务费由卖家承担：买家实付不含服务费
        BigDecimal price = nz(product.getPrice());
        User seller = userRepository.selectById(order.getSellerId());
        boolean studentVerified = seller != null
                && seller.getIsStudentVerified() != null
                && seller.getIsStudentVerified() == 1;
        BigDecimal fee = orderFeeService.calculateServiceFee(price, studentVerified);
        order.setServiceFee(fee);
        order.setSellerIncome(price.add(order.getShippingFee()).subtract(fee)
                .setScale(2, RoundingMode.HALF_UP));

        try {
            orderRepository.insert(order);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            order.setOrderNo(generateOrderNo());
            orderRepository.insert(order);
        }

        // 订单落库成功后再锁定商品，避免脏订单占用商品
        productService.markAsSold(productId);
        return order;
    }

    /**
     * 惰性过期处理：若订单仍待支付但已过支付截止时间，则自动取消并释放商品。
     *
     * <p>不依赖定时器，因此即使用户关闭浏览器未点取消，商品也不会被永久锁定。</p>
     *
     * <p>不加 {@code @Transactional}：本方法存在同类内部调用（见 payOrder），
     * 注解不会生效，反而造成误解。事务边界统一由 {@link TransactionTemplate} 控制。</p>
     *
     * @return 若本次调用触发了自动取消则返回 true
     */
    public boolean autoCancelIfExpired(Order order) {
        if (!OrderStatus.isPendingPayment(order.getStatus()) || !isExpired(order)) {
            return false;
        }
        doCancel(order);
        return true;
    }

    /**
     * 执行取消：改状态并把商品恢复为在售。
     *
     * <p>两步都必须做。只改订单状态会让商品永久停在「已售出」，
     * 之后无人能再购买它。</p>
     */
    private void doCancel(Order order) {
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setUpdatedTime(now);
        orderRepository.updateById(order);
        productService.markAsOnSale(order.getProductId());
    }

    /** 判断订单是否已过支付截止时间；未设置 expire_time 的历史订单视为未过期 */
    public boolean isExpired(Order order) {
        return order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now());
    }

    /**
     * 支付订单。全流程一个事务。
     *
     * <p><b>关键约束：业务失败一律通过返回值表达，绝不抛异常。</b>
     * 「超时取消」分支会先执行取消（含释放商品）再返回失败对象；
     * 若改为抛异常，异常将传播出事务边界导致整个事务回滚，
     * 取消与商品释放全部失效，商品被永久锁死在「已售出」。</p>
     *
     * <p>支付渠道由 {@link PaymentService} 抽象，本方法不感知具体渠道；
     * 渠道失败时直接返回其失败结果，不写入任何数据。</p>
     *
     * @param orderId   订单主键
     * @param payMethod 支付方式，原样记录（余额/支付宝/微信，不做白名单校验）
     * @param userId    当前登录用户，必须是买家
     */
    @Transactional
    public PayResult payOrder(Long orderId, String payMethod, Long userId) {
        Order order = requireOrder(orderId);

        // 归属校验：只有买家可支付
        if (order.getBuyerId() == null || !order.getBuyerId().equals(userId)) {
            return PayResult.fail(403, "只有买家可以支付该订单");
        }
        if (payMethod == null || payMethod.isBlank()) {
            return PayResult.fail("请选择支付方式");
        }

        // 过期检查：先走取消逻辑并释放商品，再返回失败（不可抛异常，见方法注释）
        if (isExpired(order)) {
            if (OrderStatus.isPendingPayment(order.getStatus())) {
                doCancel(order);
            }
            log.info("订单支付超时已自动取消: orderNo={}", order.getOrderNo());
            return PayResult.fail("订单已超时取消");
        }

        // 状态守卫：防止对同一订单重复支付
        if (!OrderStatus.isPendingPayment(order.getStatus())) {
            return PayResult.fail("当前订单状态不可支付");
        }

        // 委托支付渠道（Mock 实现永远成功）
        PayResult channelResult = paymentService.pay(order, payMethod);
        if (!channelResult.isSuccess()) {
            return channelResult;
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.PENDING_SHIPMENT.getCode());
        order.setPaymentMethod(payMethod);
        order.setPaidTime(now);
        order.setUpdatedTime(now);
        order.setTransactionId(channelResult.getTradeNo());
        orderRepository.updateById(order);

        // 落支付流水。amount 为买家实付（订单价 + 运费），不含卖家承担的服务费
        paymentRecordRepository.insert(buildPaymentRecord(order, payMethod,
                channelResult.getTradeNo(), now));

        log.info("支付成功: orderNo={}, payMethod={}, tradeNo={}",
                order.getOrderNo(), payMethod, channelResult.getTradeNo());
        return PayResult.ok(channelResult.getTradeNo());
    }

    private PaymentRecord buildPaymentRecord(Order order, String payMethod,
                                             String tradeNo, LocalDateTime now) {
        BigDecimal amount = nz(order.getPrice())
                .add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setAmount(amount);
        record.setPayMethod(payMethod);
        record.setTradeNo(tradeNo);
        record.setCreateTime(now);
        return record;
    }

    /**
     * 取消订单并把商品恢复为在售。
     *
     * <p>已支付的订单不允许取消（需走退款流程），避免资金已收却释放商品。</p>
     */
    public Order cancelOrder(Long orderId) {
        Order order = requireOrder(orderId);
        if (!OrderStatus.isPendingPayment(order.getStatus())) {
            throw new OrderBusinessException("当前订单状态（"
                    + labelOf(order.getStatus()) + "）不允许取消");
        }
        doCancel(order);
        return order;
    }

    /**
     * 扫描并取消所有超时未支付的订单（供定时任务调用）。
     *
     * <p>逐单独立事务：单条订单失败（如下架商品已被删除）不应影响其余订单，
     * 否则整批回滚会造成部分订单被重复扫描、迟迟无法释放。</p>
     *
     * <p>与 {@link ***REMOVED***autoCancelIfExpired} 是互补关系而非重复：
     * 定时任务负责用户完全离开后的兜底释放（最长延迟一个扫描周期），
     * 惰性检查负责扫描间隙内用户访问收银台时立即释放。两者同时存在，
     * 商品被锁定的窗口时间才趋近于零。</p>
     *
     * @return 本次成功取消的订单数
     */
    public int cancelTimeoutOrdersBatch() {
        LocalDateTime now = LocalDateTime.now();
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("status", OrderStatus.PENDING_PAYMENT.getCode())
                .isNotNull("expire_time")
                .lt("expire_time", now);
        List<Order> expired = orderRepository.selectList(wrapper);
        if (expired.isEmpty()) {
            return 0;
        }

        int cancelled = 0;
        for (Order order : expired) {
            try {
                final Long orderId = order.getId();
                Boolean done = transactionTemplate.execute(status -> {
                    Order fresh = orderRepository.selectById(orderId);
                    // 事务内重新确认状态，避免与用户支付并发导致误取消已支付订单
                    if (fresh == null || !OrderStatus.isPendingPayment(fresh.getStatus())
                            || !isExpired(fresh)) {
                        return false;
                    }
                    doCancel(fresh);
                    return true;
                });
                if (Boolean.TRUE.equals(done)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.error("超时取消订单失败: orderId={}, 原因={}", order.getId(), e.getMessage(), e);
            }
        }
        if (cancelled > 0) {
            log.info("定时任务取消超时订单 {} 笔", cancelled);
        }
        return cancelled;
    }

    /** 组装收银台所需的全部交易要素 */
    public Map<String, Object> buildCashierView(Order order) {
        Map<String, Object> view = new LinkedHashMap<>();

        view.put("id", order.getId());
        view.put("orderNo", order.getOrderNo());
        view.put("status", order.getStatus());
        view.put("statusLabel", labelOf(order.getStatus()));

        BigDecimal price = nz(order.getPrice());
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        // 买家实付 = 商品金额 + 运费（服务费由卖家承担，不计入买家实付）
        BigDecimal total = price.add(shipping).setScale(2, RoundingMode.HALF_UP);

        view.put("price", price);
        view.put("shippingFee", shipping);
        view.put("serviceFee", order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO);
        view.put("sellerIncome", order.getSellerIncome());
        view.put("total", total);

        // 商品快照：优先用下单时的快照，缺失时回落实时查询（兼容迁移前数据）
        String title = order.getOrderTitle();
        String image = order.getOrderImage();
        if (title == null || image == null) {
            Product product = productRepository.selectById(order.getProductId());
            if (product != null) {
                if (title == null) {
                    title = product.getTitle();
                }
                if (image == null) {
                    image = firstImage(product.getImages());
                }
            }
        }
        Map<String, Object> productView = new LinkedHashMap<>();
        productView.put("id", order.getProductId());
        productView.put("title", title != null ? title : "商品已删除");
        productView.put("image", image);
        view.put("product", productView);

        // 卖家信息
        Map<String, Object> sellerView = new LinkedHashMap<>();
        User seller = userRepository.selectById(order.getSellerId());
        sellerView.put("id", order.getSellerId());
        sellerView.put("nickname", seller != null ? seller.getUsername() : "未知卖家");
        sellerView.put("avatar", seller != null ? seller.getAvatar() : null);
        sellerView.put("studentVerified", seller != null
                && seller.getIsStudentVerified() != null
                && seller.getIsStudentVerified() == 1);
        view.put("seller", sellerView);

        view.put("createdTime", order.getCreatedTime());
        view.put("expireTime", order.getExpireTime());
        view.put("paidTime", order.getPaidTime());
        view.put("paymentMethod", order.getPaymentMethod());
        view.put("transactionId", order.getTransactionId());
        view.put("remainSeconds", remainSeconds(order));
        return view;
    }

    /** 剩余支付秒数；非待支付或已过期返回 0 */
    public long remainSeconds(Order order) {
        if (!OrderStatus.isPendingPayment(order.getStatus()) || order.getExpireTime() == null) {
            return 0L;
        }
        long seconds = java.time.Duration.between(LocalDateTime.now(), order.getExpireTime()).getSeconds();
        return Math.max(seconds, 0L);
    }

    public Order requireOrder(Long orderId) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }
        return order;
    }

    private static String labelOf(Integer status) {
        OrderStatus s = OrderStatus.fromCode(status);
        return s != null ? s.getLabel() : "未知状态";
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** 取逗号分隔图片列表的第一张 */
    private static String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        int idx = images.indexOf(',');
        return idx > 0 ? images.substring(0, idx) : images;
    }
}
