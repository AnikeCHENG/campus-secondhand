package com.example.campussecondhand.service.impl;

import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 本地模拟支付渠道：不对接任何真实支付网关，永远返回成功。
 *
 * <p>流水号格式 {@code MOCK + yyyyMMddHHmmssSSS + 12位随机}。
 * 时间戳部分保证可读性与时序性便于排查；随机部分必须有足够熵——
 * {@code payment_record.trade_no} 上有唯一索引，撞号会直接导致支付失败。
 * 仅 3 位随机（1000 种）在几十次调用内就会因生日悖论撞号，故这里取 12 位。</p>
 *
 * <p>{@code @ConditionalOnProperty} 是这个扩展点能成立的关键：
 * 没有它，将来接入真实渠道后容器里会同时存在两个 {@link PaymentService} Bean，
 * 注入将产生歧义。要切换渠道只需把配置项 {@code payment.channel} 改为其他值。</p>
 */
@Service
@ConditionalOnProperty(name = "payment.channel", havingValue = "mock", matchIfMissing = true)
public class MockPaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentServiceImpl.class);

    /** 毫秒级时间戳，保证同一秒内的多次支付在流水号上可区分 */
    private static final DateTimeFormatter TRADE_NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    public PayResult pay(Order order, String payMethod) {
        String tradeNo = generateTradeNo();
        log.info("模拟支付成功: orderNo={}, payMethod={}, tradeNo={}",
                order.getOrderNo(), payMethod, tradeNo);
        return PayResult.ok(tradeNo);
    }

    @Override
    public String channelName() {
        return "mock";
    }

    private String generateTradeNo() {
        // 后 12 位取自 64 位随机数的十进制表示，随机空间约 10^13
        long entropy = ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L);
        return "MOCK" + LocalDateTime.now().format(TRADE_NO_FORMAT) + entropy;
    }
}
