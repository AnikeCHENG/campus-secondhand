package com.example.campussecondhand.common;

import com.example.campussecondhand.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分页参数校验测试。
 *
 * <p>规则：页码从 1 开始；每页条数在 1~{@link PageParam#MAX_SIZE} 之间；
 * 非法值抛 {@link BadRequestException}（最终渲染为 HTTP 400），<b>不做静默纠正</b>。</p>
 */
class PageParamTest {

    @Test
    @DisplayName("正常参数通过校验")
    void acceptsValidValues() {
        PageParam p = PageParam.of(1, 10);
        assertThat(p.page()).isEqualTo(1);
        assertThat(p.size()).isEqualTo(10);
        assertThat(p.offset()).isZero();
    }

    @Test
    @DisplayName("null 回落到默认页码 1 与默认条数 10")
    void fallsBackToDefaults() {
        PageParam p = PageParam.of(null, null);
        assertThat(p.page()).isEqualTo(1);
        assertThat(p.size()).isEqualTo(PageParam.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("offset 为 (page-1)*size")
    void computesOffset() {
        assertThat(PageParam.of(1, 10).offset()).isZero();
        assertThat(PageParam.of(2, 10).offset()).isEqualTo(10L);
        assertThat(PageParam.of(5, 20).offset()).isEqualTo(80L);
    }

    @Test
    @DisplayName("页码小于 1 抛 400，包括 0 与负数")
    void rejectsPageBelowOne() {
        assertThatThrownBy(() -> PageParam.of(0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("页码需从 1 开始");
        assertThatThrownBy(() -> PageParam.of(-1, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("页码需从 1 开始");
    }

    @Test
    @DisplayName("每页条数小于 1 抛 400")
    void rejectsSizeBelowOne() {
        assertThatThrownBy(() -> PageParam.of(1, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("每页条数需至少为 1");
        assertThatThrownBy(() -> PageParam.of(1, -5))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("每页条数需至少为 1");
    }

    @Test
    @DisplayName("每页条数超过上限抛 400，防止一次拉取拖垮数据库")
    void rejectsSizeAboveMax() {
        assertThatThrownBy(() -> PageParam.of(1, PageParam.MAX_SIZE + 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("每页条数不能超过 100");
        assertThatThrownBy(() -> PageParam.of(1, 999999))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("每页条数不能超过 100");
    }

    @Test
    @DisplayName("边界值 1 与 100 均通过")
    void acceptsBoundaryValues() {
        assertThatCode(() -> PageParam.of(1, 1)).doesNotThrowAnyException();
        assertThatCode(() -> PageParam.of(1, PageParam.MAX_SIZE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("页码越界优先于条数越界报错，便于定位第一个问题")
    void reportsPageErrorFirst() {
        assertThatThrownBy(() -> PageParam.of(0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("页码需从 1 开始");
    }

    @Test
    @DisplayName("可转换为 MyBatis-Plus 分页对象")
    void convertsToMybatisPage() {
        var page = PageParam.of(3, 25).toPage();
        assertThat(page.getCurrent()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(25);
    }
}