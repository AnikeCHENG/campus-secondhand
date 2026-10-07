package com.example.campussecondhand.enums;

import java.util.List;

/**
 * 大厅动态类型枚举（全项目唯一语义来源）。
 *
 * <p>数据库 {@code posts.type} 为 VARCHAR(20)，取值必须与本枚举 name 严格一致。
 * 大小写敏感：库里存 {@code SELL}，不是 {@code sell}。</p>
 *
 * <p>注意：早期版本的 {@code Plaza.vue} 用的是小写 {@code sell/buy/warning/chat}，
 * 且 {@code buy}/{@code warning} 与本枚举的 {@link #SEEK}/{@link #WARN} 命名不同。
 * 那批值随前端 mock 动态一并下线，现以前端筛选值为准。</p>
 */
public enum PostType {

    /** 出售：可携带 productInfo 同事务创建商品，且 price 必填 */
    SELL("出售", true, true),

    /** 求购：不关联商品 */
    SEEK("求购", false, false),

    /** 免费送：可携带 productInfo 同事务创建商品，price 可省略（记 0） */
    FREE("免费送", true, false),

    /** 避雷：不关联商品 */
    WARN("避雷", false, false),

    /** 闲聊：不关联商品 */
    CHAT("闲聊", false, false);

    private final String label;

    /** 是否允许携带 productInfo 并联动创建商品 */
    private final boolean allowsProduct;

    /** 是否要求 productInfo.price 非空（SELL 必填，FREE 允许为 0） */
    private final boolean priceRequired;

    PostType(String label, boolean allowsProduct, boolean priceRequired) {
        this.label = label;
        this.allowsProduct = allowsProduct;
        this.priceRequired = priceRequired;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAllowsProduct() {
        return allowsProduct;
    }

    public boolean isPriceRequired() {
        return priceRequired;
    }

    /**
     * 按数据库值解析枚举；未知值返回 {@code null} 由调用方决定兜底策略。
     *
     * <p>大小写不敏感：兼容手动改库或历史小写数据，但返回的仍是枚举本身，
     * 落库时统一写 {@link #name()}。</p>
     */
    public static PostType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (PostType type : values()) {
            if (type.name().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        return null;
    }

    /** 全部类型，供接口返回枚举字典供前端渲染筛选项 */
    public static List<PostType> all() {
        return List.of(values());
    }
}