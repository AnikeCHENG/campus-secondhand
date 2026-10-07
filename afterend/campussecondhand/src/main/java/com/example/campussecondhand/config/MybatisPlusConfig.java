package com.example.campussecondhand.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.example.campussecondhand.common.PageParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p>分页插件此前混在 {@code SecurityConfig} 中（与安全认证毫无关系），
 * 已迁至本类，使「分页能力来自哪里」只有一个答案。</p>
 *
 * <p><b>没有这个 Bean，所有分页都是假的。</b>{@code selectPage(Page, wrapper)} 依赖
 * {@link PaginationInnerInterceptor} 改写 SQL 才能拼出 {@code LIMIT ?, ?}；
 * 拦截器缺席时 {@code Page} 只是一个普通参数对象，MyBatis 照旧返回<b>全表</b>，
 * 而 {@code records} 与 {@code total} 看起来都"正常"，不会有任何报错——
 * 这正是最难发现的假分页。重构时若把它连同类一起删掉，必须同步把所有列表接口
 * 改回不分页，否则会静默退化成全表查询。</p>
 *
 * <p>{@code setMaxLimit} 是绕过 {@link PageParam} 直接 {@code new Page<>(1, 99999)}
 * 时的兜底上限，与 {@link PageParam#MAX_SIZE} 保持一致。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit((long) PageParam.MAX_SIZE);
        // 页码超过总页数时返回空列表而非回到第一页，保持"翻过头就是空"的直觉
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}