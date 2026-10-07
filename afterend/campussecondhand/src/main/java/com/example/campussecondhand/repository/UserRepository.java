package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * 取出疑似明文口令的用户，供一次性迁移脚本改写为 BCrypt。
     *
     * <p>为什么能这样筛：BCrypt 密文恒为 60 字符且以 {@code $2} 开头，
     * 明文口令再长也凑不出这个形态；两个条件取「或」是为了同时兜住
     * 「短口令」与「被刻意设成超长的口令」两种情况。</p>
     *
     * <p>用 {@code LENGTH(...)} 而非 {@code CHAR_LENGTH(...)}：两者对 ASCII
     * 口令结果一致，但 {@code LENGTH} 走字节长度，与 BCrypt 固定 60 字节的
     * 判据保持同一量纲。</p>
     */
    @Select("""
        SELECT * FROM users
        WHERE deleted = 0
          AND (password IS NULL
               OR LENGTH(password) < #{maxPlaintextLength}
               OR password NOT LIKE CONCAT(#{bcryptPrefix}, '$%'))
        """)
    List<User> findPlaintextPasswordUsers(@Param("maxPlaintextLength") int maxPlaintextLength,
                                          @Param("bcryptPrefix") String bcryptPrefix);
}


