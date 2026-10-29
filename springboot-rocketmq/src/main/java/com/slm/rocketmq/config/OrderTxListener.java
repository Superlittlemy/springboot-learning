package com.slm.rocketmq.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "txRocketMQTemplate")
public class OrderTxListener implements RocketMQLocalTransactionListener {

    /**
     * 在半消息发送成功后被回调
     *
     * @param msg 事务消息
     * @param arg 本地事务执行器的临时入参，仅在首次 executeLocalTransaction 调用时存在
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        log.info("executeLocalTransaction msg: {}, arg: {}", msg, arg);
        String orderId = (String) arg;
        try {
            // 执行本地数据库事务，例如创建订单
            // orderService.createOrder(orderId);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 只在二次确认丢失（网络问题、生产者重启等）时被 Broker 触发，用于“兜底”查询真实结果。
     *
     * @param msg 事务消息
     * @return 事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        log.info("checkLocalTransaction msg: {}", msg);
        String orderId = (String) msg.getHeaders().get("orderId");
        // 查询数据库确认本地事务是否成功
        // boolean exists = orderService.isOrderExist(orderId);
        // return exists ? COMMIT : ROLLBACK;
        return RocketMQLocalTransactionState.COMMIT;
    }
}
