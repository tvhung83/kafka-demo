package com.example.kafka;

import com.example.kafka.avro.generated.Order;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static io.undertow.util.StatusCodes.ACCEPTED;

@Slf4j
public class KafkaModule extends AbstractModule {
    public static final String TOPIC_NAME = "orders";
    public static final Gson GSON = new Gson();

    @Provides
    static KafkaProducer<String, SpecificRecord> producer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        return new KafkaProducer<>(props);
    }

    @Provides @Named("receiver")
    @Inject
    static HttpHandler receive(KafkaProducer<String, SpecificRecord> producer) {
        Receiver.FullStringCallback callback = (exchange, message) -> {
            // deserialize with GSON
            final Order value = GSON.fromJson(message, Order.class);
            // get key from email
            final String key = value.getEmail();

            final ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>(TOPIC_NAME, key, value);
            producer.send(record, (m, e) -> {
                if (e == null) {
                    log.debug("Produced record to topic {} partition [{}] @ offset {}",
                            m.topic(), m.partition(), m.offset());
                } else {
                    log.error("Send failed for record {}", record, e);
                }
            });

            exchange.setStatusCode(ACCEPTED);
        };
        return exchange -> exchange.getRequestReceiver().receiveFullString(callback, StandardCharsets.UTF_8);
    }
}
