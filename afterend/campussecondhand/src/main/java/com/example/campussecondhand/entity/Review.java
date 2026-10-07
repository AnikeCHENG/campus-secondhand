package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 订单评价。
 *
 * <p>一单一评，由 {@code review.uk_order_id} 唯一索引保证。
 * 该约束不能只靠应用层判断：并发双击"提交评价"时两个请求可能同时通过
 * "还没评价过"的检查，必须让数据库做最终裁决。</p>
 */
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    /** 评价人，即买家 */
    private Long reviewerId;

    /** 被评价人，即卖家 */
    private Long targetId;

    /** 评分 1~5；4 分及以上计为好评 */
    private Integer rating;

    /** 评价内容，限 50 字 */
    private String content;

    /**
     * 评价时间。
     *
     * <p>由 {@code MyBatisMetaObjectHandler} 在插入时填充，与其他实体的
     * {@code createdTime} 走同一套机制。建表语句里的
     * {@code DEFAULT CURRENT_TIMESTAMP} 只是双保险——因为 insert 语句
     * 会显式列出这一列，数据库默认值根本不会生效。</p>
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}