package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端处罚动作的统一入口。
 *
 * <p>此前这两段逻辑内联在 {@code AdminController} 的两个端点里。举报处理模块
 * 也需要「下架商品」「封禁用户」，若在举报服务里再写一份，同一条业务规则
 * 会有两处实现，日后修一处忘另一处必然出事。故统一收敛到本类。</p>
 *
 * <p><b>行为中立</b>：本类严格等价于重构前的内联实现——
 * 入参校验、分支行为、写入字段、副作用范围一概不变，调用方感知不到差别。
 * 刻意保留两处历史行为，改造时不要顺手「修正」：</p>
 * <ul>
 *   <li>{@link #updateUserStatus} <b>不做</b> status 白名单校验。
 *       现有接口 {@code PUT /api/admin/users/{id}/status} 接受任意 Integer，
 *       包括 7 这种既非正常也非封禁的未知值。此处保持一致，
 *       否则同一个接口重构前后行为不同，冒烟测试会失败。</li>
 *   <li>{@link #updateProductStatus} <b>不修改</b> {@code sold_time}。
 *       现有接口只改 status，商品从"已售出"被改回其他状态时不会清理成交时间。
 *       保持原样，避免超��本次重构范围。</li>
 * </ul>
 *
 * <p>需要额外业务约束的场景（如"不能封管理员""已售出商品不能下架"），
 * 由调用方先行校验后再调用本类，不要塞进这里——那会改变既有接口的行为。</p>
 */
@Service
public class PenaltyService {

    private static final Logger log = LoggerFactory.getLogger(PenaltyService.class);

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Autowired
    public PenaltyService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * 修改商品状态，等价于重构前的 {@code PUT /api/admin/products/{id}/status}。
     *
     * <p>白名单校验保留：状态值必须能映射到 {@link ProductStatus}，
     * 否则返回 400「非法的商品状态值」。</p>
     *
     * @throws BadRequestException 状态值不在 {@link ProductStatus} 取值范围内
     */
    @Transactional
    public Product updateProductStatus(Long productId, Integer status, String operator) {
        Product product = productRepository.selectById(productId);
        if (product == null) {
            throw new BadRequestException("商品不存在");
        }
        ProductStatus target = ProductStatus.fromCode(status);
        if (target == null) {
            throw new BadRequestException("非法的商品状态值");
        }

        product.setStatus(target.getCode());
        productRepository.updateById(product);
        log.warn("管理员[{}] 修改了商品[{}]状态为[{}]", operator, productId, target.getLabel());
        return product;
    }

    /**
     * 修改用户状态，等价于重构前的 {@code PUT /api/admin/users/{id}/status}。
     *
     * <p><b>不做白名单校验</b>，理由见类注释。调用方若需要限制合法取值，
     * 应在调用前自行校验。</p>
     *
     * <p>{@code status=0} 表示封禁：登录时 {@code AuthController} 会拦下并返回 403。</p>
     */
    @Transactional
    public User updateUserStatus(Long userId, Integer status, String operator) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }

        user.setStatus(status);
        userRepository.updateById(user);
        log.warn("管理员[{}] 修改了用户[{}]状态为[{}]", operator, userId, status);
        return user;
    }
}