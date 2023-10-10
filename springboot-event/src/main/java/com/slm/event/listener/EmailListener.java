package com.slm.event.listener;

import com.slm.event.event.EmailEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailListener {

    @Async
    @EventListener(EmailEvent.class)
    public void listen(EmailEvent event) throws InterruptedException {
        Thread.sleep(3000);
        log.info("发送邮件，内容为：" + event.getContent());
    }

}
