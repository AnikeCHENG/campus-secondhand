package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PenaltyService 测试。
 *
 * <p>本类是「行为中立重构」的产物，因此测试的重点不是新功能，
 * 而是<b>锁定与重构前完全一致的行为</b>——包括两处刻意保留的历史行为：
 * 用户状态无白名单校验、商品状态不修改 sold_time。
 * 这些看起来像缺陷，但改动它们会改变现有接口的重构前后行为，
 * 属于超��本次重构范围，必须由单独的改动来处理。</p>
 */
@ExtendWith(MockitoExtension.class)
class PenaltyServiceTest {

    private static final Long PRODUCT_ID = 10L;
    private static final Long USER_ID = 7L;
    private static final String OPERATOR = "admin";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private PenaltyService service;

    private Product product(int status) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setTitle("捷安特山地车");
        p.setPrice(new BigDecimal("380.00"));
        p.setStatus(status);
        return p;
    }

    private User user() {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername("buyer1");
        u.setStatus(1);
        return u;
    }

    // ==================== 商品状态 ====================

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    @DisplayName("合法的商品状态值照常写入")
    void acceptsValidProductStatus(int code) {
        Product p = product(1);
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(p);

        Product result = service.updateProductStatus(PRODUCT_ID, code, OPERATOR);

        assertThat(result.getStatus()).isEqualTo(code);
        verify(productRepository).updateById(p);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 99})
    @DisplayName("非法商品状态值被白名单拦下——与重构前一致")
    void rejectsInvalidProductStatus(int code) {
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(product(1));

        assertThatThrownBy(() -> service.updateProductStatus(PRODUCT_ID, code, OPERATOR))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("非法的商品状态值");

        verify(productRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("商品不存在时抛出「商品不存在」")
    void rejectsMissingProduct() {
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.updateProductStatus(PRODUCT_ID, 0, OPERATOR))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("商品不存在");
    }

    @Test
    @DisplayName("不修改 sold_time——保持重构前的副作用范围")
    void doesNotTouchSoldTime() {
        // 商品处于已售出且带成交时间的状态；改成下架时 soldTime 必须原样保留
        Product p = product(ProductStatus.SOLD.getCode());
        java.time.LocalDateTime soldAt = java.time.LocalDateTime.now().minusDays(3);
        p.setSoldTime(soldAt);
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(p);

        service.updateProductStatus(PRODUCT_ID, ProductStatus.OFF_SHELF.getCode(), OPERATOR);

        assertThat(p.getStatus()).isEqualTo(ProductStatus.OFF_SHELF.getCode());
        assertThat(p.getSoldTime()).isEqualTo(soldAt);
    }

    // ==================== 用户状态 ====================

    @Test
    @DisplayName("封禁：status=0 照常写入")
    void bansUser() {
        User u = user();
        when(userRepository.selectById(USER_ID)).thenReturn(u);

        User result = service.updateUserStatus(USER_ID, 0, OPERATOR);

        assertThat(result.getStatus()).isZero();
        verify(userRepository).updateById(u);
    }

    @Test
    @DisplayName("解封：status=1 照常写入")
    void unbansUser() {
        User u = user();
        u.setStatus(0);
        when(userRepository.selectById(USER_ID)).thenReturn(u);

        User result = service.updateUserStatus(USER_ID, 1, OPERATOR);

        assertThat(result.getStatus()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {7, -1, 2})
    @DisplayName("刻意不做白名单校验：未知状态值原样写入，与重构前行为完全一致")
    void keepsNoWhitelistOnUserStatus(int code) {
        User u = user();
        when(userRepository.selectById(USER_ID)).thenReturn(u);

        User result = service.updateUserStatus(USER_ID, code, OPERATOR);

        assertThat(result.getStatus()).isEqualTo(code);
        verify(userRepository).updateById(u);
    }

    @Test
    @DisplayName("用户不存在时抛出「用户不存在」")
    void rejectsMissingUser() {
        when(userRepository.selectById(USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.updateUserStatus(USER_ID, 0, OPERATOR))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("用户不存在");
    }

    @Test
    @DisplayName("不校验管理员身份——是否允许封管理员由调用方判断")
    void leavesAdminGuardToCaller() {
        User admin = user();
        admin.setRole(1);
        when(userRepository.selectById(USER_ID)).thenReturn(admin);

        // 刻意允许：PenaltyService 保持行为中立，「不能封管理员」由举报模块自行前置校验
        User result = service.updateUserStatus(USER_ID, 0, OPERATOR);

        assertThat(result.getStatus()).isZero();
    }
}