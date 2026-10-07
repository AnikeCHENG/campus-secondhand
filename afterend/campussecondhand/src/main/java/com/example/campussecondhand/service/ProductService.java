package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 全量在售商品。<b>仅供一次性数据迁移脚本使用</b>——见
     * {@code ProductController#updateImages}：该端点要遍历全部商品并逐个改图或删除，
     * 分页会漏掉不在当前页的记录，语义上必须是全量。
     *
     * <p>面向用户的列表请改用 {@link #pageAvailable}，那是带 LIMIT 的物理分页。</p>
     */
    public List<Product> findAllAvailable() {
        return productRepository.findByStatus(ProductStatus.ON_SALE.getCode());
    }

    /**
     * 大厅在售商品的<b>物理分页</b>查询：筛选与排序全部下推到 SQL。
     *
     * <p>此前大厅把全量商品一次性返回、由前端在内存里做分类/关键词/价格/成色过滤与排序。
     * 改成服务端分页后若仍保留那套前端逻辑，筛选只会作用在当前页的十几条记录上，
     * 结果直接失真——所以筛选与排序必须与分页在同一条 SQL 内完成。</p>
     *
     * <p>{@code condition} 是 MySQL 保留字（建表语句中已加反引号），
     * {@code eq("condition", …)} 会直接语法报错，必须写成 {@code `condition`}。</p>
     *
     * @param paging 分页参数（已由 {@code PageParam} 校验）
     * @param sort   {@code newest} / {@code priceAsc} / {@code priceDesc} / {@code hot}，
     *               未知值按 {@code newest} 处理
     */
    public IPage<Product> pageAvailable(PageParam paging, String category, String keyword,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        String condition, String sort) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", ProductStatus.ON_SALE.getCode());

        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category);
        }
        if (keyword != null && !keyword.isBlank()) {
            // 用 and(...) 包裹，避免 OR 泄漏到后续价格等条件之外
            wrapper.and(q -> q.like("title", keyword).or().like("description", keyword));
        }
        if (minPrice != null) {
            wrapper.ge("price", minPrice);
        }
        if (maxPrice != null) {
            wrapper.le("price", maxPrice);
        }
        if (condition != null && !condition.isBlank()) {
            // 反引号不可省：condition 是 SQL 保留字
            wrapper.eq("`condition`", condition);
        }

        switch (sort == null ? "" : sort) {
            case "priceAsc" -> wrapper.orderByAsc("price");
            case "priceDesc" -> wrapper.orderByDesc("price");
            case "hot" -> wrapper.orderByDesc("view_count");
            default -> wrapper.orderByDesc("created_time");
        }

        return productRepository.selectPage(paging.toPage(), wrapper);
    }

    public List<Product> findByUserId(Long userId) {
        return productRepository.findByUserId(userId);
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategoryAndStatus(category, ProductStatus.ON_SALE.getCode());
    }

    public List<Product> searchByKeyword(String keyword) {
        return productRepository.searchByKeywordAndStatus(keyword, ProductStatus.ON_SALE.getCode());
    }

    public Product findById(Long id) {
        return productRepository.selectById(id);
    }

    public Product findByIdWithViewCount(Long id) {
        Product product = productRepository.selectById(id);
        if (product != null) {
            product.setViewCount(product.getViewCount() + 1);
            productRepository.updateById(product);
        }
        return product;
    }

    // 根据商品名称自动分类
    private String autoCategorize(String title) {
        // 分类映射
        Map<String, List<String>> categoryMapping = new HashMap<>();
        categoryMapping.put("books", Arrays.asList("书", "教材", "课本", "小说", "简史", "传", "四世同堂", "在细雨中呼喊", "堂吉坷德", "明朝那些事儿", "活着", "狂人日记", "许三观卖血记", "资治通鉴"));
        categoryMapping.put("electronics", Arrays.asList("手机", "电脑", "平板", "耳机", "相机", "电子"));
        categoryMapping.put("transport", Arrays.asList("自行车", "电动车", "出行", "车"));
        categoryMapping.put("gaming", Arrays.asList("游戏", "手柄", "键盘", "鼠标", "游戏机"));
        categoryMapping.put("clothing", Arrays.asList("衣服", "裤子", "鞋子", "帽子", "服饰"));
        categoryMapping.put("living", Arrays.asList("生活用品", "家具", "被子", "枕头", "厨具"));
        
        // 匹配分类
        for (Map.Entry<String, List<String>> entry : categoryMapping.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            for (String keyword : keywords) {
                if (title.contains(keyword)) {
                    return category;
                }
            }
        }
        return "other"; // 默认分类
    }

    public Product create(Product product) {
        product.setCreatedTime(LocalDateTime.now());
        product.setUpdatedTime(LocalDateTime.now());
        product.setViewCount(0);
        // 新发布商品默认在售
        product.setStatus(ProductStatus.ON_SALE.getCode());
        
        // 自动分类
        if (product.getCategory() == null || product.getCategory().isEmpty()) {
            product.setCategory(autoCategorize(product.getTitle()));
        }
        
        productRepository.insert(product);
        return product;
    }

    public Product update(Product product) {
        product.setUpdatedTime(LocalDateTime.now());
        productRepository.updateById(product);
        return product;
    }

    public int delete(Long id) {
        return productRepository.deleteById(id);
    }

    public boolean markAsSold(Long id) {
        Product product = productRepository.selectById(id);
        if (product != null) {
            // 成交后置为已售出
            product.setStatus(ProductStatus.SOLD.getCode());
            product.setSoldTime(LocalDateTime.now());
            product.setUpdatedTime(LocalDateTime.now());
            productRepository.updateById(product);
            return true;
        }
        return false;
    }

    /**
     * 订单取消/超时后把商品恢复为在售。
     *
     * <p>同时清空 sold_time，否则商品虽回到在售状态却仍带着成交时间，
     * 导致「在售商品却显示已售出时间」的数据不一致。</p>
     *
     * @param id 商品 ID
     * @return 商品存在并已恢复时返回 true
     */
    public boolean markAsOnSale(Long id) {
        Product product = productRepository.selectById(id);
        if (product != null) {
            product.setStatus(ProductStatus.ON_SALE.getCode());
            product.setSoldTime(null);
            product.setUpdatedTime(LocalDateTime.now());
            productRepository.updateById(product);
            return true;
        }
        return false;
    }
}
