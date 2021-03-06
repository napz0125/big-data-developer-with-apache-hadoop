# FLUME AND KAFKA INTEGRATION EXAMPLE
````
- Even though Flume and Kafka exist for different purposes they can complement each other, hence understanding both the technologies as well as integrating them is important.
- Kafka is lot more reliable and scalable than Flume
- However, if you have to publish messages from existing application’s web server logs, we have to refactor application to publish to Kafka topic using publisher API
- Some of the legacy applications are highly sensitive for changes
- In that case, we can use Flume and create agent which can
- Read from web server logs
- Publish to Kafka topic
- Once we got data to Kafka topic we can get benefit of scalability, reliability as well as agility for downstream applications to consume messages
````

# FLUME AGENT TO PUBLISH TO KAFKA
````text
- Source: /opt/gen_logs/logs/access.log
- Channel: memory
- Sink: Kafka

- Validate
- Run Kafka consumer command to see data is being streamed
````

````properties
# wskafka.conf: A single-node Flume configuration
# to read data from webserver logs and publish
# to kafka topic
# start_logs

# Name the components on this agent
wk.sources = ws
wk.sinks = kafka
wk.channels = mem

# Describe/configure the source
wk.sources.ws.type = exec
wk.sources.ws.command = tail -F /opt/gen_logs/logs/access.log

# Describe the sink
wk.sinks.kafka.type = org.apache.flume.sink.kafka.KafkaSink
wk.sinks.kafka.brokerList = localhost:9092
wk.sinks.kafka.topic = fkdemodg

# Use a channel wkich buffers events in memory
wk.channels.mem.type = memory
wk.channels.mem.capacity = 1000
wk.channels.mem.transactionCapacity = 100

# Bind the source and sink to the channel
wk.sources.ws.channels = mem
wk.sinks.kafka.channel = mem
````
````text
$  flume-ng agent --name wk --conf-file /home/cloudera/flume_demo/flume_kafka.conf

kafka-console-consumer.sh \
  --zookeeper localhost:2181 \
  --topic fkdemodg \
  --from-beginning
````