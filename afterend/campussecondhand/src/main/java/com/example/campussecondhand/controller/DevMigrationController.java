package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次性数据迁移工具，<b>仅在 dev profile 下注册</b>。
 *
 * <p>{@code @Profile("dev")} 打在类上：非 dev 环境根本不创建这个 Bean，
 * 因此不存在"生产环境误调用把密码批量重写"的风险，比在方法里判断 profile 更难绕过。</p>
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevMigrationController {

    private static final Logger log = LoggerFactory.getLogger(DevMigrationController.class);

    /**
     * BCrypt 密文固定 60 字符（{@code $2a$10$} + 53 位）。
     * 判据取「长度 &lt; 20」而不是「不等于 60」：明文口令即使被刻意设得很长，
     * 也不会以 {@code $2a$} 开头，因此再加一层前缀判断双重保险。
     */
    private static final int PLAINTEXT_MAX_LENGTH = 20;
    private static final String BCRYPT_PREFIX = "$2";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevMigrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 把库中仍为明文的密码改写为 BCrypt。
     *
     * <p>SQL 无法直接算 BCrypt（每次加盐结果都不同，无法写成确定性的 UPDATE），
     * 只能逐条取出、逐条编码、逐条回写。</p>
     *
     * <p><b>幂等</b>：已是 BCrypt 的记录会被跳过，重复调用不会二次编码
     * （二次编码会让所有用户密码同时失效）。</p>
     *
     * <p><b>整批一个事务</b>：中途失败则全部回滚，避免出现「一半用户被改写、
     * 一半还是明文」的中间态。</p>
     *
     * <p>当前库中密码已全部为 BCrypt，本接口会返回 {@code scanned>0, migrated=0}。</p>
     *
     * @return 扫描数、迁移数与逐条明细；<b>明细含用户 id 与原密码</b>，
     *         故只在 dev 环境可达，严禁在生产暴露
     */
    @PostMapping("/migrate-passwords")
    @Transactional
    public ResponseEntity<ApiResponse<?>> migratePasswords() {
        // 只取疑似明文的行，而不是全表扫一遍再逐条判断：
        // 判据下推到 SQL，迁移脚本本身也不会把全量用户读进内存
        List<User> suspicious = userRepository.findPlaintextPasswordUsers(
                PLAINTEXT_MAX_LENGTH, BCRYPT_PREFIX);

        List<Map<String, Object>> details = new ArrayList<>();
        for (User user : suspicious) {
            String stored = user.getPassword();
            if (stored != null && !stored.isBlank()
                    && stored.length() >= PLAINTEXT_MAX_LENGTH && stored.startsWith(BCRYPT_PREFIX)) {
                // SQL 已筛过，这里再挡一次：并发下另一进程可能刚把它改成了 BCrypt，
                // 二次编码会让该用户立刻无法登录
                continue;
            }
            if (stored == null || stored.isBlank()) {
                // 空口令不是明文，编码成 BCrypt 等于凭空造出一个可登录的口令
                details.add(Map.of("userId", user.getId(), "username", user.getUsername(),
                        "skipped", "空口令，未改动"));
                log.warn("用户 {} 的口令为空，跳过", user.getUsername());
                continue;
            }

            user.setPassword(passwordEncoder.encode(stored));
            userRepository.updateById(user);
            details.add(Map.of("userId", user.getId(), "username", user.getUsername()));
            // 绝不打印原文：迁移日志进了版本库就等于二次泄露
            log.warn("已将用户 {} 的明文密码改写为 BCrypt", user.getUsername());
        }

        int migrated = (int) details.stream().filter(d -> !d.containsKey("skipped")).count();
        int skipped = details.size() - migrated;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidates", suspicious.size());
        result.put("migrated", migrated);
        result.put("skipped", skipped);
        result.put("details", details);
        log.info("密码迁移完成：候选 {}，迁移 {}，跳过 {}", suspicious.size(), migrated, skipped);

        return ResponseEntity.ok(ApiResponse.success(
                migrated == 0 ? "无需迁移：所有密码已是 BCrypt" : "迁移完成", result));
    }
}