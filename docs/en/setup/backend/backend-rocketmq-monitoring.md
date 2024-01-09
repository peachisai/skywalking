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

RocketMQ monitoring provides multidimensional metrics monitoring of RocketMQ cluster as `Layer: RocketMQ` `Service` in
the OAP. In each cluster, the broker are represented as `Instance`.

### RocketMQ Cluster Supported Metrics

| Monitoring Panel                         | Metric Name                                                             | Description                                                                                            | Data Source      |
|------------------------------------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------|
| Messages Produced Today                  | meter_rocketmq_cluster_messages_produced_today                          | The number of the cluster messages produced today.                                                     | RocketMQ Cluster |
| Messages Consumed Today                  | meter_rocketmq_cluster_messages_consumed_today                          | The number of the cluster messages consumed today.                                                     | RocketMQ Cluster |
| Total Producer Tps                       | meter_rocketmq_cluster_total_producer_tps                               | The number of messages produced per second per broker.                                                 | RocketMQ Cluster |
| Total Consume Tps                        | meter_rocketmq_cluster_total_producer_tps                               | The number of messages consumed per second per broker.                                                 | RocketMQ Cluster |
| Producer Message Size                    | meter_rocketmq_cluster_producer_message_size                            | The size of a message produced per broker (byte per second).                                           | RocketMQ Cluster |
| Consumer Message Size                    | meter_rocketmq_cluster_consumer_message_size                            | The size of the consumed message per broker (byte per second).                                         | RocketMQ Cluster |
| Messages Produced Until Yesterday        | meter_rocketmq_cluster_messages_produced_until_yesterday                | The total number of messages put until 12 o'clock last night.                                          | RocketMQ Cluster |
| Messages Consumed Until Yesterday        | meter_rocketmq_cluster_messages_consumed_until_yesterday                | The total number of messages read until 12 o'clock last night.                                         | RocketMQ Cluster |
| Max Consumer Latency                     | meter_rocketmq_cluster_max_consumer_latency                             | The max number of consumer latency.                                                                    | RocketMQ Cluster |
| Max CommitLog Disk Ratio                 | meter_rocketmq_cluster_max_commitLog_disk_ratio                         | The max utilization ratio of the commit log disk.                                                      | RocketMQ Cluster |
| CommitLog Disk Ratio                     | meter_rocketmq_cluster_commitLog_disk_ratio                             | The utilization ratio of commit log disk per brokerIp.                                                 | RocketMQ Cluster |
| Pull ThreadPool Queue Head Wait Time     | meter_rocketmq_cluster_pull_threadPool_queue_head_wait_time             | The wait time in milliseconds for pulling threadPool queue (millisecond unit).                         | RocketMQ Cluster |
| Send ThreadPool Queue Head Wait Time     | meter_rocketmq_cluster_send_threadPool_queue_head_wait_time             | The wait time in milliseconds for sending threadPool queue (millisecond unit).                         | RocketMQ Cluster |

### RocketMQ Node Supported Metrics


| Monitoring Panel                | Metric Name                                                                                                                                                                         | Description                                             | Data Source    |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|----------------|
| Active Connections              | meter_RocketMQ_broker_active_connections                                                                                                                                              | The number of active connections.                       | RocketMQ Broker  |
| Total Connections               | meter_RocketMQ_broker_total_connections                                                                                                                                               | The total number of connections.                        | RocketMQ Broker  |
| Connection Create Success Count | meter_RocketMQ_broker_connection_create_success_count                                                                                                                                 | The number of successfully created connections.         | RocketMQ Broker  |
| Connection Create Fail Count    | meter_RocketMQ_broker_connection_create_fail_count                                                                                                                                    | The number of failed connections.                       | RocketMQ Broker  |
| Connection Closed Total Count   | meter_RocketMQ_broker_connection_closed_total_count                                                                                                                                   | The total number of closed connections.                 | RocketMQ Broker  |
| JVM Buffer Pool Used            | meter_RocketMQ_broker_jvm_buffer_pool_used_bytes                                                                                                                                      | The usage of jvm buffer pool.                           | RocketMQ Broker  |
| JVM Memory Pool Used            | meter_RocketMQ_broker_jvm_memory_pool_used                                                                                                                                            | The usage of jvm memory pool.                           | RocketMQ Broker  |
| JVM Memory                      | meter_RocketMQ_broker_jvm_memory_init <br /> meter_RocketMQ_broker_jvm_memory_used <br /> meter_RocketMQ_broker_jvm_memory_committed                                                      | The usage of jvm memory.                                | RocketMQ Broker  |
| JVM Threads                     | meter_RocketMQ_broker_jvm_threads_current <br /> meter_RocketMQ_broker_jvm_threads_daemon <br /> meter_RocketMQ_broker_jvm_threads_peak <br /> meter_RocketMQ_broker_jvm_threads_deadlocked | The usage of jvm threads.                               | RocketMQ Broker  |
| GC Time                         | meter_RocketMQ_broker_jvm_gc_collection_seconds_sum                                                                                                                                   | Time spent in a given JVM garbage collector in seconds. | RocketMQ Broker  |
| GC Count                        | meter_RocketMQ_broker_jvm_gc_collection_seconds_count                                                                                                                                 | The count of a given JVM garbage collector.             | RocketMQ Broker  |

## Customizations

You can customize your own metrics/expression/dashboard panel.
The metrics definition and expression rules are found
in `otel-rules/rocketmq/rocketmq-cluster.yaml, otel-rules/rocketmq/rocketmq-broker.yaml`.
The RocketMQ dashboard panel configurations are found in `ui-initialized-templates/rocketmq`.