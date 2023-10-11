package com.slm.kafka;

import com.slm.kafka.producer.MsgProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ProducerTest {

    @Autowired
    private MsgProducer msgProducer;

    @Test
    public void sendTest() {
        msgProducer.send("绝密文件");
        while (true) {}
    }

}
