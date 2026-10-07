package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.Report;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.enums.ReportAction;
import com.example.campussecondhand.enums.ReportStatus;
import com.example.campussecondhand.enums.ReportTargetType;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.ReportRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 举报处理服务。
 *
 * <p>三条核心约束：</p>
 * <ol>
 *   <li>同一用户对同一目标 24 小时内只能举报一次（防刷）</li>
 *   <li>处理动作与被举报对象类型严格匹配（商品只能下架、用户只能封号）</li>
 *   <li>处罚动作与举报状态更新必须在同一事务内</li>
 * </ol>
 *
 * <p>业务前置校验（如"不能封管理员""已售出商品不下架"）刻意放在本类而非
 * {@link PenaltyService}：后者必须保持行为中立，若把管理员守卫塞进去，
 * 现有管理端点 {@code PUT /admin/users/{id}/status} 的行为会被改变。</p>
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    /** 同一目标重复举报的冷却窗口 */
    private static final int DUPLICATE_WINDOW_HOURS = 24;
    private static final int MAX_REASON_LENGTH = 100;
    private static final int MAX_REMARK_LENGTH = 500;

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PenaltyService penaltyService;

    @Autowired
    public ReportService(ReportRepository reportRepository,
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         PenaltyService penaltyService) {
        this.reportRepository = reportRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.penaltyService = penaltyService;
    }

    // ==================== C 端提交 ====================

    /**
     * 用户提交举报。
     *
     * @param reporterId 举报人（当前登录用户）
     * @param targetType PRODUCT / USER
     * @param targetId   被举报对象ID
     * @param reason     举报原因
     */
    @Transactional
    public Report submit(Long reporterId, String targetType, Long targetId, String reason) {
        // 校验顺序刻意从「纯格式」到「查库」：非法输入不该产生任何数据库查询
        ReportTargetType type = ReportTargetType.from(targetType);
        if (type == null) {
            throw new BadRequestException("举报类型不合法");
        }
        if (targetId == null) {
            throw new BadRequestException("请选择举报对象");
        }
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) {
            throw new BadRequestException("请填写举报原因");
        }
        if (cleanReason.length() > MAX_REASON_LENGTH) {
            throw new BadRequestException("举报原因不能超过 " + MAX_REASON_LENGTH + " 个字符");
        }

        if (type == ReportTargetType.PRODUCT) {
            Product product = productRepository.selectById(targetId);
            if (product == null) {
                throw new BadRequestException("举报的商品不存在");
            }
            // 不能举报自己的商品，否则可借举报功能给自己刷记录
            if (Objects.equals(product.getUserId(), reporterId)) {
                throw new BadRequestException("不能举报自己的商品");
            }
        } else {
            User target = userRepository.selectById(targetId);
            if (target == null) {
                throw new BadRequestException("举报的用户不存在");
            }
            if (Objects.equals(targetId, reporterId)) {
                throw new BadRequestException("不能举报自己");
            }
        }

        LocalDateTime since = LocalDateTime.now().minusHours(DUPLICATE_WINDOW_HOURS);
        if (reportRepository.countRecentReports(reporterId, type.name(), targetId, since) > 0) {
            throw new BadRequestException(DUPLICATE_WINDOW_HOURS + "小时内已举报过，请勿重复提交");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetId(targetId);
        report.setTargetType(type.name());
        report.setReason(cleanReason);
        report.setStatus(ReportStatus.PENDING.getCode());
        reportRepository.insert(report);
        log.info("用户[{}] 举报了{}，ID:[{}]，原因:{}", reporterId, type.name(), targetId, cleanReason);
        return report;
    }

    // ==================== 管理端查询 ====================

    /**
     * 管理端分页查询举报列表。
     *
     * @param paging     分页参数
     * @param status     按处理状态筛选，null 表示全部
     * @param targetType 按对象类型筛选，null 表示全部
     * @param search     关键词，命中举报原因
     */
    public PageResult<Map<String, Object>> pageReports(PageParam paging, Integer status,
                                                       String targetType, String search) {
        QueryWrapper<Report> wrapper = new QueryWrapper<>();
        if (status != null) {
            if (!ReportStatus.isValid(status)) {
                throw new BadRequestException("非法的举报状态值");
            }
            wrapper.eq("status", status);
        }
        if (targetType != null && !targetType.isBlank()) {
            if (!ReportTargetType.isValid(targetType)) {
                throw new BadRequestException("非法的举报类型");
            }
            wrapper.eq("target_type", targetType);
        }
        if (search != null && !search.isBlank()) {
            wrapper.like("reason", search.trim());
        }
        // status 升序会把「待处理」排在最前，与管理员「先处理未处理」的直觉一致
        wrapper.orderByAsc("status").orderByDesc("create_time", "id");

        IPage<Report> paged = reportRepository.selectPage(paging.toPage(), wrapper);
        // 列表项是拼装出来的 Map 而非实体，直接返回 PageResult（它就是 list/total/page/size）
        return PageResult.of(toViewList(paged.getRecords()), paged.getTotal(), paging);
    }

    /**
     * 补充举报人昵称与被举报对象标题/昵称。
     *
     * <p>全部用 {@code selectBatchIds} 批量取而不是逐条 {@code selectById}：
     * 单页最多 100 条，逐条查会产生上百次往返。</p>
     */
    private List<Map<String, Object>> toViewList(List<Report> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = new ArrayList<>();
        reports.forEach(r -> {
            if (r.getReporterId() != null) {
                userIds.add(r.getReporterId());
            }
            if (ReportTargetType.USER.name().equals(r.getTargetType()) && r.getTargetId() != null) {
                userIds.add(r.getTargetId());
            }
        });
        Map<Long, User> users = loadUsers(userIds);

        List<Long> productIds = reports.stream()
                .filter(r -> ReportTargetType.PRODUCT.name().equals(r.getTargetType()))
                .map(Report::getTargetId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Product> products = productIds.isEmpty()
                ? Map.of()
                : productRepository.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Map<String, Object>> list = new ArrayList<>(reports.size());
        for (Report report : reports) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", report.getId());
            view.put("reporterId", report.getReporterId());
            User reporter = users.get(report.getReporterId());
            view.put("reporterName", reporter != null ? reporter.getUsername() : null);
            view.put("targetType", report.getTargetType());
            view.put("targetId", report.getTargetId());

            if (ReportTargetType.PRODUCT.name().equals(report.getTargetType())) {
                Product product = products.get(report.getTargetId());
                // 目标被删除时给出明确占位，前端据此显示「已删除」而非空白
                view.put("targetTitle", product != null ? product.getTitle() : "商品已删除");
                view.put("targetName", product != null ? product.getTitle() : "商品已删除");
            } else {
                User target = users.get(report.getTargetId());
                view.put("targetName", target != null ? target.getUsername() : "用户已删除");
                view.put("targetTitle", target != null ? target.getUsername() : "用户已删除");
            }

            view.put("reason", report.getReason());
            view.put("status", report.getStatus());
            view.put("adminRemark", report.getAdminRemark());
            view.put("createdTime", report.getCreatedTime());
            view.put("updateTime", report.getUpdateTime());
            list.add(view);
        }
        return list;
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userRepository.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    // ==================== 管理端处理 ====================

    /**
     * 处理举报：执行处罚并更新举报状态，全程单事务。
     *
     * <p>{@link PenaltyService} 上的 {@code @Transactional} 默认 REQUIRED，
     * 会加入本方法开启的事务。因此若最后更新举报状态失败，
     * 前面已执行的下架/封号会一并回滚，不会出现
     * 「商品已下架但举报仍是待处理」的中间态。</p>
     *
     * @param reportId     举报ID
     * @param action       处理动作
     * @param remark       处理备注，必填
     * @param adminName    操作管理员昵称，仅用于日志
     */
    @Transactional
    public Report handle(Long reportId, String action, String remark, String adminName) {
        Report report = reportRepository.selectById(reportId);
        if (report == null) {
            throw new BadRequestException("举报不存在");
        }
        // 状态守卫：只能处理待处理的举报，防止重复处罚
        // 用 intValue() 比较：status 在实体上是 Integer，历史脏数据可能为 null
        if (report.getStatus() == null || report.getStatus() != ReportStatus.PENDING.getCode()) {
            throw new BadRequestException("该举报已处理，请勿重复操作");
        }

        ReportAction act = ReportAction.from(action);
        if (act == null) {
            throw new BadRequestException("处理动作不合法");
        }
        String cleanRemark = remark == null ? "" : remark.trim();
        if (cleanRemark.isEmpty()) {
            throw new BadRequestException("请填写处理备注");
        }
        if (cleanRemark.length() > MAX_REMARK_LENGTH) {
            throw new BadRequestException("处理备注不能超过 " + MAX_REMARK_LENGTH + " 个字符");
        }

        ReportTargetType type = ReportTargetType.from(report.getTargetType());
        // action 与 targetType 的匹配校验放在服务端：前端按类型过滤了下拉选项，
        // 但那是 UX 层，直接调接口可以绕过
        if (!act.appliesTo(type)) {
            throw new BadRequestException("动作[" + act.getLabel() + "]不适用于举报类型["
                    + (type == null ? "未知" : type.name()) + "]");
        }

        switch (act) {
            case BAN_USER -> applyBanUser(report);
            case DELETE_PRODUCT -> applyDeleteProduct(report);
            case REJECT -> { /* 驳回不执行任何处罚 */ }
        }

        report.setStatus(act.toHandledStatus());
        // 把处罚动作写进备注形成快照：商品被删之后仍能回溯当时为何被处罚
        report.setAdminRemark(buildRemark(act, type, report, cleanRemark));
        reportRepository.updateById(report);

        log.warn("管理员[{}] 处理了举报[{}]，动作:[{}]，目标:{}[{}]，原因:{}，备注:{}",
                adminName, reportId, act.name(), type == null ? "未知" : type.name(),
                report.getTargetId(), report.getReason(), cleanRemark);
        return report;
    }

    private void applyBanUser(Report report) {
        User target = userRepository.selectById(report.getTargetId());
        if (target == null) {
            throw new BadRequestException("被举报的用户不存在，无法执行处罚");
        }
        // 管理员账号不参与封禁：否则误操作可让整个后台失去唯一的管理入口
        if (target.getRole() != null && target.getRole() == 1) {
            throw new BadRequestException("不能封禁管理员账号");
        }
        penaltyService.updateUserStatus(target.getId(), 0, "举报处理");
    }

    private void applyDeleteProduct(Report report) {
        Product product = productRepository.selectById(report.getTargetId());
        if (product == null) {
            throw new BadRequestException("被举报的商品不存在，无法执行处罚");
        }
        // 已售出说明存在进行中订单，此时下架会让买家付了钱却拿不到商品
        if (ProductStatus.SOLD.getCode() == product.getStatus()) {
            throw new BadRequestException("该商品已售出，存在进行中订单，无法下架");
        }
        penaltyService.updateProductStatus(product.getId(),
                ProductStatus.OFF_SHELF.getCode(), "举报处理");
    }

    private String buildRemark(ReportAction action, ReportTargetType type,
                               Report report, String cleanRemark) {
        String prefix = "[" + action.getLabel() + "] 目标:" + (type == null ? "未知" : type.name())
                + "#" + report.getTargetId() + " | 原因:" + report.getReason();
        String full = prefix + " | 备注:" + cleanRemark;
        // 备注列上限 500，这里兜底避免超长被数据库截断成半句话
        return full.length() > MAX_REMARK_LENGTH ? full.substring(0, MAX_REMARK_LENGTH) : full;
    }
}