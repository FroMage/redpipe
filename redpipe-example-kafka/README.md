This project contains an example Kafka application based on [the Vert.x Kafka
examples](https://github.com/vert-x3/vertx-examples/tree/master/kafka-examples).

It is using Kafka to query the load/memory of the machine, and sends it to the client via Server-Sent Events (SSE).

You can run the example by executing the `org.vertxrs.example.kafka.Main` class and
visiting the [main page](http://localhost:9000/static/index.html).
