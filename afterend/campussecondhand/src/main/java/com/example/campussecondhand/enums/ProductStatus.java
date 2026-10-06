package com.example.campussecondhand.enums;

/**
 * 商品状态枚举（全项目唯一语义来源）。
 *
 * <p>数据库 {@code products.status} 为 INT，取值必须与本枚举 code 严格一致。</p>
 *
 * <p>迁移历史：旧语义为 0=在售 / 1=已售出 / 2=下架，
 * 已通过 {@code resources/migration-status-semantics.sql} 一次性迁移为当前语义。</p>
 */
public enum ProductStatus {

    /** 0：已下架，不在售，不可下单 */
    OFF_SHELF(0, "已下架"),

    /** 1：在售，可下单 */
    ON_SALE(1, "在售"),

    /** 2：已售出，成交后由 ProductService#markAsSold 写入 */
    SOLD(2, "已售出");

    private final int code;
    private final String label;

    ProductStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 按数据库值解析枚举；未知值返回 {@code null} 由调用方决定兜底策略 */
    public static ProductStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /** 判断给定数据库值是否为在售 */
    public static boolean isOnSale(Integer code) {
        return fromCode(code) == ON_SALE;
    }

    /** 判断给定数据库值是否为已售出 */
    public static boolean isSold(Integer code) {
        return fromCode(code) == SOLD;
    }

    /** 判断给定数据库值是否为已下架 */
    public static boolean isOffShelf(Integer code) {
        return fromCode(code) == OFF_SHELF;
    }
}
