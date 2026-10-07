package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.campussecondhand.enums.ConditionLevel;
import com.example.campussecondhand.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("price")
    private BigDecimal price;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("category")
    private String category;

    @TableField("condition_level")
    private Integer conditionLevel;

    @TableField("flaw_description")
    private String flawDescription;

    @TableField("images")
    private String images;

    @TableField("view_count")
    private Integer viewCount = 0;

    /**
     * 商品状态，取值语义见 {@link com.example.campussecondhand.enums.ProductStatus}。
     * 禁止在业务代码中直接使用字面量 0/1/2，请统一引用该枚举。
     */
    @TableField("status")
    private Integer status = ProductStatus.ON_SALE.getCode();

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    /**
     * 成交时间。
     *
     * <p>MyBatis-Plus 默认忽略 null 字段（NOT_NULL 策略），订单取消时
     * {@code setSoldTime(null)} 不会落库，会导致商品已恢复在售却仍残留成交时间。
     * 因此这里必须显式改为 IGNORED，使 null 也能写入。</p>
     */
    @TableField(value = "sold_time", updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime soldTime;

    @TableField(exist = false)
    private User seller;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }

    public String getFlawDescription() { return flawDescription; }
    public void setFlawDescription(String flawDescription) { this.flawDescription = flawDescription; }

    /**
     * 成色的中文标签，供不需要引入枚举的调用方直接展示。
     *
     * <p>刻意不在实体里存标签字符串：那是冗余数据，改枚举忘了同步就会自相矛盾。
     * 需要展示时调 {@link ConditionLevel#fromCode(Integer)} 现算。</p>
     */
    public String getConditionLabel() {
        ConditionLevel level = ConditionLevel.fromCode(conditionLevel);
        return level != null ? level.getLabel() : "未标注成色";
    }

    /** 是否声明无瑕疵：瑕疵说明为空即视为卖家承诺无明显瑕疵 */
    public boolean isFlawFree() {
        return flawDescription == null || flawDescription.isBlank();
    }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public LocalDateTime getSoldTime() { return soldTime; }
    public void setSoldTime(LocalDateTime soldTime) { this.soldTime = soldTime; }

    @JsonIgnore
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
}
