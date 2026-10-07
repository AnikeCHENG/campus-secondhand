package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学生认证服务测试。
 *
 * <p>重点是学号格式校验与「学号不可被两个账号占用」——
 * 后者是安全约束：否则任何人都能用别人的学号认证，从而获取免手续费资格。</p>
 */
@ExtendWith(MockitoExtension.class)
class StudentVerifyServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private StudentVerifyService service;

    private User user() {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername("tester");
        u.setIsStudentVerified(0);
        return u;
    }

    @ParameterizedTest
    @ValueSource(strings = {"202012345678", "20211234567", "2022123456"})
    @DisplayName("合法学号通过校验")
    void acceptsValidStudentNo(String studentNo) {
        User u = user();
        when(userRepository.selectById(USER_ID)).thenReturn(u);
        when(userRepository.selectByStudentNo(studentNo)).thenReturn(null);

        assertThatCode(() -> service.verify(USER_ID, studentNo, "张三")).doesNotThrowAnyException();
        assertThat(u.getIsStudentVerified()).isEqualTo(1);
        assertThat(u.getStudentNo()).isEqualTo(studentNo);
        assertThat(u.getRealName()).isEqualTo("张三");
        verify(userRepository).updateById(u);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "199912345678",
            "3012345678",
            "2020",
            "20201234567890",
            "2020123456a",
            "2020 12345678"
    })
    @DisplayName("非法学号被拒绝，且完全不查库")
    void rejectsInvalidStudentNo(String studentNo) {
        assertThatThrownBy(() -> service.verify(USER_ID, studentNo, "张三"))
                .isInstanceOf(BadRequestException.class);

        // 格式校验在最前面，连用户表都不该查——非法输入不该走到 DB
        verify(userRepository, never()).selectById(any());
        verify(userRepository, never()).updateById(any());
    }

    @ParameterizedTest
    @CsvSource({",'张三'", "202012345678,''"})
    @DisplayName("学号或姓名为空时拒绝")
    void rejectsBlankFields(String studentNo, String realName) {
        assertThatThrownBy(() -> service.verify(USER_ID, studentNo, realName))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("同一学号已被他人占用时拒绝，防止冒用身份骗取免手续费")
    void rejectsOccupiedStudentNo() {
        User other = new User();
        other.setId(99L);
        when(userRepository.selectById(USER_ID)).thenReturn(user());
        when(userRepository.selectByStudentNo("202012345678")).thenReturn(other);

        assertThatThrownBy(() -> service.verify(USER_ID, "202012345678", "李四"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已被其他账号认证");

        verify(userRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("重复认证被拒绝")
    void rejectsAlreadyVerified() {
        User u = user();
        u.setIsStudentVerified(1);
        when(userRepository.selectById(USER_ID)).thenReturn(u);

        assertThatThrownBy(() -> service.verify(USER_ID, "202012345678", "张三"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已完成学生认证");
    }

    @Test
    @DisplayName("并发下唯一索引冲突被翻译成友好提示")
    void translatesDuplicateKey() {
        User u = user();
        when(userRepository.selectById(USER_ID)).thenReturn(u);
        when(userRepository.selectByStudentNo("202012345678")).thenReturn(null);
        when(userRepository.updateById(u)).thenThrow(new DuplicateKeyException("uk_student_no"));

        assertThatThrownBy(() -> service.verify(USER_ID, "202012345678", "张三"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已被其他账号认证");
    }

    @Test
    @DisplayName("输入的空格被去除后仍可通过")
    void trimsInput() {
        User u = user();
        when(userRepository.selectById(USER_ID)).thenReturn(u);
        when(userRepository.selectByStudentNo("202012345678")).thenReturn(null);

        service.verify(USER_ID, "  202012345678  ", "  张三  ");

        assertThat(u.getStudentNo()).isEqualTo("202012345678");
        assertThat(u.getRealName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("isStudentVerified 为 null 时视为未认证")
    void nullFlagMeansUnverified() {
        User u = user();
        u.setIsStudentVerified(null);
        assertThat(u.isStudentVerifiedUser()).isFalse();
        u.setIsStudentVerified(1);
        assertThat(u.isStudentVerifiedUser()).isTrue();
    }
}