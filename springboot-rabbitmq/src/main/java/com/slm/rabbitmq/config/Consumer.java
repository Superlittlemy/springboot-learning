package com.slm.rabbitmq.config;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Consumer {

    @RabbitHandler
    @RabbitListener(queues = { "demo-queue" })
    public void process(Map map) {
        System.out.println("get msg: " + map.toString());
    }

}
