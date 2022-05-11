package com.example.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public abstract class ConsumeLoop<K, V extends SpecificRecord> implements Runnable {
    private final KafkaConsumer<K, V> consumer;
    private final List<String> topics;
    private final CountDownLatch shutdownLatch;

    public ConsumeLoop(KafkaConsumer<K, V> consumer, List<String> topics) {
        this.consumer = consumer;
        this.topics = topics;
        this.shutdownLatch = new CountDownLatch(1);

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.shutdown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public abstract void process(ConsumerRecord<K, V> record);

    @Override
    public void run() {
        try (consumer) {
            consumer.subscribe(topics);
            while (true) {
                ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
                records.forEach(this::process);
                try {
                    consumer.commitSync();
                } catch (CommitFailedException e) {
                    log.warn("Failed to commit: {}", e.getMessage(), e);
                }
            }
        } catch (WakeupException e) {
            log.info("Consumer thread is closing...");
        } catch (Exception e) {
            log.error("Unexpected error", e);
        } finally {
            shutdownLatch.countDown();
        }
    }

    public void shutdown() throws InterruptedException {
        consumer.wakeup();
        shutdownLatch.await();
    }
}
