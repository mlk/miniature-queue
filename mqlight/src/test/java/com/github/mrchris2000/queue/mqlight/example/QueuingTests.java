package com.github.mrchris2000.queue.mqlight.example;

import com.github.mlk.queue.*;
import com.github.mlk.queue.codex.StringDecoder;
import com.github.mlk.queue.codex.StringEncoder;
import com.github.mrchris2000.queue.mqlight.MqLightServer;
import org.junit.ClassRule;
import org.junit.Test;
import pl.domzal.junit.docker.rule.DockerRule;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueuingTests {
    @ClassRule
    public static DockerRule dockerRule =
            DockerRule.builder()
                    .imageName("ibmcom/mqlight:1.0")
                    .env("LICENSE","accept")
                    .env("MQLIGHT_USER","user")
                    .env("MQLIGHT_PASSWORD","password")
                    .publishAllPorts(true)
                    .waitForMessage("Monitoring MQ Light...", 90)
                    .build();

    @Queue(value = "/fanout-example", queueTypeHint = QueueType.FANOUT_QUEUE)
    interface FanoutExampleQueue {
        @Publish
        void publishMessage(String message);

        @Handle
        void receiveMessage(Function<String, Boolean> function);
    }

    @Test(timeout = 120000)
    public void whenItemPutOnQueueThenAllListenersRelieveACopy() throws InterruptedException {
        final AtomicBoolean oneReceiveMessage = new AtomicBoolean(false);
        final AtomicBoolean twoReceiveMessage = new AtomicBoolean(false);

        MqLightServer mqls =  new MqLightServer("amqp://user:password@"+ dockerRule.getDockerHost()+":"+dockerRule.getExposedContainerPort("5672"));
        MqLightServer mqls2 =  new MqLightServer("amqp://user:password@"+ dockerRule.getDockerHost()+":"+dockerRule.getExposedContainerPort("5672"));

        FanoutExampleQueue one = Queuify.builder().decoder(new StringDecoder()).server(mqls).target(FanoutExampleQueue.class);
        FanoutExampleQueue two = Queuify.builder().decoder(new StringDecoder()).server(mqls2).target(FanoutExampleQueue.class);
        FanoutExampleQueue sender = Queuify.builder().encoder(new StringEncoder()).server(mqls).target(FanoutExampleQueue.class);

        one.receiveMessage((x) -> { oneReceiveMessage.set(true); return true; });
        two.receiveMessage((x) -> { twoReceiveMessage.set(true); return true; });

        Thread.sleep(10000L); // Allow time for Docker container to be properly ready (Despite saying it's good to go).
        sender.publishMessage("msg");
        Thread.sleep(500L); // Allow time for message delivery to clients.

        assertTrue(oneReceiveMessage.get() && twoReceiveMessage.get());


    }

    @Queue(value = "/worker-example", queueTypeHint = QueueType.WORKER_QUEUE)
    interface WorkerExampleQueue {
        @Publish
        void publishMessage(String message);

        @Handle
        void receiveMessage(Function<String, Boolean> function);
    }

    @Test(timeout = 120000)
    public void whenItemPutOnQueueThenASingleListenerRecievesACopy() throws InterruptedException {
        final AtomicBoolean oneReceiveMessage = new AtomicBoolean(false);
        final AtomicBoolean twoReceiveMessage = new AtomicBoolean(false);

        MqLightServer mqls =  new MqLightServer("amqp://user:password@"+ dockerRule.getDockerHost()+":"+dockerRule.getExposedContainerPort("5672"));
        MqLightServer mqls2 =  new MqLightServer("amqp://user:password@"+ dockerRule.getDockerHost()+":"+dockerRule.getExposedContainerPort("5672"));

        WorkerExampleQueue one = Queuify.builder().decoder(new StringDecoder()).server(mqls).target(WorkerExampleQueue.class);
        WorkerExampleQueue two = Queuify.builder().decoder(new StringDecoder()).server(mqls2).target(WorkerExampleQueue.class);

        WorkerExampleQueue sender = Queuify.builder().encoder(new StringEncoder()).server(mqls).target(WorkerExampleQueue.class);

        one.receiveMessage((x) -> { oneReceiveMessage.set(true); return true; });
        two.receiveMessage((x) -> { twoReceiveMessage.set(true); return true; });

        Thread.sleep(10000L);
        sender.publishMessage("msg");
        Thread.sleep(500L);

        assertFalse(oneReceiveMessage.get() && twoReceiveMessage.get());
        assertTrue(oneReceiveMessage.get() || twoReceiveMessage.get());
    }
}
