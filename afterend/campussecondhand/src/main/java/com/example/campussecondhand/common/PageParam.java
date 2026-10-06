package com.example.campussecondhand.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campussecondhand.exception.BadRequestException;

/**
 * 分页参数统一校验。
 *
 * <p>此前各分页端点各自用 {@code defaultValue} 接收参数，越界时没有任何校验：
 * {@code page=0} 会让 {@code Stream.skip(-10)} 抛
 * {@code IllegalArgumentException: -10}，最终被兜底分支渲染成
 * <b>HTTP 500</b>——把客户端的参数错误伪装成服务端故障，
 * 调用方会去查后端日志，而真正的原因只是一个负数页码。</p>
 *
 * <p>本类把规则收敛到一处，所有分页端点统一调用 {@link #of(Integer, Integer)}，
 * 非法值一律抛 {@link BadRequestException} 并以 400 返回，
 * <b>不做静默纠正</b>：静默 clamp 会让调用方拿到与请求不一致的数据，
 * 且掩盖调用方的分页 bug（例如把 0 当成第一页的 1-based / 0-based 混淆）。</p>
 *
 * <p>页码从 <b>1</b> 开始（与 {@code MyBatis-Plus} 的 {@code Page#getCurrent()} 一致），
 * 每页条数上限 {@value #MAX_SIZE}，防止 {@code size=999999} 拖垮数据库。</p>
 */
public final class PageParam {

    /** 每页条数上限 */
    public static final int MAX_SIZE = 100;

    /** 每页默认条数 */
    public static final int DEFAULT_SIZE = 10;

    private final int page;
    private final int size;

    private PageParam(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /**
     * 校验并构造分页参数。
     *
     * @param page 页码，从 1 开始；{@code null} 视为 1
     * @param size 每页条数；{@code null} 视为 {@value #DEFAULT_SIZE}
     * @throws BadRequestException 页码小于 1，或每页条数不在 1~{@value #MAX_SIZE} 之间
     */
    public static PageParam of(Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;

        if (p < 1) {
            throw new BadRequestException("页码需从 1 开始");
        }
        if (s < 1) {
            throw new BadRequestException("每页条数需至少为 1");
        }
        if (s > MAX_SIZE) {
            throw new BadRequestException("每页条数不能超过 " + MAX_SIZE);
        }
        return new PageParam(p, s);
    }

    /** 页码，从 1 开始 */
    public int page() {
        return page;
    }

    /** 每页条数 */
    public int size() {
        return size;
    }

    /** 跳过的记录数，用于 {@code Stream#skip(long)} */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /** 转为 {@code MyBatis-Plus} 分页对象 */
    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }

    @Override
    public String toString() {
        return "PageParam{page=" + page + ", size=" + size + '}';
    }
}