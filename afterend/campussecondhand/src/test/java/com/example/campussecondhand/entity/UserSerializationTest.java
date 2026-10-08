package com.example.campussecondhand.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 敏感字段不得出现在任何 JSON 响应里。
 *
 * <p>这个测试的价值在于「钉死默认行为」。之前 password / studentNo / realName
 * 三��都能从 {@code /api/posts} 这个公开接口直接拿到——只要有人往实体上加个
 * {@code @JsonIgnore}，这里立刻红。</p>
 */
class UserSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 构造一个字段齐全的 User。
     *
     * <p>密码用占位串而非真实账号的 BCrypt 哈希：测试要验证的是「password
     * 不出现在 JSON 里」，与哈希值本身无关。而真实哈希一旦提交进仓库，
     * 等于把该账号的凭据公开——本项目 admin 用的还是弱口令，
     * BCrypt_10 对常见密码抗性极低。占位串保留同样的格式特征，
     * 足够让断言 hasSize/contains 等检查生效。</p>
     */
    private User fullUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("admin");
        u.setEmail("admin@campus.com");
        u.setPassword("$2a$10$TESTHASHnotARealCredentialAAAAAAAAAAAAAAAAAAAAAAA");
        u.setPhone("13800000000");
        u.setStudentNo("202012345678");
        u.setRealName("张三");
        u.setRole(1);
        return u;
    }

    @Test
    @DisplayName("password 绝不出现在序列化结果里")
    void passwordIsNeverSerialized() throws Exception {
        String json = mapper.writeValueAsString(fullUser());
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("$2a$10$");
    }

    @Test
    @DisplayName("学号与真实姓名不随实体自动带出")
    void piiIsNotAutoSerialized() throws Exception {
        String json = mapper.writeValueAsString(fullUser());
        assertThat(json).doesNotContain("studentNo");
        assertThat(json).doesNotContain("realName");
        assertThat(json).doesNotContain("202012345678");
        assertThat(json).doesNotContain("张三");
    }

    @Test
    @DisplayName("非敏感字段照常返回，不能把整个对象都吞掉")
    void harmlessFieldsStillSerialized() throws Exception {
        String json = mapper.writeValueAsString(fullUser());
        assertThat(json).contains("\"username\":\"admin\"");
        assertThat(json).contains("\"email\":\"admin@campus.com\"");
        assertThat(json).contains("\"phone\":\"13800000000\"");
    }

    @Test
    @DisplayName("WRITE_ONLY 仍允许反序列化进来")
    void stillDeserializable() throws Exception {
        // 反序列化必须能写入 password，否则将来若有接口用 User 接输入会静默丢字段
        User parsed = mapper.readValue(
                "{\"username\":\"x\",\"password\":\"plain\"}", User.class);
        assertThat(parsed.getUsername()).isEqualTo("x");
        assertThat(parsed.getPassword()).isEqualTo("plain");
    }
}