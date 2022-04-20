/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.example.kafka;

import com.example.kafka.avro.generated.Order;
import com.example.kafka.guice.HandlerModule;
import com.example.kafka.guice.KafkaModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

import static com.example.kafka.guice.HandlerModule.TOPIC_NAME;

@Slf4j
public class ConsumerApp {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new KafkaModule(), new HandlerModule());
        Properties props = injector.getInstance(Key.get(Properties.class, Names.named("consumer")));
        final KafkaConsumer<String, Order> consumer = new KafkaConsumer<>(props);

        final ConsumeLoop<String, Order> loop = new ConsumeLoop<>(consumer, ImmutableList.of(TOPIC_NAME)) {
            @Override
            public void process(ConsumerRecord<String, Order> record) {
                log.debug(">>> Key [{}] >> orderNumber [{}]", record.key(), record.value().getOrderNumber());
            }
        };

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                loop.shutdown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        new Thread(loop).start();
    }
}