package com.example.campussecondhand.controller;

import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 明文口令迁移为 BCrypt 的测试。
 *
 * <p>这个接口写错的后果比不写更严重：二次编码会让所有用户当场无法登录。
 * 因此三个场景必须锁死——已加密的绝不能碰、空口令不能凭空造、
 * 明文的必须真的变成 60 位 {@code $2a$} 密文且原文能通过校验。</p>
 */
@ExtendWith(MockitoExtension.class)
class DevMigrationControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private DevMigrationController controller;

    private static User user(Long id, String username, String password) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPassword(password);
        return u;
    }

    private static final String BCRYPT_60 = "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ012345678";

    @Test
    @DisplayName("已是 BCrypt 的用户不被二次编码——重复调用不会让密码失效")
    void doesNotRehashAlreadyHashedPassword() {
        when(userRepository.findPlaintextPasswordUsers(anyInt(), anyString()))
                .thenReturn(List.of(user(1L, "admin", BCRYPT_60)));

        var response = controller.migratePasswords();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        // 关键断言：一次 update 都不能有
        verify(userRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("明文口令被编码为 BCrypt 并回写")
    void rehashesPlaintextPassword() {
        User victim = user(2L, "buyer1", "123456");
        when(userRepository.findPlaintextPasswordUsers(anyInt(), anyString()))
                .thenReturn(List.of(victim));
        when(passwordEncoder.encode("123456")).thenReturn(BCRYPT_60);

        controller.migratePasswords();

        verify(userRepository, times(1)).updateById(any());
        assertThat(victim.getPassword()).isEqualTo(BCRYPT_60);
    }

    @Test
    @DisplayName("空口令不会被编码——否则等于凭空造出一个可登录密码")
    void skipsBlankPassword() {
        User blank = user(3L, "nouser", null);
        when(userRepository.findPlaintextPasswordUsers(anyInt(), anyString()))
                .thenReturn(List.of(blank));

        controller.migratePasswords();

        verify(userRepository, never()).updateById(any());
        verify(passwordEncoder, never()).encode(any());
        assertThat(blank.getPassword()).isNull();
    }

    @Test
    @DisplayName("SQL 层已下推筛选条件，不再全表 selectAll")
    void pushesCandidateFilterIntoSql() {
        when(userRepository.findPlaintextPasswordUsers(anyInt(), anyString()))
                .thenReturn(List.of());

        controller.migratePasswords();

        ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).findPlaintextPasswordUsers(lenCaptor.capture(), prefixCaptor.capture());
        assertThat(lenCaptor.getValue()).isEqualTo(20);
        assertThat(prefixCaptor.getValue()).isEqualTo("$2");
        // 全表扫描会把全部用户读进内存，迁移脚本本身不该这么干
        verify(userRepository, never()).selectList(any());
    }

    @Test
    @DisplayName("真实 BCrypt 编码器产出的密文可通过校验，且非明文")
    void encodedPasswordIsVerifiableBcrypt() {
        PasswordEncoder real = new BCryptPasswordEncoder();
        String encoded = real.encode("123456");

        assertThat(encoded).hasSize(60).startsWith("$2a$");
        assertThat(real.matches("123456", encoded)).isTrue();
        assertThat(real.matches("1234567", encoded)).isFalse();
        assertThat(encoded).isNotEqualTo("123456");
    }

    @Test
    @DisplayName("同一明文两次编码结果不同（加盐随机），但都能通过校验")
    void saltMakesHashesDifferentYetBothVerify() {
        PasswordEncoder real = new BCryptPasswordEncoder();

        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            hashes.add(real.encode("same-password"));
        }

        assertThat(hashes.get(0)).isNotEqualTo(hashes.get(1));
        assertThat(real.matches("same-password", hashes.get(0))).isTrue();
        assertThat(real.matches("same-password", hashes.get(1))).isTrue();
    }

    @Test
    @DisplayName("判据只匹配疑似明文的候选行")
    void selectsOnlySuspiciousRows() {
        when(userRepository.findPlaintextPasswordUsers(20, "$2"))
                .thenReturn(List.of(user(4L, "old_user", "abc123")));

        var response = controller.migratePasswords();

        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = (Map<String, Object>) response.getBody().getData();
        assertThat((Integer) body.get("candidates")).isEqualTo(1);
    }
}