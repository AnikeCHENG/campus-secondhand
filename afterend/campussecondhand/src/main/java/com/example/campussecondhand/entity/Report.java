package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户举报。
 *
 * <p><b>字段命名说明</b>：数据库列是 {@code create_time}（按接口契约），
 * 而实体字段叫 {@code createdTime}，目的是让序列化出去的 JSON 字段名是
 * {@code createdTime} —— 管理端 {@code AdminReports.vue} 按该名字读取。
 * 仓库里 {@code users/orders/products} 用的是 {@code created_time} 列配
 * {@code createdTime} 字段，这里是同一套做法，只是列名按契约定。</p>
 */
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 被举报对象ID */
    private Long targetId;

    /** 被举报类型：PRODUCT 商品 / USER 用户 */
    private String targetType;

    /** 举报原因 */
    private String reason;

    /** 0 待处理 / 1 已处理并处罚 / 2 已驳回 */
    private Integer status = 0;

    /** 管理员处理备注，内含处罚动作留痕快照 */
    private String adminRemark;

    /**
     * 举报时间。
     *
     * <p>用 fill 而不是靠建表语句的 {@code DEFAULT CURRENT_TIMESTAMP}：
     * insert 会显式列出该列，数据库默认值不会生效。</p>
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 更新时间，由建表语句的 {@code ON UPDATE CURRENT_TIMESTAMP} 维护 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}