package com.slm.tcc.task;

import com.slm.tcc.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ManagedResource(objectName = "com.slm.tcc:name=taskUtil")
public class TaskUtil {

    private final OrderService orderService;

    /**
     * 每小时检测创建15分粥后依然为待确认状态的订单
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    @ManagedOperation(description = "订单自动关闭任务")
    public void orderAutoclose() {
        orderService.autoclose();
    }

}
