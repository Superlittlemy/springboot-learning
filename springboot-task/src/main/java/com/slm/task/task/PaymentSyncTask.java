package com.slm.task.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentSyncTask {

    private int i = 0;

    // 支付结果定时拉取
    @Scheduled(fixedDelay = 5000)
    public void sync() {
        log.info("拉取银行支付状态信息……");
    }

    // 报表定时刷新
    @Scheduled(fixedRate = 3000)
    public void dashboardRefresh() throws InterruptedException {
        i++;
        if (i % 3 == 0) {
            Thread.sleep(10000);
        }
        log.info("固定每3S更新报表 " + i);
    }

    // 每日报表计算
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyReport() {
        log.info("每天凌晨统计日报");
    }

}
