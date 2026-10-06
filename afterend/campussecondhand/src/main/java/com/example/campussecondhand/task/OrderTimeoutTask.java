package com.example.campussecondhand.task;

import com.example.campussecondhand.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时订单定时清理任务。
 *
 * <p>订单下单时商品即被锁定为「已售出」，若用户既不支付也不关闭页面，
 * 商品会一直被占住。本任务周期性取消超时订单并把商品恢复为在售。</p>
 *
 * <p>注意：{@code @Scheduled} 需要类上存在 {@code @EnableScheduling} 才会生效，
 * 否则方法被静默忽略——不报错、不执行。启用位置见
 * {@code CampussecondhandApplication}。</p>
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private final OrderService orderService;

    public OrderTimeoutTask(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 周期性取消超时未支付订单。
     *
     * <p>用 {@code fixedDelay}（上一次执行结束到本次开始）而非 {@code fixedRate}：
     * 前者保证两次执行不重叠，即使某次扫描耗时较久也不会并发进入。</p>
     *
     * <p>扫描间隔由配置项 {@code order.timeout-scan-interval-ms} 控制，默认 5 分钟。
     * 需要更快释放商品时缩短该值即可，无需改代码。</p>
     */
    @Scheduled(
            fixedDelayString = "${order.timeout-scan-interval-ms:300000}",
            initialDelayString = "${order.timeout-scan-initial-delay-ms:60000}")
    public void cancelTimeoutOrders() {
        try {
            int cancelled = orderService.cancelTimeoutOrdersBatch();
            if (cancelled > 0) {
                log.info("超时订单清理完成，共取消 {} 笔", cancelled);
            }
        } catch (Exception e) {
            // 定时任务抛异常会导致后续调度被取消，必须兜住
            log.error("超时订单清理任务执行异常", e);
        }
    }
}
