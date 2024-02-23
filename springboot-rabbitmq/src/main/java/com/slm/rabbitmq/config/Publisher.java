package com.slm.rabbitmq.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Publisher {

    private final RabbitTemplate rabbitTemplate;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void sendMsg(String msg) {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String sendTime = sdf.format(new Date());
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId);
        map.put("sendTime", sendTime);
        map.put("msg", msg);
        rabbitTemplate.convertAndSend("demo-exchange", "routing", map);
    }

}
