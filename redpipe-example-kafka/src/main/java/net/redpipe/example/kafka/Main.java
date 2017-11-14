package net.redpipe.example.kafka;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.core.Server;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import io.vertx.rxjava.kafka.client.consumer.KafkaConsumer;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

public class Main extends Server {
    public static void main( String[] args ){
    	new Server().start(new JsonObject().put("scan", new JsonArray().add(Main.class.getPackage().getName())))
    	.subscribe(v -> onStart(),
    			x -> x.printStackTrace());
    }

	private static void onStart() {
		System.err.println("Started");

		// Kafka setup for the example
	    File dataDir = Testing.Files.createTestingDirectory("cluster");
	    dataDir.deleteOnExit();
	    KafkaCluster kafkaCluster;
		try {
			kafkaCluster = new KafkaCluster()
			  .usingDirectory(dataDir)
			  .withPorts(2181, 9092)
			  .addBrokers(1)
			  .deleteDataPriorToStartup(true)
			  .startup();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	    // Deploy the dashboard
	    JsonObject consumerConfig = new JsonObject((Map) kafkaCluster.useTo()
	      .getConsumerProperties("the_group", "the_client", OffsetResetStrategy.LATEST));

	    AppGlobals globals = AppGlobals.get();
	    
	    // Create the consumer
		KafkaConsumer<String, JsonObject> consumer = KafkaConsumer.create(globals.getVertx(), (Map)consumerConfig.getMap(), 
	    		String.class, JsonObject.class);
		
		BehaviorSubject<JsonObject> consumerReporter = BehaviorSubject.create();
		consumer.toObservable().subscribe(record -> consumerReporter.onNext(record.value()));
		
	    // Subscribe to Kafka
	    consumer.subscribe("the_topic");
	    globals.setGlobal("consumer", consumerReporter);
	    

	    // Deploy the metrics collector : 3 times
	    JsonObject producerConfig = new JsonObject((Map) kafkaCluster.useTo()
	      .getProducerProperties("the_producer"));
	    globals.getVertx().deployVerticle(
	      MetricsVerticle.class.getName(),
	      new DeploymentOptions().setConfig(producerConfig).setInstances(3)
	    );
	}
}
