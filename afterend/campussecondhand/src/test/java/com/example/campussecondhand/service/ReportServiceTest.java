package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.Report;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.enums.ReportStatus;
import com.example.campussecondhand.enums.ReportTargetType;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.ReportRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 举报服务测试。
 *
 * <p>重点锁死四条规则：24 小时防刷、不能举报自己、动作与对象类型严格匹配、
 * 处罚必须经由 {@link PenaltyService} 执行。三条业务前置校验
 * （不能封管理员、已售出商品不下架）也在此验证——它们刻意放在
 * ReportService 而非 PenaltyService，为的是不改变现有管理端点行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long REPORTER_ID = 6L;
    private static final Long TARGET_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PenaltyService penaltyService;
    @InjectMocks
    private ReportService service;

    private Product product(int status, Long ownerId) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setTitle("捷安特山地车");
        p.setPrice(new BigDecimal("380.00"));
        p.setStatus(status);
        p.setUserId(ownerId);
        return p;
    }

    private User user(Long id, int role) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setRole(role);
        u.setStatus(1);
        return u;
    }

    private Report report(String targetType, Long targetId, int status) {
        Report r = new Report();
        r.setId(100L);
        r.setReporterId(REPORTER_ID);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason("虚假宣传");
        r.setStatus(status);
        return r;
    }

    // ==================== C 端提交 ====================

    @Test
    @DisplayName("提交商品举报成功")
    void submitsProductReport() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));
        when(reportRepository.countRecentReports(anyLong(), anyString(), anyLong(), any()))
                .thenReturn(0L);

        Report saved = service.submit(REPORTER_ID, "PRODUCT", PRODUCT_ID, "虚假宣传");

        verify(reportRepository).insert(any(Report.class));
        assertThat(saved.getTargetType()).isEqualTo("PRODUCT");
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("24 小时内重复举报被拒绝")
    void rejectsDuplicateWithin24Hours() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));
        when(reportRepository.countRecentReports(anyLong(), anyString(), anyLong(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.submit(REPORTER_ID, "PRODUCT", PRODUCT_ID, "虚假宣传"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("24小时内已举报过");

        verify(reportRepository, never()).insert(any());
    }

    @Test
    @DisplayName("防刷查询带上 24 小时前的时间边界")
    void antiSpamQueryUsesWindow() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));
        when(reportRepository.countRecentReports(anyLong(), anyString(), anyLong(), any()))
                .thenReturn(0L);

        service.submit(REPORTER_ID, "PRODUCT", PRODUCT_ID, "虚假宣传");

        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reportRepository).countRecentReports(eq(REPORTER_ID), eq("PRODUCT"),
                eq(PRODUCT_ID), sinceCaptor.capture());
        LocalDateTime since = sinceCaptor.getValue();
        long hoursAgo = java.time.Duration.between(since, LocalDateTime.now()).toHours();
        assertThat(hoursAgo).isBetween(23L, 25L);
    }

    @Test
    @DisplayName("不能举报自己的商品")
    void rejectsReportingOwnProduct() {
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), REPORTER_ID));

        assertThatThrownBy(() -> service.submit(REPORTER_ID, "PRODUCT", PRODUCT_ID, "虚假宣传"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("不能举报自己的商品");

        verify(reportRepository, never()).countRecentReports(anyLong(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("不能举报自己")
    void rejectsReportingSelf() {
        when(userRepository.selectById(REPORTER_ID)).thenReturn(user(REPORTER_ID, 0));

        assertThatThrownBy(() -> service.submit(REPORTER_ID, "USER", REPORTER_ID, "辱骂"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("不能举报自己");
    }

    @Test
    @DisplayName("目标不存在时拒绝，且不查防刷")
    void rejectsMissingTarget() {
        when(productRepository.selectById(PRODUCT_ID)).thenReturn(null);
        assertThatThrownBy(() -> service.submit(REPORTER_ID, "PRODUCT", PRODUCT_ID, "虚假宣传"))
                .isInstanceOf(BadRequestException.class);

        when(userRepository.selectById(TARGET_ID)).thenReturn(null);
        assertThatThrownBy(() -> service.submit(REPORTER_ID, "USER", TARGET_ID, "辱骂"))
                .isInstanceOf(BadRequestException.class);

        verify(reportRepository, never()).countRecentReports(anyLong(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("非法类型与空原因被拒绝，且完全不查库")
    void rejectsInvalidInputBeforeQuery() {
        assertThatThrownBy(() -> service.submit(REPORTER_ID, "COMMENT", 1L, "虚假宣传"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("举报类型不合法");

        // 原因为空在校验阶段就被拦下，连商品表都不该查——故此处不设桩
        assertThatThrownBy(() -> service.submit(REPORTER_ID, "PRODUCT", PRODUCT_ID, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("请填写举报原因");

        verify(productRepository, never()).selectById(any());
    }

    // ==================== 管理端处理 ====================

    @Test
    @DisplayName("DELETE_PRODUCT 下架商品并置举报为已处理")
    void handlesDeleteProduct() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));

        service.handle(100L, "DELETE_PRODUCT", "已确认虚假宣传", "admin");

        verify(penaltyService).updateProductStatus(PRODUCT_ID,
                ProductStatus.OFF_SHELF.getCode(), "举报处理");
        verify(reportRepository).updateById(r);
        assertThat(r.getStatus()).isEqualTo(ReportStatus.HANDLED.getCode());
    }

    @Test
    @DisplayName("BAN_USER 封禁用户并置举报为已处理")
    void handlesBanUser() {
        Report r = report("USER", TARGET_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);
        when(userRepository.selectById(TARGET_ID)).thenReturn(user(TARGET_ID, 0));

        service.handle(100L, "BAN_USER", "多次辱骂他人", "admin");

        verify(penaltyService).updateUserStatus(TARGET_ID, 0, "举报处理");
        verify(reportRepository).updateById(r);
        assertThat(r.getStatus()).isEqualTo(ReportStatus.HANDLED.getCode());
    }

    @Test
    @DisplayName("REJECT 驳回举报，不执行任何处罚")
    void handlesReject() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);

        service.handle(100L, "REJECT", "商品无违规", "admin");

        verify(penaltyService, never()).updateProductStatus(anyLong(), anyInt(), anyString());
        verify(penaltyService, never()).updateUserStatus(anyLong(), any(), anyString());
        verify(reportRepository).updateById(r);
        assertThat(r.getStatus()).isEqualTo(ReportStatus.REJECTED.getCode());
    }

    @Test
    @DisplayName("动作与对象类型不匹配一律 400")
    void rejectsActionTargetTypeMismatch() {
        // 商品举报用封用户的动作
        Report r1 = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r1);
        assertThatThrownBy(() -> service.handle(100L, "BAN_USER", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不适用于举报类型");
        verify(penaltyService, never()).updateUserStatus(anyLong(), any(), anyString());

        // 用户举报用下架商品的���作
        Report r2 = report("USER", TARGET_ID, 0);
        when(reportRepository.selectById(101L)).thenReturn(r2);
        assertThatThrownBy(() -> service.handle(101L, "DELETE_PRODUCT", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不适用于举报类型");
        verify(penaltyService, never()).updateProductStatus(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("已处理的举报不能重复处理")
    void rejectsAlreadyHandled() {
        Report r = report("PRODUCT", PRODUCT_ID, 1);
        when(reportRepository.selectById(100L)).thenReturn(r);

        assertThatThrownBy(() -> service.handle(100L, "DELETE_PRODUCT", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已处理");

        verify(penaltyService, never()).updateProductStatus(anyLong(), anyInt(), anyString());
        verify(reportRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("处罚失败时不更新举报状态——事务回滚的前提")
    void doesNotMarkHandledWhenPenaltyFails() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.SOLD.getCode(), 99L));

        assertThatThrownBy(() -> service.handle(100L, "DELETE_PRODUCT", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已售出");

        // 举报仍是待处理；配合 @Transactional，已执行的下架也会一并回滚
        assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING.getCode());
        verify(reportRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("已售出商品拒绝下架——买家付了钱却拿不到商品")
    void rejectsOffShelfSoldProduct() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.SOLD.getCode(), 99L));

        assertThatThrownBy(() -> service.handle(100L, "DELETE_PRODUCT", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("已售出");

        verify(penaltyService, never()).updateProductStatus(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("不能封禁管理员账号")
    void rejectsBanningAdmin() {
        Report r = report("USER", TARGET_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);
        when(userRepository.selectById(TARGET_ID)).thenReturn(user(TARGET_ID, 1));

        assertThatThrownBy(() -> service.handle(100L, "BAN_USER", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("不能封禁管理员");

        verify(penaltyService, never()).updateUserStatus(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("处理备注必填")
    void requiresRemark() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);

        assertThatThrownBy(() -> service.handle(100L, "DELETE_PRODUCT", "   ", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("请填写处理备注");

        verify(penaltyService, never()).updateProductStatus(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("非法动作值被拒绝")
    void rejectsUnknownAction() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);

        assertThatThrownBy(() -> service.handle(100L, "DROP_DATABASE", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("处理动作不合法");
    }

    @Test
    @DisplayName("举报不存在时拒绝")
    void rejectsMissingReport() {
        when(reportRepository.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.handle(100L, "REJECT", "备注", "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("举报不存在");
    }

    @Test
    @DisplayName("备注中写入处罚快照：商品被删后仍可回溯")
    void writesPenaltySnapshotIntoRemark() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectById(100L)).thenReturn(r);
        when(productRepository.selectById(PRODUCT_ID))
                .thenReturn(product(ProductStatus.ON_SALE.getCode(), 99L));

        service.handle(100L, "DELETE_PRODUCT", "已确认虚假宣传", "admin");

        assertThat(r.getAdminRemark())
                .contains("[下架商品]")
                .contains("PRODUCT#" + PRODUCT_ID)
                .contains("虚假宣传")
                .contains("已确认虚假宣传");
    }

    // ==================== 管理端列表 ====================

    @Test
    @DisplayName("列表按 status 升序 + 时间倒序，待处理排在最前")
    void listOrdersPendingFirst() {
        IPage<Report> paged = new Page<>(1, 10, 1);
        when(reportRepository.selectPage(any(), any())).thenReturn(paged);

        service.pageReports(PageParam.of(1, 10), null, null, null);

        ArgumentCaptor<Wrapper<Report>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(reportRepository).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("ORDER BY status ASC");
        assertThat(sql).contains("create_time DESC");
    }

    @Test
    @DisplayName("status / targetType / search 三个筛选条件都会下推到 SQL")
    void pushesFiltersIntoSql() {
        when(reportRepository.selectPage(any(), any())).thenReturn(new Page<>(1, 10, 0));

        service.pageReports(PageParam.of(1, 10), 0, "PRODUCT", "虚假");

        ArgumentCaptor<Wrapper<Report>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(reportRepository).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("status =");
        assertThat(sql).contains("target_type =");
        assertThat(sql).contains("reason LIKE");
    }

    @Test
    @DisplayName("非法筛选值返回 400")
    void rejectsInvalidFilters() {
        assertThatThrownBy(() -> service.pageReports(PageParam.of(1, 10), 9, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("非法的举报状态值");
        assertThatThrownBy(() -> service.pageReports(PageParam.of(1, 10), null, "COMMENT", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("非法的举报类型");
    }

    @Test
    @DisplayName("列表项补齐举报人昵称与被举报对象标题")
    void enrichesListWithNames() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        r.setId(100L);
        when(reportRepository.selectPage(any(), any())).thenReturn(pageOf(r));
        when(userRepository.selectBatchIds(any())).thenReturn(List.of(user(REPORTER_ID, 0)));
        when(productRepository.selectBatchIds(any()))
                .thenReturn(List.of(product(ProductStatus.ON_SALE.getCode(), 99L)));

        PageResult<Map<String, Object>> result = service.pageReports(PageParam.of(1, 10), null, null, null);

        Map<String, Object> row = result.list().get(0);
        assertThat(row.get("reporterName")).isEqualTo("user6");
        assertThat(row.get("targetTitle")).isEqualTo("捷安特山地车");
        // 管理端 AdminReports.vue 按 createdTime 读取时间列
        assertThat(row).containsKey("createdTime");
    }

    @Test
    @DisplayName("目标已被删除时给出明确占位而非 null")
    void usesPlaceholderForDeletedTarget() {
        Report r = report("PRODUCT", PRODUCT_ID, 0);
        when(reportRepository.selectPage(any(), any())).thenReturn(pageOf(r));
        when(userRepository.selectBatchIds(any())).thenReturn(List.of());
        when(productRepository.selectBatchIds(any())).thenReturn(List.of());

        PageResult<Map<String, Object>> result = service.pageReports(PageParam.of(1, 10), null, null, null);

        assertThat(result.list().get(0).get("targetTitle")).isEqualTo("商品已删除");
    }

    @Test
    @DisplayName("分页参数越界在到达数据库前被拒绝")
    void rejectsInvalidPaging() {
        assertThatThrownBy(() -> service.pageReports(PageParam.of(0, 10), null, null, null))
                .isInstanceOf(BadRequestException.class);
        verify(reportRepository, never()).selectPage(any(), any());
    }

    private IPage<Report> pageOf(Report... reports) {
        Page<Report> p = new Page<>(1, 10, reports.length);
        p.setRecords(List.of(reports));
        return p;
    }
}