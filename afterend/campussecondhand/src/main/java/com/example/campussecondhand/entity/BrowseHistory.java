package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 浏览历史。
 *
 * <p>(user_id, product_id) 唯一：同一用户对同一商品只保留一条记录，
 * 重复浏览只更新 {@code last_view_time}。</p>
 */
@TableName("browse_history")
public class BrowseHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("product_id")
    private Long productId;

    @TableField("last_view_time")
    private LocalDateTime lastViewTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public LocalDateTime getLastViewTime() {
        return lastViewTime;
    }

    public void setLastViewTime(LocalDateTime lastViewTime) {
        this.lastViewTime = lastViewTime;
    }
}
