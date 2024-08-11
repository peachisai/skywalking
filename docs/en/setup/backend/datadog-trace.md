# Datadog java Trace Format

SkyWalking can receive traces from Datadog java agent and convert them to Zipkin Trace format eventually. 
For data analysis and queries related to Zipkin Trace, please [refer to the relevant documentation](./zipkin-trace.md#zipkin-query).

Datadog Trace handler references the [Zipkin Exporter in the OpenTelemetry Collector](https://opentelemetry.io/docs/specs/otel/trace/sdk_exporters/zipkin/#summary) to convert the data format.

## Set up backend receiver

1. Make sure to enable **receiver-datadog** module in `application.yml`.
```yaml
receiver-datadog:
  selector: ${SW_RECEIVER_DATADOG:-}
  default:
    port: ${SW_RECEIVER_DATADOG_PORT:8126}
    boosThreadGroupSize: ${SW_RECEIVER_DATADOG_BOSS_THREAD_GROUP_SIZE:4}
    workerThreadGroupSize: ${SW_RECEIVER_DATADOG_WORKER_THREAD_GROUP_SIZE:8}
```

2. Make sure to enable zipkin receiver and zipkin query in `application.yml` for config the zipkin.

## Setup Query and Lens UI 

Please read [deploy Lens UI documentation](./zipkin-trace.md#lens-ui) for query Datadog java traces.