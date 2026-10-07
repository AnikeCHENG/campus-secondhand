package com.example.campussecondhand.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 成色等级枚举测试。
 *
 * <p>重点是 {@code fromCode} 对未知值的处理：成色直接决定买家对价格的预期，
 * 把非法等级悄悄兜底成"全新"或"95新"比返回 null 更危险——那等于系统替卖家
 * 编造了一个他没声明的成色。因此必须返回 null，由调用方显式决定如何提示。</p>
 */
class ConditionLevelTest {

    @Test
    @DisplayName("0~4 五个等级齐全，数字越大越旧")
    void fiveLevelsOrderedFromNewToOld() {
        assertThat(ConditionLevel.values()).hasSize(5);
        assertThat(ConditionLevel.BRAND_NEW.getCode()).isZero();
        assertThat(ConditionLevel.FAIR.getCode()).isEqualTo(4);
        assertThat(ConditionLevel.LIKE_NEW.getCode()).isLessThan(ConditionLevel.GOOD.getCode());
        assertThat(ConditionLevel.GOOD.getCode()).isLessThan(ConditionLevel.FAIR.getCode());
    }

    @Test
    @DisplayName("每个等级都有可展示的标签与说明")
    void everyLevelHasLabelAndDescription() {
        for (ConditionLevel level : ConditionLevel.values()) {
            assertThat(level.getLabel()).isNotBlank();
            assertThat(level.getDescription()).isNotBlank();
            assertThat(level.getBadge()).isNotBlank();
        }
    }

    @Test
    @DisplayName("合法码能正确反查枚举")
    void fromCodeResolvesValidValues() {
        for (ConditionLevel level : ConditionLevel.values()) {
            assertThat(ConditionLevel.fromCode(level.getCode())).isEqualTo(level);
            assertThat(ConditionLevel.isValid(level.getCode())).isTrue();
        }
    }

    @Test
    @DisplayName("未知码、越界码、null 一律返回 null，不静默兜底")
    void fromCodeRejectsUnknownValues() {
        assertThat(ConditionLevel.fromCode(null)).isNull();
        assertThat(ConditionLevel.fromCode(-1)).isNull();
        assertThat(ConditionLevel.fromCode(5)).isNull();
        assertThat(ConditionLevel.fromCode(99)).isNull();
        assertThat(ConditionLevel.isValid(null)).isFalse();
        assertThat(ConditionLevel.isValid(5)).isFalse();
    }

    @Test
    @DisplayName("成色等级可直接用于升序排序（数值小=更新）")
    void levelOrderMatchesNewToOld() {
        assertThat(ConditionLevel.BRAND_NEW.getCode())
                .isLessThan(ConditionLevel.NEAR_NEW.getCode());
        assertThat(ConditionLevel.NEAR_NEW.getCode())
                .isLessThan(ConditionLevel.FAIR.getCode());
    }
}