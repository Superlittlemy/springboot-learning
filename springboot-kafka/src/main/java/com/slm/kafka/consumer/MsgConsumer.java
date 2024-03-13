package com.slm.kafka.consumer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Slf4j
@Component
public class MsgConsumer {

    // 普通消费者
    @SneakyThrows
    @KafkaHandler
    @KafkaListener(topics = { "ct" }, groupId = "test-consumer-group1")
    public void receive(ConsumerRecord<?, ?> record) {
        log.info("消费者1接收到的消息：" + record.value());
        Thread.sleep(1000);
    }

    @Bean("manualListenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> manualListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // 配置手动提交offset
        return factory;
    }

    // 手动提交 offset 消费者
    @SneakyThrows
    @KafkaHandler
    @KafkaListener(topics = { "ct" }, groupId = "test-consumer-group2", containerFactory = "manualListenerContainerFactory")
    public void receive2(ConsumerRecord record, Acknowledgment ack) {
        log.info("消费者2接收到的消息：" + record.value());
        ack.acknowledge();
        Thread.sleep(1000);
    }

    @Bean("manualListenerContainerFactory2")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> manualListenerContainerFactory2(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true); // 批量更新
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // 配置手动提交offset
        return factory;
    }

    // 批量手动提交 offset 消费者
    @SneakyThrows
    @KafkaHandler
    @KafkaListener(topics = { "ct" }, groupId = "test-consumer-group3", containerFactory = "manualListenerContainerFactory2", properties = {
            "max.poll.records: 10", // 设置最大批量poll条数
    })
    public void receive3(ConsumerRecords<?, ?> records, Acknowledgment ack) {
        StringBuilder sb = new StringBuilder();
        Iterator<? extends ConsumerRecord<?, ?>> iterator = records.iterator();
        while (iterator.hasNext()) {
            ConsumerRecord<?, ?> next = iterator.next();
            sb.append("\n").append("==> ").append(next.value());
        }
        log.info("消费者3接收到的消息：" + sb);
        ack.acknowledge();
        Thread.sleep(1000);
    }

}
