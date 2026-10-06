package com.example.campussecondhand.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 products.status 的语义，防止误改导致商品列表/下单判断错乱。
 */
class ProductStatusTest {

    @Test
    void code_mapping_is_stable() {
        assertThat(ProductStatus.OFF_SHELF.getCode()).isEqualTo(0);
        assertThat(ProductStatus.ON_SALE.getCode()).isEqualTo(1);
        assertThat(ProductStatus.SOLD.getCode()).isEqualTo(2);
    }

    @Test
    void labels_match_semantics() {
        assertThat(ProductStatus.OFF_SHELF.getLabel()).isEqualTo("已下架");
        assertThat(ProductStatus.ON_SALE.getLabel()).isEqualTo("在售");
        assertThat(ProductStatus.SOLD.getLabel()).isEqualTo("已售出");
    }

    @Test
    void fromCode_resolves_known_and_rejects_unknown() {
        assertThat(ProductStatus.fromCode(0)).isEqualTo(ProductStatus.OFF_SHELF);
        assertThat(ProductStatus.fromCode(1)).isEqualTo(ProductStatus.ON_SALE);
        assertThat(ProductStatus.fromCode(2)).isEqualTo(ProductStatus.SOLD);
        assertThat(ProductStatus.fromCode(3)).isNull();
        assertThat(ProductStatus.fromCode(null)).isNull();
    }

    @Test
    void predicates_are_mutually_exclusive() {
        assertThat(ProductStatus.isOnSale(1)).isTrue();
        assertThat(ProductStatus.isSold(1)).isFalse();
        assertThat(ProductStatus.isOffShelf(1)).isFalse();

        assertThat(ProductStatus.isSold(2)).isTrue();
        assertThat(ProductStatus.isOnSale(2)).isFalse();

        assertThat(ProductStatus.isOffShelf(0)).isTrue();
        assertThat(ProductStatus.isOnSale(0)).isFalse();
    }
}
