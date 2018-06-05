package net.redpipe.example.kafka;

import java.lang.management.ManagementFactory;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;

import com.sun.management.OperatingSystemMXBean;

import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaWriteStream;
import io.vertx.reactivex.core.AbstractVerticle;

public class MetricsVerticle extends AbstractVerticle {

	  private OperatingSystemMXBean systemMBean;
	  private KafkaWriteStream<String, JsonObject> producer;

	  @Override
	  public void start() throws Exception {
	    systemMBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

	    // A random identifier
	    String pid = UUID.randomUUID().toString();

	    // Get the kafka producer config
	    JsonObject config = config();

	    // Create the producer
	    producer = KafkaWriteStream.create(vertx.getDelegate(), config.getMap(), String.class, JsonObject.class);

	    // Publish the metircs in Kafka
	    vertx.setPeriodic(1000, id -> {
	      JsonObject metrics = new JsonObject();
	      metrics.put("CPU", systemMBean.getProcessCpuLoad());
	      metrics.put("Mem", systemMBean.getTotalPhysicalMemorySize() - systemMBean.getFreePhysicalMemorySize());
	      producer.write(new ProducerRecord<>("the_topic", new JsonObject().put(pid, metrics)));
	    });
	  }

	  @Override
	  public void stop() throws Exception {
	    if (producer != null) {
	      producer.close();
	    }
	  }
	}
