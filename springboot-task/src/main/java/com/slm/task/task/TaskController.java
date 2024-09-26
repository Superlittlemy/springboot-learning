package com.slm.task.task;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    public final DynamicTask dynamicTask;

    @GetMapping("/modifyCron")
    public void modifyCron(@RequestParam String cron) {
        dynamicTask.setCron(cron);
    }

}
