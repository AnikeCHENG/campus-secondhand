package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Product;
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

    public List<Product> findAllAvailable() {
        return productRepository.findAllAvailable();
    }

    public List<Product> findByUserId(Long userId) {
        return productRepository.findByUserId(userId);
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> searchByKeyword(String keyword) {
        return productRepository.searchByKeyword(keyword);
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
        product.setStatus(0);
        
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
            product.setStatus(1);
            product.setSoldTime(LocalDateTime.now());
            product.setUpdatedTime(LocalDateTime.now());
            productRepository.updateById(product);
            return true;
        }
        return false;
    }
}
