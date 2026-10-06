package com.example.campussecondhand.enums;

/**
 * 订单状态枚举（全项目唯一语义来源）。
 *
 * <p>数据库 {@code orders.status} 为 INT，取值必须与本枚举 code 严格一致。</p>
 *
 * <p>状态流转：</p>
 * <pre>
 *   0 待支付 ──支付──&gt; 1 待发货 ──发货──&gt; 2 待收货 ──确认收货──&gt; 3 已完成
 *      │
 *      └──取消/超时──&gt; 4 已取消
 * </pre>
 *
 * <p>注意：仅 {@link #PENDING_PAYMENT} 允许支付与取消，其余状态为终态或需卖家/买家推进。</p>
 */
public enum OrderStatus {

    /** 0：待支付，商品已锁定为已售出，30 分钟内未支付自动取消 */
    PENDING_PAYMENT(0, "待支付"),

    /** 1：待发货，已收款，等待卖家发货 */
    PENDING_SHIPMENT(1, "待发货"),

    /** 2：待收货，卖家已发货，等待买家确认 */
    PENDING_RECEIPT(2, "待收货"),

    /** 3：已完成，买家确认收货，资金结算给卖家 */
    COMPLETED(3, "已完成"),

    /** 4：已取消，商品已恢复为在售 */
    CANCELLED(4, "已取消");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
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
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /** 判断给定数据库值是否为待支付 */
    public static boolean isPendingPayment(Integer code) {
        return fromCode(code) == PENDING_PAYMENT;
    }

    /** 判断给定数据库值是否为已取消 */
    public static boolean isCancelled(Integer code) {
        return fromCode(code) == CANCELLED;
    }

    /** 判断给定数据库值是否为已完成 */
    public static boolean isCompleted(Integer code) {
        return fromCode(code) == COMPLETED;
    }
}
