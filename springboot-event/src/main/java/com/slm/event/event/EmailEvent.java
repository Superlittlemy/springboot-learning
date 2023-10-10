package com.slm.event.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 邮件事件
 */
@Data
@AllArgsConstructor
public class EmailEvent {

    private String content;

}
