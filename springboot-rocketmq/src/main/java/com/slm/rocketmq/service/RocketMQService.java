package com.slm.rocketmq.service;

import com.slm.rocketmq.config.TxRocketMQTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class RocketMQService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TxRocketMQTemplate txRocketMQTemplate;

    /**
     * 同步发送普通消息
     * @param msg 消息内容
     */
    public void syncSendNormalMessage(String msg) {
        SendResult sendResult = rocketMQTemplate.syncSend("normal-message", msg);
        if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
            log.info("sync send normal message success: {}", sendResult);
        } else {
            log.error("sync send normal message failed: {}", sendResult);
        }
    }

    /**
     * 异步发送普通消息
     * @param msg 消息内容
     */
    public void asyncSendNormalMessage(String msg) {
        rocketMQTemplate.asyncSend("normal-message", msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("async send normal message success: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("async send normal message failed: {}", e.getMessage());
            }
        });
    }

    /**
     * 发送延迟消息
     * @param msg 消息内容
     */
    public void sendDelayMessage(String msg, int delaySeconds) {
        Message<String> message = MessageBuilder.withPayload(msg).build();
        SendResult sendResult = rocketMQTemplate.syncSendDelayTimeSeconds("delay-message", message, delaySeconds);
        if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
            log.info("send delay message success: {}", sendResult);
        } else {
            log.error("send delay message failed: {}", sendResult);
        }
    }

    /**
     * 发送顺序消息
     * @param orderMessages 有序消息
     * @param queue 队列
     */
    public void sendOrderlyMessage(String[] orderMessages, String queue) {
        Arrays.stream(orderMessages).forEach(msg -> {
            SendResult sendResult = rocketMQTemplate.syncSendOrderly("orderly-message", msg, queue);
            if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
                log.info("send orderly message success: {}", sendResult);
            } else {
                log.error("send orderly message failed: {}", sendResult);
            }
        });
    }

    /**
     * 发送事务消息
     * @param msg 消息内容
     */
    public void sendTransactionMessage(String msg) {
        Message<String> message = MessageBuilder.withPayload(msg).build();
        TransactionSendResult transactionSendResult = txRocketMQTemplate.sendMessageInTransaction("transaction-message", message, null);
        if (SendStatus.SEND_OK == transactionSendResult.getSendStatus()) {
            log.info("send transaction message success: {}", transactionSendResult);
        } else {
            log.error("send transaction message failed: {}", transactionSendResult);
        }
    }

    /**
     * 同步发送普通消息
     * @param msg 消息内容
     */
    public void sendBroadcastingMessage(String msg) {
        SendResult sendResult = rocketMQTemplate.syncSend("broadcasting-message", msg);
        if (SendStatus.SEND_OK == sendResult.getSendStatus()) {
            log.info("send broadcasting message success: {}", sendResult);
        } else {
            log.error("send broadcasting message failed: {}", sendResult);
        }
    }

    /**
     * 发送带TAG消息
     * @param msg 消息内容
     * @param tag TAG名称
     */
    public void sendTagFilterMessage(String msg, String tag) {
        rocketMQTemplate.convertAndSend("tag-message:" + tag, msg);
    }

    /**
     * 发送SQL92消息
     * @param msg 消息内容
     */
    public void sendSQL92FilterMessage(String msg, Map<String, Object> headers) {
        MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(msg);
        headers.forEach(messageBuilder::setHeader);
        Message<String> message = messageBuilder.build();
        rocketMQTemplate.syncSend("sql92-message", message);
    }

}
