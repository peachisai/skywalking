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

RocketMQ monitoring provides multidimensional metrics monitoring of RocketMQ Exporter as `Layer: RocketMQ` `Service` in
the OAP. In each cluster, the broker are represented as `Instance`.

### RocketMQ Cluster Supported Metrics

| Monitoring Panel                           |Unit        | Metric Name                                                             | Description                                                                                            | Data Source      |
|--------------------------------------------|------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------|
| Messages Produced Today                    | Count      | meter_rocketmq_cluster_messages_produced_today                          | The number of the cluster messages produced today.                                                     | RocketMQ Exporter |
| Messages Consumed Today                    | Count      | meter_rocketmq_cluster_messages_consumed_today                          | The number of the cluster messages consumed today.                                                     | RocketMQ Exporter |
| Total Producer Tps                         | Msg/sec    | meter_rocketmq_cluster_total_producer_tps                               | The number of messages produced per second per broker.                                                 | RocketMQ Exporter |
| Total Consume Tps                          | Msg/sec    | meter_rocketmq_cluster_total_producer_tps                               | The number of messages consumed per second per broker.                                                 | RocketMQ Exporter |
| Producer Message Size                      | Bytes/sec  | meter_rocketmq_cluster_producer_message_size                            | The size of a message produced per broker.                                                             | RocketMQ Exporter |
| Consumer Message Size                      | Bytes/sec  | meter_rocketmq_cluster_consumer_message_size                            | The size of the consumed message per broker (byte per second).                                         | RocketMQ Exporter |
| Messages Produced Until Yesterday          | Count      | meter_rocketmq_cluster_messages_produced_until_yesterday                | The total number of messages put until 12 o'clock last night.                                          | RocketMQ Exporter |
| Messages Consumed Until Yesterday          | Count      | meter_rocketmq_cluster_messages_consumed_until_yesterday                | The total number of messages read until 12 o'clock last night.                                         | RocketMQ Exporter |
| Max Consumer Latency                       | ms         | meter_rocketmq_cluster_max_consumer_latency                             | The max number of consumer latency.                                                                    | RocketMQ Exporter |
| Max CommitLog Disk Ratio                   | %          | meter_rocketmq_cluster_max_commitLog_disk_ratio                         | The max utilization ratio of the commit log disk.                                                      | RocketMQ Exporter |
| CommitLog Disk Ratio                       | %          | meter_rocketmq_cluster_commitLog_disk_ratio                             | The utilization ratio of commit log disk per brokerIp.                                                 | RocketMQ Exporter |
| Pull ThreadPool Queue Head Wait Time       | ms         | meter_rocketmq_cluster_pull_threadPool_queue_head_wait_time             | The wait time in milliseconds for pulling threadPool queue (millisecond unit).                         | RocketMQ Exporter |
| Send ThreadPool Queue Head Wait Time       | ms         | meter_rocketmq_cluster_send_threadPool_queue_head_wait_time             | The wait time in milliseconds for sending threadPool queue (millisecond unit).                         | RocketMQ Exporter |

### RocketMQ Broker Supported Metrics

| Monitoring Panel                           |Unit        | Metric Name                                                             | Description                                                                                            | Data Source      |
|--------------------------------------------|------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------|
| Produce Tps                                | Count      | meter_rocketmq_broker_produce_tps                                       | The number of broker produces messages per second.                                                     | RocketMQ Exporter |
| Consume Qps                                | Count      | meter_rocketmq_broker_consume_qps                                       | The number of broker consumes messages per second.                                                     | RocketMQ Exporter |
| Producer Message Size                      | Bytes/sec  | meter_rocketmq_broker_producer_message_size                             | The size of the messages produced.                                                                     | RocketMQ Exporter |
| Consumer Message Size                      | Bytes/sec  | meter_rocketmq_broker_consumer_message_size                             | The size of the messages consumed.                                                                     | RocketMQ Exporter |

### RocketMQ Topic Supported Metrics

| Monitoring Panel                           |Unit        | Metric Name                                                             | Description                                                                                            | Data Source      |
|--------------------------------------------|------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|------------------|
| Messages Produced Today                    | Count      | meter_rocketmq_cluster_messages_produced_today                          | The number of the cluster messages produced today.                                                     | RocketMQ Exporter |
| Messages Consumed Today                    | Count      | meter_rocketmq_cluster_messages_consumed_today                          | The number of the cluster messages consumed today.                                                     | RocketMQ Exporter |
| Total Producer Tps                         | Count      | meter_rocketmq_cluster_total_producer_tps                               | The number of messages produced per second per broker.                                                 | RocketMQ Exporter |
| Total Consume Tps                          | Count      | meter_rocketmq_cluster_total_producer_tps                               | The number of messages consumed per second per broker.                                                 | RocketMQ Exporter |
| Producer Message Size                      | Bytes/sec  | meter_rocketmq_cluster_producer_message_size                            | The size of a message produced per broker.                                                             | RocketMQ Exporter |
| Consumer Message Size                      | Bytes/sec  | meter_rocketmq_cluster_consumer_message_size                            | The size of the consumed message per broker (byte per second).                                         | RocketMQ Exporter |
| Messages Produced Until Yesterday          | Count      | meter_rocketmq_cluster_messages_produced_until_yesterday                | The total number of messages put until 12 o'clock last night.                                          | RocketMQ Exporter |
| Messages Consumed Until Yesterday          | Count      | meter_rocketmq_cluster_messages_consumed_until_yesterday                | The total number of messages read until 12 o'clock last night.                                         | RocketMQ Exporter |
| Max Consumer Latency                       | ms         | meter_rocketmq_cluster_max_consumer_latency                             | The max number of consumer latency.                                                                    | RocketMQ Exporter |
| Max CommitLog Disk Ratio                   | %          | meter_rocketmq_cluster_max_commitLog_disk_ratio                         | The max utilization ratio of the commit log disk.                                                      | RocketMQ Exporter |
| CommitLog Disk Ratio                       | %          | meter_rocketmq_cluster_commitLog_disk_ratio                             | The utilization ratio of commit log disk per brokerIp.                                                 | RocketMQ Exporter |
| Pull ThreadPool Queue Head Wait Time       | ms         | meter_rocketmq_cluster_pull_threadPool_queue_head_wait_time             | The wait time in milliseconds for pulling threadPool queue (millisecond unit).                         | RocketMQ Exporter |
| Send ThreadPool Queue Head Wait Time       | ms         | meter_rocketmq_cluster_send_threadPool_queue_head_wait_time             | The wait time in milliseconds for sending threadPool queue (millisecond unit).                         | RocketMQ Exporter |

## Customizations

You can customize your own metrics/expression/dashboard panel.
The metrics definition and expression rules are found
in `otel-rules/rocketmq/rocketmq-cluster.yaml, otel-rules/rocketmq/rocketmq-broker.yaml, otel-rules/rocketmq/rocketmq-topic.yaml`.
The RocketMQ dashboard panel configurations are found in `ui-initialized-templates/rocketmq`.