# Support Flink Monitoring
## Motivation
Apache Flink is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. Now that Skywalking can monitor OpenTelemetry metrics, I want to add Flink monitoring via the OpenTelemetry Collector, which fetches metrics from its own Http Endpoint
to expose metrics data for Prometheus.

## Architecture Graph
There is no significant architecture-level change.

## Proposed Changes
Flink expose its own [metrics](https://docs.konghq.com/hub/kong-inc/prometheus/) via HTTP endpoint to OpenTelemetry collector, using SkyWalking openTelemetry receiver to receive these metrics。
Provide cluster, taskManage, and job dimensions monitoring.

### Flink Cluster Supported Metrics

| Monitoring Panel              | Unit  | Metric Name                                           | Description                            | Data Source      |
|-------------------------------|-------|-------------------------------------------------------|----------------------------------------|------------------|
| Running Jobs                  | Count | meter_flink_jobManager_running_job_number             | The number of running jobs.            | Flink JobManager |
| TaskManagers                  | Count | meter_flink_jobManager_taskManagers_registered_number | The number of taskManagers.            | Flink JobManager |
| Jvm Cpu Load                  | Count | meter_flink_jobManager_jvm_cpu_load                   | The number of the Jvm cpu load.        | Flink JobManager |
| Jvm thread count              | Count | meter_flink_jobManager_jvm_thread_count               | The number of jvm threads.             | Flink JobManager |
| Jvm Memory Heap Used          | Count | meter_flink_jobManager_jvm_memory_heap_used           | The amount of jvm memory used.         | Flink JobManager |
| Jvm Memory NonHeap Used       | Count | meter_flink_jobManager_jvm_memory_NonHeap_used        | The amount of jvm nonHeap memory used. | Flink JobManager |
| Task Managers Slots Total     | Count | meter_flink_jobManager_taskManagers_slots_total       | The number of total slots.             | Flink JobManager |
| Task Managers Slots Available | Count | meter_flink_jobManager_taskManagers_slots_available   | The number of available slots.         | Flink JobManager |
| Jvm Cpu Time                  | Count | meter_flink_jobManager_jvm_cpu_time                   | The cpu time used by the JVM.          | Flink JobManager |

### Flink taskManager Supported Metrics

| Monitoring Panel                           |Unit        | Metric Name                                                             | Description                                        | Data Source        |
|--------------------------------------------|------------|-------------------------------------------------------------------------|----------------------------------------------------|--------------------|
| Produce TPS                                | Msg/sec    | meter_rocketmq_broker_produce_tps                                       | The number of broker produces messages per second. | Flink taskManager  |
| Consume QPS                                | Msg/sec    | meter_rocketmq_broker_consume_qps                                       | The number of broker consumes messages per second. | Flink taskManager  |
| Producer Message Size                      | Bytes/sec  | meter_rocketmq_broker_producer_message_size                             | The max size of the messages produced per second.  | Flink taskManager  |
| Consumer Message Size                      | Bytes/sec  | meter_rocketmq_broker_consumer_message_size                             | The max size of the messages consumed per second.  | Flink taskManager  |

### Flink Job Supported Metrics

| Monitoring Panel          | Unit      | Metric Name                                                      | Description                                                           | Data Source        |
|---------------------------|-----------|------------------------------------------------------------------|-----------------------------------------------------------------------|--------------------|
| Max Producer Message Size | Byte      | meter_rocketmq_topic_max_producer_message_size                   | The maximum number of messages produced.                              | Flink JobManager   |
| Max Consumer Message Size | Byte      | meter_rocketmq_topic_max_consumer_message_size                   | The maximum number of messages consumed.                              | RocketMQ Exporter  |
| Consumer Latency          | ms        | meter_rocketmq_topic_consumer_latency                            | Consumption delay time of a consumer group.                           | RocketMQ Exporter  |
| Producer Tps              | Msg/sec   | meter_rocketmq_topic_producer_tps                                | The number of messages produced per second.                           | RocketMQ Exporter  |
| Consumer Group Tps        | Msg/sec   | meter_rocketmq_topic_consumer_group_tps                          | The number of messages consumed per second per consumer group.        | RocketMQ Exporter  |
| Producer Offset           | Count     | meter_rocketmq_topic_producer_offset                             | The max progress of a topic's production message.                     | RocketMQ Exporter  |
| Consumer Group Offset     | Count     | meter_rocketmq_topic_consumer_group_offset                       | The max progress of a topic's consumption message per consumer group. | RocketMQ Exporter  |
| Producer Message Size     | Byte/sec  | meter_rocketmq_topic_producer_message_size                       | The max size of messages produced per second.                         | RocketMQ Exporter  |
| Consumer Message Size     | Byte/sec  | meter_rocketmq_topic_consumer_message_size                       | The max size of messages consumed per second.                         | RocketMQ Exporter  |
| Consumer Group_Count      | Count     | meter_rocketmq_topic_consumer_group_count                        | The number of consumer groups.                                        | RocketMQ Exporter  |
| Broker Count              | Count     | meter_rocketmq_topic_broker_count                                | The number of topics that received messages from the producer.        | RocketMQ Exporter  |

## Imported Dependencies libs and their licenses.
No new dependency.

## Compatibility
no breaking changes.

## General usage docs

This feature is out of the box.
