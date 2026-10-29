package com.slm.rocketmq.config;

import org.apache.rocketmq.spring.annotation.ExtRocketMQTemplateConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

@ExtRocketMQTemplateConfiguration(group = "tx-producer-group")
public class TxRocketMQTemplate extends RocketMQTemplate {
}
