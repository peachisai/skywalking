from openai import OpenAI
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter
from opentelemetry.instrumentation.openai import OpenAIInstrumentor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

provider = TracerProvider()

otlp_exporter = OTLPSpanExporter(
    endpoint="http://localhost:11800",
    insecure=True
)
processor = BatchSpanProcessor(otlp_exporter)
provider.add_span_processor(processor)
trace.set_tracer_provider(provider)

OpenAIInstrumentor().instrument()

client = OpenAI(
    timeout=120.0,
)


def get_ai_streaming_response(prompt):
    response = client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=[{"role": "user", "content": prompt}]
    )
    print(response.choices[0].message.content)
    return


result = get_ai_streaming_response("Write a short poem on OpenTelemetry.")
print(result)
provider.force_flush()