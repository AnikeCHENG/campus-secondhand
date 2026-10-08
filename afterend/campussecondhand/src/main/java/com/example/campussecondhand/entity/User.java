package com.example.campussecondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    /**
     * BCrypt 哈希。
     *
     * <p>{@code @JsonProperty(access = WRITE_ONLY)} 而非 {@code @JsonIgnore}：
     * 前者禁止「序列化出去」但保留「反序列化进来」，后者两个方向都禁。
     * 这里两者都不需要，但 WRITE_ONLY 语义更准——万一将来有接口用 User
     * 反序列化接收输入（比如资料更新），用 @JsonIgnore 会静默丢字段。</p>
     *
     * <p>为什么必须在实体上拦而不是在每个 Controller 拦：{@code /api/posts}
     * 是公开接口，它把 User 整个塞进 author 字段返回。任何"逐个 Controller
     * 检查"的做法都会在新增接口时漏掉——默认序列化整个实体的写法太顺手了。
     * 在实体上设默认拒绝，是唯一能让漏写也安全的做法。</p>
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    /**
     * 学号；唯一索引保证一个学号只能对应一个账号，防止冒用他人身份骗取免手续费。
     *
     * <p>WRITE_ONLY：学号 + 真实姓名是一对实名组合，能直接定位到具体学生。
     * {@code /api/posts} 是公开接口且把整个 User 塞进 author 返回，
     * 不拦的话等于任何人无需登录即可批量拿到全校用户的实名信息。
     * 需要读学号的场景走 {@code /api/user/profile}（仅本人）或管理端接口。</p>
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField("student_no")
    private String studentNo;

    /** 真实姓名，仅认证时填写。与学号同理由 WRITE_ONLY 保护。 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
