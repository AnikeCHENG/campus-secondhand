package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campussecondhand.common.PageParam;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
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
     * 记录一次浏览并裁剪到 {@link #MAX_HISTORY} 条。
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
     * 浏览历史列表（物理分页），按最近浏览时间倒序。
     *
     * <p>已售出商品同样返回；商品被物理删除时给「商品已删除」占位。</p>
     *
     * <p><b>此处不再拼 {@code LIMIT 50}</b>：保留 50 条的规则由写入路径
     * {@link #recordView} 中的 {@code trimToRecent(userId, 50)} 在<b>写入时</b>删除旧行保证，
     * 表内每个用户本就最多 50 行。原先读取时再拼一次 LIMIT 会与分页拦截器追加的
     * {@code LIMIT ?, ?} 撞成 {@code LIMIT 50 LIMIT ?, ?} 直接语法错误。
     * 业务规则不变，只是从"查询期截断"改为依赖已有的"写入期裁剪"。</p>
     *
     * <p>商品按当页 id 批量查询，避免逐条 selectById。</p>
     */
    public IPage<Map<String, Object>> pageHistory(Long userId, PageParam paging) {
        IPage<BrowseHistory> historyPage = browseHistoryRepository.selectPage(
                paging.toPage(),
                new QueryWrapper<BrowseHistory>()
                        .eq("user_id", userId)
                        .orderByDesc("last_view_time", "id"));

        List<BrowseHistory> histories = historyPage.getRecords();

        List<Long> productIds = histories.stream()
                .map(BrowseHistory::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> products = productIds.isEmpty()
                ? Collections.emptyMap()
                : productRepository.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        List<Map<String, Object>> items = new ArrayList<>();
        for (BrowseHistory history : histories) {
            Product product = products.get(history.getProductId());
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
            items.add(item);
        }

        IPage<Map<String, Object>> result = new Page<>(paging.page(), paging.size(), historyPage.getTotal());
        result.setRecords(items);
        return result;
    }

    /** 清空当前用户的浏览历史 */
    public int clearHistory(Long userId) {
        return browseHistoryRepository.deleteByUserId(userId);
    }
}
