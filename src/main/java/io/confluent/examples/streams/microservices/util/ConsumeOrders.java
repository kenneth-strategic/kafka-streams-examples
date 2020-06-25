package io.confluent.examples.streams.microservices.util;

import io.confluent.examples.streams.microservices.domain.Schemas.Topics;
import io.confluent.examples.streams.utils.MonitoringInterceptorUtils;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static io.confluent.examples.streams.microservices.util.MicroserviceUtils.buildPropertiesFromConfigFile;

public class ConsumeOrders {

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(final String[] args) throws Exception {

        final Options opts = new Options();
        opts.addOption(Option.builder("b")
                        .longOpt("bootstrap-server").hasArg().desc("Kafka cluster bootstrap server string").build())
                .addOption(Option.builder("s")
                        .longOpt("schema-registry").hasArg().desc("Schema Registry URL").build())
                .addOption(Option.builder("c")
                        .longOpt("config-file").hasArg().desc("Java properties file with configurations for Kafka Clients").build())
                .addOption(Option.builder("h")
                        .longOpt("help").hasArg(false).desc("Show usage information").build());

        final CommandLine cl = new DefaultParser().parse(opts, args);

        final String bootstrapServers = cl.getOptionValue("b", "localhost:9092");
        final String schemaRegistryUrl = cl.getOptionValue("schema-registry", "http://localhost:8081");

        final Properties defaultConfig =
                buildPropertiesFromConfigFile(Optional.ofNullable(cl.getOptionValue("config-file", null)));

        final Properties props = new Properties();
        props.putAll(defaultConfig);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-orders");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, Topics.ORDERS.keySerde().deserializer().getClass());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Topics.ORDERS.valueSerde().deserializer().getClass());

        try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("orders"));

            while (true) {
                // poll returns right away when there is data available.
                // the timeout is basically configuring "long poll" behavior, how long to keep checking when there
                // is no data available, so it's better to poll for a longer period of time
                final ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(30));
                for (final ConsumerRecord<String, String> record : records)
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
            }
        }
    }
}
