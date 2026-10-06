package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.BrowseHistory;
import com.example.campussecondhand.entity.Favorite;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.BrowseHistoryRepository;
import com.example.campussecondhand.repository.FavoriteRepository;
import com.example.campussecondhand.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收藏与浏览历史测试。
 *
 * <p>重点覆盖四条业务约束：</p>
 * <ol>
 *   <li>重复收藏幂等，且并发撞唯一索引不能变成 500</li>
 *   <li>已售出商品必须仍然出现在收藏/历史列表中</li>
 *   <li>浏览历史超过上限后裁剪到最近 N 条</li>
 *   <li>商品被物理删除时返回占位而非 null</li>
 * </ol>
 */
class FavoriteAndHistoryTest {

    private static final Long USER_ID = 3L;
    private static final Long PRODUCT_ID = 5L;

    private FavoriteRepository favoriteRepository;
    private ProductRepository productRepository;
    private BrowseHistoryRepository browseHistoryRepository;
    private FavoriteService favoriteService;
    private BrowseHistoryService historyService;

    @BeforeEach
    void setUp() {
        favoriteRepository = mock(FavoriteRepository.class);
        productRepository = mock(ProductRepository.class);
        browseHistoryRepository = mock(BrowseHistoryRepository.class);
        favoriteService = new FavoriteService(favoriteRepository, productRepository);
        historyService = new BrowseHistoryService(browseHistoryRepository, productRepository);
    }

    private Product product(int status) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setTitle("捷安特山地车");
        p.setPrice(new BigDecimal("380.00"));
        p.setImages("data:image/jpeg;base64,AAAA,data:image/jpeg;base64,BBBB");
        p.setStatus(status);
        p.setUserId(1L);
        return p;
    }

    // ==================== 收藏 ====================

    @Test
    void add_favorite_inserts_when_not_favorited() {
        when(favoriteRepository.selectCount(any())).thenReturn(0L);
        when(favoriteRepository.insert(any(Favorite.class))).thenReturn(1);

        assertThat(favoriteService.addFavorite(USER_ID, PRODUCT_ID)).isTrue();
        verify(favoriteRepository).insert(any(Favorite.class));
    }

    @Test
    void add_favorite_is_idempotent_when_already_favorited() {
        when(favoriteRepository.selectCount(any())).thenReturn(1L);

        // 重复收藏不报错、不重复插入
        assertThat(favoriteService.addFavorite(USER_ID, PRODUCT_ID)).isFalse();
        verify(favoriteRepository, never()).insert(any(Favorite.class));
    }

    @Test
    void add_favorite_swallows_duplicate_key_from_concurrent_click() {
        // 先查后插仍可能因并发双击撞 uk_user_product，若不兜住会变成 500
        when(favoriteRepository.selectCount(any())).thenReturn(0L);
        when(favoriteRepository.insert(any(Favorite.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry"));

        assertThat(favoriteService.addFavorite(USER_ID, PRODUCT_ID)).isFalse();
    }

    @Test
    void remove_favorite_is_idempotent() {
        when(favoriteRepository.delete(any())).thenReturn(0);

        // 未收藏时删除也返回成功，不报错
        assertThat(favoriteService.removeFavorite(USER_ID, PRODUCT_ID)).isFalse();
    }

    @Test
    void check_returns_false_for_null_args() {
        assertThat(favoriteService.isFavorited(null, PRODUCT_ID)).isFalse();
        assertThat(favoriteService.isFavorited(USER_ID, null)).isFalse();
    }

    @Test
    void favorite_list_keeps_sold_product_and_marks_it() {
        when(favoriteRepository.selectList(any())).thenReturn(List.of(favorite(PRODUCT_ID)));
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(product(ProductStatus.SOLD.getCode()));

        List<Map<String, Object>> list = favoriteService.listFavorites(USER_ID);

        // 关键断言：已售出商品不得被过滤掉
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("status")).isEqualTo(ProductStatus.SOLD.getCode());
        assertThat(list.get(0).get("statusLabel")).isEqualTo("已售出");
        assertThat(list.get(0).get("deleted")).isEqualTo(false);
    }

    @Test
    void favorite_list_keeps_off_shelf_product() {
        when(favoriteRepository.selectList(any())).thenReturn(List.of(favorite(PRODUCT_ID)));
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(product(ProductStatus.OFF_SHELF.getCode()));

        assertThat(favoriteService.listFavorites(USER_ID)).hasSize(1);
    }

    @Test
    void favorite_list_returns_placeholder_when_product_was_hard_deleted() {
        // ProductService.delete 是硬删除，商品可能真的不存在
        when(favoriteRepository.selectList(any())).thenReturn(List.of(favorite(PRODUCT_ID)));
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(null);

        List<Map<String, Object>> list = favoriteService.listFavorites(USER_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("title")).isEqualTo("商品已删除");
        assertThat(list.get(0).get("deleted")).isEqualTo(true);
    }

    @Test
    void favorite_list_takes_only_first_image() {
        when(favoriteRepository.selectList(any())).thenReturn(List.of(favorite(PRODUCT_ID)));
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(product(ProductStatus.ON_SALE.getCode()));

        List<Map<String, Object>> list = favoriteService.listFavorites(USER_ID);

        // base64 载荷内部含逗号，不能被截断
        assertThat(list.get(0).get("image")).isEqualTo("data:image/jpeg;base64,AAAA");
    }

    /**
     * 回归测试：多图 Data URL 的第一张图切分。
     *
     * <p>曾经用 {@code indexOf(',')} 切分，得到的��
     * {@code data:image/jpeg;base64} —— 丢了整个 base64 载荷，图片彻底失效。
     * 该缺陷已实际损坏过数据库中的 order_image 快照。</p>
     */
    @Test
    void first_image_keeps_full_base64_payload_when_multiple_images() {
        String images = "data:image/jpeg;base64,/9j/4AAQSkZJRg==,data:image/png;base64,iVBORw0KGgo=";

        String first = OrderService.firstImage(images);

        assertThat(first).isEqualTo("data:image/jpeg;base64,/9j/4AAQSkZJRg==");
        // 绝不能退化成只剩前缀
        assertThat(first).doesNotEndWith("base64");
        assertThat(first).contains("/9j/");
    }

    @Test
    void first_image_handles_single_and_empty_inputs() {
        assertThat(OrderService.firstImage("data:image/png;base64,AAAA")).isEqualTo("data:image/png;base64,AAAA");
        assertThat(OrderService.firstImage(null)).isNull();
        assertThat(OrderService.firstImage("")).isNull();
        // 非 data URL 时退回按逗号分隔
        assertThat(OrderService.firstImage("/img/a.jpg,/img/b.jpg")).isEqualTo("/img/a.jpg");
    }

    private Favorite favorite(Long productId) {
        Favorite f = new Favorite();
        f.setId(1L);
        f.setUserId(USER_ID);
        f.setProductId(productId);
        f.setCreatedTime(LocalDateTime.now());
        return f;
    }

    // ==================== 浏览历史 ====================

    private BrowseHistory history(Long productId, LocalDateTime time) {
        BrowseHistory h = new BrowseHistory();
        h.setId(productId);
        h.setUserId(USER_ID);
        h.setProductId(productId);
        h.setLastViewTime(time);
        return h;
    }

    @Test
    void record_view_upserts_then_trims() {
        historyService.recordView(USER_ID, PRODUCT_ID);

        // 顺序必须是先 upsert 再裁剪：upsert 刷新了时间，该商品一定在保留窗口内
        verify(browseHistoryRepository).upsertView(USER_ID, PRODUCT_ID);
        verify(browseHistoryRepository).trimToRecent(USER_ID, BrowseHistoryService.MAX_HISTORY);
    }

    @Test
    void record_view_ignores_null_args() {
        historyService.recordView(null, PRODUCT_ID);
        historyService.recordView(USER_ID, null);

        verify(browseHistoryRepository, never()).upsertView(anyLong(), anyLong());
    }

    @Test
    void history_limit_is_fifty() {
        assertThat(BrowseHistoryService.MAX_HISTORY).isEqualTo(50);
    }

    @Test
    void history_list_is_capped_at_fifty() {
        List<BrowseHistory> rows = new java.util.ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            rows.add(history(i, LocalDateTime.now().minusMinutes(i)));
        }
        when(browseHistoryRepository.selectList(any())).thenReturn(rows);
        when(productRepository.selectById(any())).thenReturn(product(ProductStatus.ON_SALE.getCode()));

        assertThat(historyService.listHistory(USER_ID)).hasSize(50);
    }

    @Test
    void history_list_returns_newest_first_and_marks_sold() {
        LocalDateTime now = LocalDateTime.now();
        // selectList 已按 last_view_time DESC 排序，这里模拟 A 最新、B 较旧
        when(browseHistoryRepository.selectList(any())).thenReturn(List.of(
                history(1L, now), history(2L, now.minusMinutes(5))));
        when(productRepository.selectById(1L)).thenReturn(product(ProductStatus.SOLD.getCode()));
        when(productRepository.selectById(2L)).thenReturn(product(ProductStatus.ON_SALE.getCode()));

        List<Map<String, Object>> list = historyService.listHistory(USER_ID);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("productId")).isEqualTo(1L);
        assertThat(list.get(0).get("statusLabel")).isEqualTo("已售出");
        assertThat(list.get(1).get("statusLabel")).isEqualTo("在售");
    }

    @Test
    void history_list_handles_deleted_product() {
        when(browseHistoryRepository.selectList(any())).thenReturn(List.of(history(9L, LocalDateTime.now())));
        when(productRepository.selectById(9L)).thenReturn(null);

        List<Map<String, Object>> list = historyService.listHistory(USER_ID);

        assertThat(list.get(0).get("title")).isEqualTo("商品已删除");
    }

    @Test
    void repeated_view_of_same_product_never_duplicates_row() {
        // 连续浏览同一商品 3 次，upsert 只更新时间
        for (int i = 0; i < 3; i++) {
            historyService.recordView(USER_ID, PRODUCT_ID);
        }

        // 依赖 uk_user_product 唯一索引；若索引缺失，upsert 会退化为重复插入
        verify(browseHistoryRepository, times(3)).upsertView(USER_ID, PRODUCT_ID);
    }

    @Test
    void clear_history_deletes_only_current_user() {
        when(browseHistoryRepository.deleteByUserId(USER_ID)).thenReturn(12);

        assertThat(historyService.clearHistory(USER_ID)).isEqualTo(12);
        verify(browseHistoryRepository).deleteByUserId(USER_ID);
        verify(browseHistoryRepository, never()).deleteByUserId(eq(1L));
    }
}
