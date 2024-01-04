# RocketMQ monitoring

SkyWalking leverages rocketmq-exporter for collecting metrics data from RocketMQ. It leverages OpenTelemetry
Collector to transfer the metrics to
[OpenTelemetry receiver](opentelemetry-receiver.md) and into the [Meter System](./../../concepts-and-designs/meter.md).

## Data flow

1. The `rocketmq-exporter` collects metrics data from RocketMQ, The RocketMQ version is required to be 4.3.2+.
2. OpenTelemetry Collector fetches metrics from rocketmq-exporter via Prometheus Receiver and pushes metrics to
   SkyWalking OAP Server via OpenTelemetry gRPC exporter.
3. The SkyWalking OAP Server parses the expression with [MAL](../../concepts-and-designs/mal.md) to
   filter/calculate/aggregate and store the results.

## Setup

1. Setup [mongodb-exporter](https://github.com/percona/mongodb_exporter).
2. Set up [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/getting-started/#docker). The example for OpenTelemetry Collector configuration, refer
   to [here](../../../../test/e2e-v2/cases/mongodb/otel-collector-config.yaml).
3. Config SkyWalking [OpenTelemetry receiver](opentelemetry-receiver.md).

## RocketMQ Monitoring

RocketMQ monitoring provides multidimensional metrics monitoring of RocketMQ cluster as `Layer: PULSAR` `Service` in
the OAP. In each cluster, the broker are represented as `Instance`.

### RocketMQ Cluster Supported Metrics

| Monitoring Panel     | Metric Name                                | Description                                                                                            | Data Source    |
|-------------------------------|-----------------------------------|--------------------------------------------------------------------------------------------------------|----------------|
| Messages Produced Today         | meter_rocketmq_messages_produced_today                  | The number of the cluster messages produced today.                                                           | Pulsar Cluster |
| Messages Consumed Today  | meter_rocketmq_messages_consumed_today           | The number of the cluster messages consumed today.                                                    | Pulsar Cluster |
| Total Producer Tps      | meter_rocketmq_total_producer_tps               | The number of messages produced per second per broker.                                              | Pulsar Cluster |
| Total Consumers      | meter_pulsar_total_consumers               | The number of active consumers connected to this cluster.                                              | Pulsar Cluster |
| Message Rate In      | meter_pulsar_message_rate_in               | The total message rate coming into this cluster (message per second).                                  | Pulsar Cluster |
| Message Rate Out     | meter_pulsar_message_rate_out              | The total message rate going out from this cluster (message per second).                               | Pulsar Cluster |
| Throughput In        | meter_pulsar_throughput_in                 | The total throughput coming into this cluster (byte per second).                                       | Pulsar Cluster |
| Throughput Out       | meter_pulsar_throughput_out                | The total throughput going out from this cluster (byte per second).                                    | Pulsar Cluster |
| Storage Size         | meter_pulsar_storage_size                  | The total storage size of all topics in this broker (in bytes).                                        | Pulsar Cluster |
| Storage Logical Size | meter_pulsar_storage_logical_size          | The storage size of all topics in this broker without replicas (in bytes).                             | Pulsar Cluster |
| Storage Write Rate   | meter_pulsar_storage_write_rate            | The total message batches (entries) written to the storage for this broker (message batch per second). | Pulsar Cluster |
| Storage Read Rate    | meter_pulsar_storage_read_rate             | The total message batches (entries) read from the storage for this broker (message batch per second).  | Pulsar Cluster |


### RocketMQ Node Supported Metrics


| Monitoring Panel                | Metric Name                                                                                                                                                                         | Description                                             | Data Source    |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|----------------|
| Active Connections              | meter_pulsar_broker_active_connections                                                                                                                                              | The number of active connections.                       | Pulsar Broker  |
| Total Connections               | meter_pulsar_broker_total_connections                                                                                                                                               | The total number of connections.                        | Pulsar Broker  |
| Connection Create Success Count | meter_pulsar_broker_connection_create_success_count                                                                                                                                 | The number of successfully created connections.         | Pulsar Broker  |
| Connection Create Fail Count    | meter_pulsar_broker_connection_create_fail_count                                                                                                                                    | The number of failed connections.                       | Pulsar Broker  |
| Connection Closed Total Count   | meter_pulsar_broker_connection_closed_total_count                                                                                                                                   | The total number of closed connections.                 | Pulsar Broker  |
| JVM Buffer Pool Used            | meter_pulsar_broker_jvm_buffer_pool_used_bytes                                                                                                                                      | The usage of jvm buffer pool.                           | Pulsar Broker  |
| JVM Memory Pool Used            | meter_pulsar_broker_jvm_memory_pool_used                                                                                                                                            | The usage of jvm memory pool.                           | Pulsar Broker  |
| JVM Memory                      | meter_pulsar_broker_jvm_memory_init <br /> meter_pulsar_broker_jvm_memory_used <br /> meter_pulsar_broker_jvm_memory_committed                                                      | The usage of jvm memory.                                | Pulsar Broker  |
| JVM Threads                     | meter_pulsar_broker_jvm_threads_current <br /> meter_pulsar_broker_jvm_threads_daemon <br /> meter_pulsar_broker_jvm_threads_peak <br /> meter_pulsar_broker_jvm_threads_deadlocked | The usage of jvm threads.                               | Pulsar Broker  |
| GC Time                         | meter_pulsar_broker_jvm_gc_collection_seconds_sum                                                                                                                                   | Time spent in a given JVM garbage collector in seconds. | Pulsar Broker  |
| GC Count                        | meter_pulsar_broker_jvm_gc_collection_seconds_count                                                                                                                                 | The count of a given JVM garbage collector.             | Pulsar Broker  |

## Customizations

You can customize your own metrics/expression/dashboard panel.
The metrics definition and expression rules are found
in `otel-rules/rocketmq/rocketmq-cluster.yaml, otel-rules/rocketmq/rocketmq-broker.yaml`.
The RocketMQ dashboard panel configurations are found in `ui-initialized-templates/rocketmq`.