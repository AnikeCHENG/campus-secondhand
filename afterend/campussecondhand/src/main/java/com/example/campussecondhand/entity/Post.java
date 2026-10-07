package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 大厅动态。
 *
 * <p>与 {@link Product} 的关联：仅 {@code SELL}/{@code FREE} 且发布时携带
 * productInfo 时，{@link #productId} 才非空（由 {@code PostService} 在同一事务内
 * 建商品后回填）。其余类型恒为 null，前端据此隐藏点赞/评论按钮。</p>
 */
@TableName("posts")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 类型，取值见 {@link com.example.campussecondhand.enums.PostType} */
    @TableField("type")
    private String type;

    @TableField("content")
    private String content;

    /** 逗号分隔的标签串，落库形态 */
    @TableField("tags")
    private String tags;

    /** 逗号分隔的图片串（Base64 或 URL），落库形态 */
    @TableField("images")
    private String images;

    /** 关联商品ID；非商品类动态为 null */
    @TableField("product_id")
    private Long productId;

    @TableField("created_time")
    private LocalDateTime createdTime;

    /* ---------- 以下为接口返回用的非持久化字段 ---------- */

    /** 发布者公开信息（username / avatar / isStudentVerified） */
    @TableField(exist = false)
    private User author;

    /** 关联商品快照；productId 为空时为 null */
    @TableField(exist = false)
    private Product product;

    /** 类型中文标签，供前端直接展示 */
    @TableField(exist = false)
    private String typeLabel;

    /** 点赞数；仅商品类动态统计，非商品类恒为 0 */
    @TableField(exist = false)
    private Integer likeCount = 0;

    /** 评论数；仅商品类动态统计，非商品类恒为 0 */
    @TableField(exist = false)
    private Integer commentCount = 0;

    @TableField(exist = false)
    private Integer shareCount = 0;

    public Post() {}

    /** 逗号分隔串 → 字符串数组；空串/空白返回空列表，绝不返回 null */
    public List<String> tagList() {
        return splitToList(tags);
    }

    public List<String> imageList() {
        return splitToList(images);
    }

    private static List<String> splitToList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getTypeLabel() { return typeLabel; }
    public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }
}