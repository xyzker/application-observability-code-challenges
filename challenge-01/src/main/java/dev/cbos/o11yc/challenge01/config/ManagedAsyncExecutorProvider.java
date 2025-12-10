package dev.cbos.o11yc.challenge01.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import org.glassfish.jersey.server.ManagedAsyncExecutor;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

@ManagedAsyncExecutor
public class ManagedAsyncExecutorProvider extends ThreadPoolExecutorProvider {

    private final AttributeKey<String> TYPE = AttributeKey.stringKey("type");
    private final String CORE = "core";
    private final String ACTIVE = "active";
    private final String MAX = "max";
    private final String CURRENT = "current";

    private final String CREATED = "total_created";
    private final String COMPLETED = "completed";

    public ManagedAsyncExecutorProvider() {
        super("challenge01-managed-async-executor");
    }

    @PostConstruct
    public void init() {
        //Get the executor, it created only once as singleton, so we can use this to add metrics
        final ThreadPoolExecutor executor = getExecutor();

        Meter meter = GlobalOpenTelemetry.getMeter("Challenge01Application");
        // Add measurements for the threads
        meter.gaugeBuilder("managed_async_executor_threads")
                .ofLongs()
                .buildWithCallback(
                        measurement -> {
                            measurement.record(executor.getCorePoolSize(), Attributes.of(TYPE, CORE));
                            measurement.record(executor.getMaximumPoolSize(), Attributes.of(TYPE, MAX));
                            measurement.record(executor.getPoolSize(), Attributes.of(TYPE, CURRENT));
                            measurement.record(executor.getActiveCount(), Attributes.of(TYPE, ACTIVE));
                        });

        // Add measurements for the tasks
        meter.gaugeBuilder("managed_async_executor_tasks")
                .ofLongs()
                .buildWithCallback(
                        measurement -> {
                            measurement.record(executor.getTaskCount(), Attributes.of(TYPE, CREATED));
                            measurement.record(executor.getCompletedTaskCount(), Attributes.of(TYPE, COMPLETED));
                        });

        // Add measurements for the queue
        meter.gaugeBuilder("managed_async_executor_queue")
                .ofLongs()
                .buildWithCallback(
                        measurement -> {
                            measurement.record(executor.getQueue().size());
                        });
    }

    @Override
    protected int getCorePoolSize() {
        return 4;
    }

    @Override
    protected int getMaximumPoolSize() {
        return 40;
    }

    @Override
    protected BlockingQueue<Runnable> getWorkQueue() {
        // Just create a small queue, to prevent immediate new thread creation, but if the queue is full, new threads can be created
        return new LinkedBlockingQueue<>(2);
    }
}
