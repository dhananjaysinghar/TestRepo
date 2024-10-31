import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KafkaConsumerHealthIndicator implements HealthIndicator {

    private final MeterRegistry meterRegistry;

    public KafkaConsumerHealthIndicator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        // Get partition-wise lag details
        Map<String, Object> partitionLagDetails = new HashMap<>();
        try {
            Search search = meterRegistry.find("kafka.consumer.records-lag");
            search.meters().forEach(meter -> {
                String topic = meter.getId().getTag("topic");
                String partition = meter.getId().getTag("partition");

                // Get the lag value for each partition
                Double lag = meterRegistry.get(meter.getId().getName()).tags(meter.getId().getTags()).gauge().value();

                if (topic != null && partition != null && lag != null) {
                    Map<String, Object> lagDetail = new HashMap<>();
                    lagDetail.put("lag", lag);
                    partitionLagDetails.put("topic-" + topic + "-partition-" + partition, lagDetail);
                }
            });
            details.put("partitionLag", partitionLagDetails);
        } catch (Exception e) {
            details.put("partitionLag", "Unavailable");
        }

        return Health.up().withDetails(details).build();
    }
}
