package com.github.mlk.queue.rabbitmq.example;

import com.github.mlk.queue.Handle;
import com.github.mlk.queue.Publish;
import com.github.mlk.queue.Queue;
import com.google.common.base.Function;


@Queue("messages")
public interface MessageQueue {
    @Publish
    void publishMessage(String message);

    @Handle
    void receiveMessage(Function<String, Boolean> function);
}
