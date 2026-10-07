package com.example.campussecondhand.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 全局统一分页返回结构：{@code {list, total, page, size}}。
 *
 * <p>此前后台列表用 {@code {items, total, page, pageSize}}，业务列表则直接返回裸数组，
 * 前端每处都要写不同的取值逻辑。现在<b>所有</b>分页接口统一为这一种形状，
 * 前端只需一套解析：{@code res.data.list} / {@code res.data.total}。</p>
 *
 * <p>{@code total} 是<b>总记录数</b>（不是当页条数），前端据此计算总页数；
 * 注意 MyBatis-Plus 的 {@code IPage#getTotal()} 是 {@code long}，
 * 若某处内存分页拿不到它，必须显式传入而不是丢成 0。</p>
 *
 * <p>用 record 而非普通 POJO：字段固定、无可变形参、Jackson 直接按声明顺序输出
 * {@code list/total/page/size}，顺序与接口文档一致，便于对照验收。</p>
 *
 * @param <T> 列表元素类型
 */
public record PageResult<T>(List<T> list, long total, int page, int size) {

    /** 由 MyBatis-Plus 分页对象构造 */
    public static <T> PageResult<T> of(IPage<T> paged) {
        return new PageResult<>(paged.getRecords(), paged.getTotal(),
                (int) paged.getCurrent(), (int) paged.getSize());
    }

    /** 手工分页场景：列表与总数分别已知 */
    public static <T> PageResult<T> of(List<T> list, long total, PageParam paging) {
        return new PageResult<>(list, total, paging.page(), paging.size());
    }
}