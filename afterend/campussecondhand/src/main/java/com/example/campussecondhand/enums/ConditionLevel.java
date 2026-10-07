package com.example.campussecondhand.enums;

import java.util.Arrays;

/**
 * 商品成色等级。
 *
 * <p>替代原先 {@code products.condition} 的 VARCHAR 自由中文文本。
 * 自由文本有两个致命问题：一是无法排序筛选（前端筛选用的是
 * {@code new/like-new/good/fair}，与库里的中文值根本不匹配，筛选一直失效）；
 * 二是同义不同写无法比较——"良好" 与 "轻微使用痕迹" 谁更新说不清。
 * 规范化为 0~4 的整数后，排序、筛选、统计才有意义。</p>
 *
 * <p>数字越大＝越旧，便于直接 {@code ORDER BY condition_level ASC} 得到"从新到旧"。</p>
 */
public enum ConditionLevel {

    BRAND_NEW(0, "全新", "未使用或仅试机"),
    LIKE_NEW(1, "99新", "几乎全新，无明显使用痕迹"),
    NEAR_NEW(2, "95新", "轻微使用痕迹，功能完好"),
    GOOD(3, "9成新", "有正常使用痕迹"),
    FAIR(4, "8成新及以下", "明显磨损或存在瑕疵");

    private final int code;
    private final String label;
    private final String description;

    ConditionLevel(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** 等级对应的视觉标识，供前端卡片直接取用，避免各处硬编码 emoji */
    public String getBadge() {
        return switch (this) {
            case BRAND_NEW -> "全新";
            case LIKE_NEW -> "99新";
            case NEAR_NEW -> "95新";
            case GOOD -> "9成新";
            case FAIR -> "8成新以下";
        };
    }

    /** 未知或越界码返回 null，由调用方决定如何兜底（不静默塞一个"全新"） */
    public static ConditionLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(level -> level.code == code)
                .findFirst()
                .orElse(null);
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }
}