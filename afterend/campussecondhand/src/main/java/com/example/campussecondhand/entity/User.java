package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("password")
    private String password;

    @TableField("phone")
    private String phone;

    @TableField("avatar")
    private String avatar;

    @TableField("bio")
    private String bio;

    @TableField("location")
    private String location;

    @TableField("qq")
    private String qq;

    @TableField("wechat")
    private String wechat;

    @TableField("status")
    private Integer status = 1;

    @TableField("is_student_verified")
    private Integer isStudentVerified = 0;

    /** 学号；唯一索引保证一个学号只能对应一个账号，防止冒用他人身份骗取免手续费 */
    @TableField("student_no")
    private String studentNo;

    /** 真实姓名，仅认证时填写，不在公开接口中返回 */
    @TableField("real_name")
    private String realName;

    @TableField("role")
    private Integer role = 0; // 0: 普通用户, 1: 管理员

    @TableField(value = "created_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField("deleted")
    private Integer deleted = 0;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getQq() { return qq; }
    public void setQq(String qq) { this.qq = qq; }

    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getIsStudentVerified() { return isStudentVerified; }
    public void setIsStudentVerified(Integer isStudentVerified) { this.isStudentVerified = isStudentVerified; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    /** 是否已认证：null 视为未认证，避免历史脏数据被当成已认证 */
    public boolean isStudentVerifiedUser() {
        return isStudentVerified != null && isStudentVerified == 1;
    }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
