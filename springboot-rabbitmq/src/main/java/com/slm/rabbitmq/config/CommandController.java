package com.slm.rabbitmq.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/command")
@RequiredArgsConstructor
public class CommandController {

    private final Publisher publisher;

    @RequestMapping("send")
    public void sendMsg(@RequestParam String msg) {
        publisher.sendMsg(msg);
    }

}
