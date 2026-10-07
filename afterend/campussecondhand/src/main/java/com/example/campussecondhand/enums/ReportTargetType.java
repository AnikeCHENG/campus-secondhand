package com.example.campussecondhand.enums;

/**
 * 被举报对象类型。
 *
 * <p>用枚举而非裸字符串：管理端处理接口要求 action 与 targetType 严格匹配
 * （商品只能下架、用户只能封号），裸字符串会让这条约束散落在多个 if 里。</p>
 */
public enum ReportTargetType {

    /** 商品 */
    PRODUCT,
    /** 用户 */
    USER;

    public static boolean isValid(String value) {
        return value != null && (PRODUCT.name().equals(value) || USER.name().equals(value));
    }

    /** 宽松解析：非法值返回 null，由调用方决定如何兜底 */
    public static ReportTargetType from(String value) {
        if (value == null) {
            return null;
        }
        for (ReportTargetType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}