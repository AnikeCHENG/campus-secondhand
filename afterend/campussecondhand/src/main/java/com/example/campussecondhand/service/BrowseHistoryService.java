package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.BrowseHistory;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.BrowseHistoryRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览历史服务。
 *
 * <p>记录时机在商品详情接口内，但本服务不感知 HTTP：调用方解析不出用户时
 * 直接不调用即可，未登录浏览商品不会产生任何副作用。</p>
 */
@Service
public class BrowseHistoryService {

    /** 每个用户最多保留的历史条数 */
    public static final int MAX_HISTORY = 50;

    private final BrowseHistoryRepository browseHistoryRepository;
    private final ProductRepository productRepository;

    @Autowired
    public BrowseHistoryService(BrowseHistoryRepository browseHistoryRepository,
                                ProductRepository productRepository) {
        this.browseHistoryRepository = browseHistoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * 记录一次浏览并裁剪到 {@link ***REMOVED***MAX_HISTORY} 条。
     *
     * <p>先 upsert 再裁剪：upsert 会把该商品的浏览时间刷新到最新，
     * 因此它一定落在保留窗口内，不会被自己挤掉。</p>
     */
    public void recordView(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return;
        }
        browseHistoryRepository.upsertView(userId, productId);
        browseHistoryRepository.trimToRecent(userId, MAX_HISTORY);
    }

    /**
     * 浏览历史列表，按最近浏览时间倒序，最多 {@link ***REMOVED***MAX_HISTORY} 条。
     *
     * <p>已售出商品同样返回；商品被物理删除时给「商品已删除」占位。</p>
     */
    public List<Map<String, Object>> listHistory(Long userId) {
        List<BrowseHistory> histories = browseHistoryRepository.selectList(
                new QueryWrapper<BrowseHistory>()
                        .eq("user_id", userId)
                        .orderByDesc("last_view_time", "id")
                        .last("LIMIT " + MAX_HISTORY));

        List<Map<String, Object>> result = new ArrayList<>();
        for (BrowseHistory history : histories) {
            Product product = productRepository.selectById(history.getProductId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", history.getProductId());
            item.put("lastViewTime", history.getLastViewTime());
            if (product == null) {
                item.put("id", history.getProductId());
                item.put("title", "商品已删除");
                item.put("image", null);
                item.put("price", "0.00");
                item.put("status", null);
                item.put("statusLabel", "已删除");
                item.put("deleted", true);
            } else {
                item.put("id", product.getId());
                item.put("title", product.getTitle());
                item.put("image", FavoriteService.firstImage(product.getImages()));
                item.put("price", product.getPrice());
                item.put("status", product.getStatus());
                ProductStatus status = ProductStatus.fromCode(product.getStatus());
                item.put("statusLabel", status != null ? status.getLabel() : "未知");
                item.put("deleted", false);
            }
            result.add(item);
        }
        return result;
    }

    /** 清空当前用户的浏览历史 */
    public int clearHistory(Long userId) {
        return browseHistoryRepository.deleteByUserId(userId);
    }
}
