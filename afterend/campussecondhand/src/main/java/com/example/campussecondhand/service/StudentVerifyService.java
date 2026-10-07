package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 学生认证服务。
 *
 * <p><b>当前为模拟校验，接口预留，未来可对接教务系统或改为人工审核。</b>
 *
 * <p>现在的判定只看「学号格式像不像 + 姓名非空」，任何人构造一个
 * {@code 20xx} 开头的 10~12 位数字就能通过。之所以先这样做，是因为毕设需要
 * 展示"认证 → 免手续费 → 徽章"这条完整业务链路，而对接真实教务系统需要
 * 校内网络与接口授权，不具备可行性。</p>
 *
 * <p>因此这里把规则收敛到一个类里：将来接入教务系统只需替换
 * {@link #verify} 的校验部分，接口签名、事务边界与前端契约都不用动。</p>
 */
@Service
public class StudentVerifyService {

    private static final Logger log = LoggerFactory.getLogger(StudentVerifyService.class);

    /**
     * 学号格式：4 位入学年份（20xx）+ 6~8 位院系专业编号。
     *
     * <p>限制 20 开头是为了排除明显不可能的输入（如手机号、身份证片段）。</p>
     */
    private static final Pattern STUDENT_NO = Pattern.compile("^20\\d{2}\\d{6,8}$");

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_STUDENT_NO_LENGTH = 20;

    private final UserRepository userRepository;

    public StudentVerifyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 提交认证申请。通过则直接置为已认证。
     *
     * @param userId 当前登录用户
     * @param studentNo 学号
     * @param realName 真实姓名
     * @return 认证后的用户
     * @throws BadRequestException 参数不合法或学号已被占用
     */
    @Transactional
    public User verify(Long userId, String studentNo, String realName) {
        String no = studentNo == null ? "" : studentNo.trim();
        String name = realName == null ? "" : realName.trim();

        if (no.isEmpty()) {
            throw new BadRequestException("请填写学号");
        }
        if (no.length() > MAX_STUDENT_NO_LENGTH) {
            throw new BadRequestException("学号长度不合法");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("请填写真实姓名");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("真实姓名过长");
        }
        // 当前为模拟校验：只校验格式。未来对接教务系统时，这里替换为
        // 调用教务接口核验「学号 + 姓名」是否匹配，或改为提交后由管理员人工审核。
        if (!STUDENT_NO.matcher(no).matches()) {
            throw new BadRequestException("学号格式不正确，应为 20 开头的 10~12 位数字");
        }

        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        if (user.isStudentVerifiedUser()) {
            throw new BadRequestException("你已完成学生认证");
        }
        // 同一学号不能认证到两个账号，否则可冒用他人身份获取免手续费
        User occupied = userRepository.selectByStudentNo(no);
        if (occupied != null && !occupied.getId().equals(userId)) {
            throw new BadRequestException("该学号已被其他账号认证");
        }

        user.setStudentNo(no);
        user.setRealName(name);
        user.setIsStudentVerified(1);
        try {
            userRepository.updateById(user);
        } catch (DuplicateKeyException e) {
            // 并发下两人同时提交同一学号：唯一索引是最后一道防线
            throw new BadRequestException("该学号已被其他账号认证");
        }
        log.info("用户 {} 完成学生认证（模拟校验）", user.getUsername());
        return user;
    }
}