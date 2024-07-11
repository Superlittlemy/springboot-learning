package com.slm.redis.pubsub.listener;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PublishService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void sendMsg(String channel, String msg){
        stringRedisTemplate.convertAndSend(channel, msg);
    }

}
