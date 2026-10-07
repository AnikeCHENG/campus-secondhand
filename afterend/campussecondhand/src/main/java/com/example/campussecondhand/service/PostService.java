package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campussecondhand.entity.Post;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.PostType;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.PostRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 大厅动态服务。
 *
 * <p>核心约定：{@code SELL}/{@code FREE} 且携带 productInfo 时，
 * 在<b>同一事务</b>内先建商品（status=1 在售）再回填 {@code posts.product_id}，
 * 从而保证「大厅动态」与「商品页」同时出现同一条商品。</p>
 */
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductService productService;

    /** 发布动态入参（已脱离 HTTP 层，便于单测） */
    public record CreateCommand(
            String type,
            String content,
            List<String> tags,
            List<String> images,
            String productTitle,
            BigDecimal productPrice,
            String productDescription,
            String productCategory
    ) {}

    /**
     * 创建动态。
     *
     * @param userId 发布者
     * @param cmd   已解析的入参
     * @return 创建后的动态（含 productId / typeLabel）
     */
    @Transactional(rollbackFor = Exception.class)
    public Post create(Long userId, CreateCommand cmd) {
        PostType type = PostType.fromCode(cmd.type());
        if (type == null) {
            throw new BadRequestException("未知的动态类型：" + cmd.type());
        }

        String content = cmd.content() == null ? "" : cmd.content().trim();
        boolean hasImage = cmd.images() != null && !cmd.images().isEmpty();
        if (content.isEmpty() && !hasImage) {
            throw new BadRequestException("请输入动态内容或上传图片");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setType(type.name());
        post.setTypeLabel(type.getLabel());
        post.setContent(content);
        post.setTags(joinOrNull(cmd.tags()));
        post.setImages(joinOrNull(cmd.images()));
        post.setCreatedTime(LocalDateTime.now());

        // 仅 SELL/FREE 且带 productInfo 时联动建商品
        if (type.isAllowsProduct() && hasProductInfo(cmd)) {
            Product product = buildProduct(userId, type, cmd);
            // create() 内部已置 status=ON_SALE(1) 与自动分类
            productService.create(product);
            post.setProductId(product.getId());
        }

        postRepository.insert(post);
        return post;
    }

    /** 带商品快照、作者信息与互动计数的列表；type 为 null 表示不限类型 */
    public List<Post> list(String typeCode, int page, int size) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        PostType type = PostType.fromCode(typeCode);
        if (typeCode != null && !typeCode.isBlank() && type == null) {
            // 传了非法 type：按铁律不静默降级为「全部」，直接返回空列表由前端隐藏筛选态
            return List.of();
        }
        if (type != null) {
            wrapper.eq("type", type.name());
        }
        wrapper.orderByDesc("created_time", "id");

        List<Post> posts = postRepository.selectList(wrapper);
        enrich(posts);
        return posts;
    }

    /**
     * 批量补齐展示字段：作者、商品快照、互动计数。
     *
     * <p>所有关联查询都做了批量 IN + Map 回填，避免逐条 selectById 的 N+1。</p>
     */
    public void enrich(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        // 类型标签
        for (Post post : posts) {
            PostType type = PostType.fromCode(post.getType());
            post.setTypeLabel(type != null ? type.getLabel() : null);
            post.setLikeCount(0);
            post.setCommentCount(0);
            post.setShareCount(0);
        }

        // 作者（一次 IN 查全部）
        List<Long> userIds = posts.stream()
                .map(Post::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userRepository.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            posts.forEach(p -> p.setAuthor(userMap.get(p.getUserId())));
        }

        // 关联商品快照
        List<Long> productIds = posts.stream()
                .map(Post::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (!productIds.isEmpty()) {
            Map<Long, Product> productMap = productRepository.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            posts.forEach(p -> p.setProduct(productMap.get(p.getProductId())));
        }

        // 互动计数：只对商品类动态（productId 非空）统计。
        // 现有互动接口挂在 /api/products 下，like_record 等表无 target_type 列，
        // post 与 product 的 ID 会碰撞，故非商品类动态一律保持 0。
        if (productIds.isEmpty()) {
            return;
        }
        Map<Long, Long> likeMap = toCountMap(postRepository.countLikesByTargets(productIds));
        Map<Long, Long> commentMap = toCountMap(postRepository.countCommentsByTargets(productIds));
        Map<Long, Long> shareMap = toCountMap(postRepository.countSharesByTargets(productIds));
        posts.forEach(p -> {
            if (p.getProductId() == null) {
                return;
            }
            p.setLikeCount(likeMap.getOrDefault(p.getProductId(), 0L).intValue());
            p.setCommentCount(commentMap.getOrDefault(p.getProductId(), 0L).intValue());
            p.setShareCount(shareMap.getOrDefault(p.getProductId(), 0L).intValue());
        });
    }

    /** hall-profile：一次接口返回三条统计，全部为 SQL 聚合 */
    public Map<String, Object> countStats(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("posts", postRepository.countByUser(userId));
        stats.put("selling", productRepository.selectCount(
                new QueryWrapper<Product>().eq("user_id", userId).eq("status", 1)));
        stats.put("seeking", postRepository.countByUserAndType(userId, PostType.SEEK.name()));
        return stats;
    }

    /** 一次性取回一页动态（含 enrich），供 Controller 直接使用 */
    public List<Post> page(String typeCode, int offset, int size) {
        List<Post> all = list(typeCode, 1, Integer.MAX_VALUE);
        if (offset >= all.size()) {
            return List.of();
        }
        int to = Math.min(offset + size, all.size());
        return new ArrayList<>(all.subList(offset, to));
    }

    /* ---------------- 内部工具 ---------------- */

    private boolean hasProductInfo(CreateCommand cmd) {
        return cmd.productTitle() != null && !cmd.productTitle().isBlank();
    }

    private Product buildProduct(Long userId, PostType type, CreateCommand cmd) {
        BigDecimal price = cmd.productPrice();
        if (type.isPriceRequired()) {
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("出售动态必须填写大于 0 的价格");
            }
        } else if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            // FREE 允许不填价格（记 0），但显式填了负数一律拒绝。
            // 注意此处不能写成 price == null || ...：null 正是 FREE 的合法输入。
            throw new BadRequestException("价格不能为负数");
        }

        Product product = new Product();
        product.setUserId(userId);
        product.setTitle(cmd.productTitle().trim());
        product.setDescription(cmd.productDescription() != null && !cmd.productDescription().isBlank()
                ? cmd.productDescription().trim()
                : cmd.content());   // 缺省用动态正文作为商品描述，避免商品页空白
        product.setPrice(price != null ? price : BigDecimal.ZERO);
        if (cmd.productCategory() != null && !cmd.productCategory().isBlank()) {
            product.setCategory(cmd.productCategory().trim());
        }
        if (cmd.images() != null && !cmd.images().isEmpty()) {
            product.setImages(joinOrNull(cmd.images()));
        }
        return product;
    }

    private static Map<Long, Long> toCountMap(List<PostRepository.CountRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>();
        for (PostRepository.CountRow row : rows) {
            map.put(row.getTargetId(), row.getCnt() == null ? 0L : row.getCnt());
        }
        return map;
    }

    /** 字符串数组 → 逗号分隔串；空数组返回 null（避免写入空字符串） */
    private static String joinOrNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> cleaned = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }
}