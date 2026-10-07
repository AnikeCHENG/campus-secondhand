package com.example.campussecondhand.enums;

/**
 * 举报处理动作。
 *
 * <p>动作与 {@link ReportTargetType} 严格绑定：商品只能 DELETE_PRODUCT，
 * 用户只能 BAN_USER，REJECT 两者皆可。这条约束在服务端强制校验——
 * 前端虽然按类型过滤了下拉选项，但那是 UX 层，直接调接口仍可绕过。</p>
 */
public enum ReportAction {

    /** 封禁被举报用户，仅适用于 USER */
    BAN_USER,
    /** 下架被举报商品，仅适用于 PRODUCT */
    DELETE_PRODUCT,
    /** 驳回举报，不执行任何处罚 */
    REJECT;

    /**
     * 判断动作是否适用于给定的举报对象类型。
     *
     * <p>返回 false 时调用方应返回 400，而不是静默忽略——
     * 「用下架商品的接口去封用户」是明确的越权信号，必须让调用方看见。</p>
     */
    public boolean appliesTo(ReportTargetType targetType) {
        if (targetType == null) {
            return false;
        }
        return switch (this) {
            case BAN_USER -> targetType == ReportTargetType.USER;
            case DELETE_PRODUCT -> targetType == ReportTargetType.PRODUCT;
            case REJECT -> true;
        };
    }

    public static ReportAction from(String value) {
        if (value == null) {
            return null;
        }
        for (ReportAction action : values()) {
            if (action.name().equals(value)) {
                return action;
            }
        }
        return null;
    }

    /** 处罚生效后举报状态置为 1，驳回置为 2 */
    public int toHandledStatus() {
        return this == REJECT ? ReportStatus.REJECTED.getCode() : ReportStatus.HANDLED.getCode();
    }

    public String getLabel() {
        return switch (this) {
            case BAN_USER -> "封禁用户";
            case DELETE_PRODUCT -> "下架商品";
            case REJECT -> "驳回举报";
        };
    }
}