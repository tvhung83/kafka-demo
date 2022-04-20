package com.example.kafka.guice;

import com.example.kafka.avro.generated.Order;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static io.undertow.util.StatusCodes.ACCEPTED;

@Slf4j
public class HandlerModule extends AbstractModule {
    public static final String TOPIC_NAME = "orders";
    public static final Gson GSON = new Gson();

    @Provides @Named("receiver")
    @Inject
    static HttpHandler receive(@Named("producer") Properties props) {
        Receiver.FullStringCallback callback = (exchange, message) -> {
            // deserialize with GSON
            final Order value = GSON.fromJson(message, Order.class);
            // get key from email
            final String key = value.getEmail();
            final ProducerRecord<String, Order> record = new ProducerRecord<>(TOPIC_NAME, key, value);

            // send record to Kafka
            try (KafkaProducer<String, Order> producer = new KafkaProducer<>(props)) {
                producer.send(record, (m, e) -> {
                    if (e == null) {
                        log.debug("Produced record to topic {} partition [{}] @ offset {}",
                                m.topic(), m.partition(), m.offset());
                    } else {
                        log.error("Send failed for record {}", record, e);
                    }
                });
            }

            exchange.setStatusCode(ACCEPTED);
        };
        return exchange -> exchange.getRequestReceiver().receiveFullString(callback, StandardCharsets.UTF_8);
    }
}
