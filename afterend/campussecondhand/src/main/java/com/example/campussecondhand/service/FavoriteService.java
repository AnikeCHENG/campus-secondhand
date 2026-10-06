package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Favorite;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.FavoriteRepository;
import com.example.campussecondhand.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务。
 *
 * <p>集合操作全部以 (userId, productId) 为维度，重复调用保持幂等——
 * 详情页的收藏按钮可能被连点，或用户同时开了两个标签页。</p>
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    @Autowired
    public FavoriteService(FavoriteRepository favoriteRepository,
                           ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    /**
     * 收藏商品。已收藏时直接返回，不报错。
     *
     * <p>需吞掉 {@code DuplicateKeyException}：唯一索引存在的前提下，
     * 「先查后插」仍可能因并发双击而撞键，若不兜住会变成 500。</p>
     */
    @Transactional
    public boolean addFavorite(Long userId, Long productId) {
        if (isFavorited(userId, productId)) {
            return false;
        }
        try {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            favoriteRepository.insert(favorite);
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发下已被其他请求收藏，视为成功
            return false;
        }
    }

    /** 取消收藏。不存在也返回成功（幂等） */
    @Transactional
    public boolean removeFavorite(Long userId, Long productId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Favorite> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        return favoriteRepository.delete(wrapper) > 0;
    }

    /** 是否已收藏，供详情页回显收藏状态 */
    public boolean isFavorited(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Favorite> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        return favoriteRepository.selectCount(wrapper) > 0;
    }

    /**
     * 我的收藏列表，按收藏时间倒序。
     *
     * <p>商品已售出（status=2）或已下架（status=0）**依然返回**，
     * 由前端加「已售出」角标置灰，而不是在后端过滤掉——
     * 用户收藏时商品在售，卖出后理应看到「已售出」而不是凭空消失。</p>
     *
     * <p>商品被物理删除时（{@code ProductService.delete} 是硬删除），
     * 返回「商品已删除」占位，避免前端拿到 null 字段崩溃。</p>
     */
    public List<Map<String, Object>> listFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Favorite>()
                        .eq("user_id", userId)
                        // created_time 只有秒级精度，追加 id 作为 tiebreaker 保证顺序确定
                        .orderByDesc("created_time", "id"));

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Favorite favorite : favorites) {
            Product product = productRepository.selectById(favorite.getProductId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", favorite.getProductId());
            item.put("createTime", favorite.getCreatedTime());
            applyProduct(item, product);
            result.add(item);
        }
        return result;
    }

    /** 把商品字段填入列表项；商品已删除时给出占位 */
    private void applyProduct(Map<String, Object> item, Product product) {
        if (product == null) {
            item.put("id", item.get("productId"));
            item.put("title", "商品已删除");
            item.put("image", null);
            item.put("price", "0.00");
            item.put("status", null);
            item.put("statusLabel", "已删除");
            item.put("deleted", true);
            item.put("sellerName", "");
            return;
        }
        item.put("id", product.getId());
        item.put("title", product.getTitle());
        item.put("image", firstImage(product.getImages()));
        item.put("price", product.getPrice());
        item.put("status", product.getStatus());
        ProductStatus status = ProductStatus.fromCode(product.getStatus());
        item.put("statusLabel", status != null ? status.getLabel() : "未知");
        item.put("deleted", false);
        item.put("sellerName", product.getUserId() != null ? String.valueOf(product.getUserId()) : "");
    }

    /**
     * 取多图列表中的第一张。
     *
     * <p>{@code products.images} 存的是 base64 Data URL，载荷内部本身含逗号，
     * 按第一个逗号切分会得到 {@code data:image/jpeg;base64} 这种残缺 URL。
     * base64 字母表不含逗号，故从 {@code base64,} 之后找下一个逗号才是真正的分隔符。</p>
     */
    static String firstImage(String images) {
        return OrderService.firstImage(images);
    }
}
