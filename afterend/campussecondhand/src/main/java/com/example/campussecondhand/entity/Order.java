package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("buyer_id")
    private Long buyerId;

    @TableField("seller_id")
    private Long sellerId;

    @TableField("product_id")
    private Long productId;

    /** 商品标题快照，避免商品被改/删后订单信息失真 */
    @TableField("order_title")
    private String orderTitle;

    /** 商品首图快照（base64 Data URL），取下单时 products.images 的第一张 */
    @TableField("order_image")
    private String orderImage;

    @TableField("price")
    private BigDecimal price;

    /** 运费。校内自提免运费，当前恒为 0，保留字段以便扩展邮费规则 */
    @TableField("shipping_fee")
    private BigDecimal shippingFee;

    /** 状态语义以 {@link com.example.campussecondhand.enums.OrderStatus} 枚举为准 */
    @TableField("status")
    private Integer status;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableField("paid_time")
    private LocalDateTime paidTime;

    /** 支付截止时间（下单时间 + 30 分钟），过期后由惰性检查自动取消 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("completed_time")
    private LocalDateTime completedTime;

    @TableField("service_fee")
    private BigDecimal serviceFee;

    @TableField("seller_income")
    private BigDecimal sellerIncome;

    @TableField(exist = false)
    private User buyer;

    @TableField(exist = false)
    private User seller;

    @TableField(exist = false)
    private Product product;

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getOrderTitle() { return orderTitle; }
    public void setOrderTitle(String orderTitle) { this.orderTitle = orderTitle; }
    public String getOrderImage() { return orderImage; }
    public void setOrderImage(String orderImage) { this.orderImage = orderImage; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public LocalDateTime getPaidTime() { return paidTime; }
    public void setPaidTime(LocalDateTime paidTime) { this.paidTime = paidTime; }

    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }

    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }

    public BigDecimal getSellerIncome() { return sellerIncome; }
    public void setSellerIncome(BigDecimal sellerIncome) { this.sellerIncome = sellerIncome; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}