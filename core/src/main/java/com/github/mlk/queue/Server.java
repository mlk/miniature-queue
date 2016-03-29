package com.github.mlk.queue;

import com.github.mlk.queue.implementation.ServerImplementation;

public abstract class Server {
    protected abstract ServerImplementation getImplementation();
    public void close() {
        getImplementation().close();
    }
}
