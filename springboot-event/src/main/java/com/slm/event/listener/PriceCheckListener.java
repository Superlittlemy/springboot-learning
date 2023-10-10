package com.slm.event.listener;

import com.slm.event.event.PriceCheckEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PriceCheckListener implements ApplicationListener<PriceCheckEvent> {

    @Override
    public void onApplicationEvent(PriceCheckEvent priceCheckEvent) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("价格检验通过");
    }

}
