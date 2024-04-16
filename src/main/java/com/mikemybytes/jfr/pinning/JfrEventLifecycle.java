package com.mikemybytes.jfr.pinning;

import jdk.jfr.consumer.RecordingStream;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
class JfrEventLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final JfrVirtualThreadPinnedEventHandler virtualThreadPinnedEventHandler;

    private RecordingStream recordingStream;

    JfrEventLifecycle(JfrVirtualThreadPinnedEventHandler virtualThreadPinnedEventHandler) {
        this.virtualThreadPinnedEventHandler = virtualThreadPinnedEventHandler;
    }

    @Override
    public void start() {
        if (!isRunning()) {
            recordingStream = new RecordingStream();

            recordingStream.enable("jdk.VirtualThreadPinned").withStackTrace();
            recordingStream.onEvent("jdk.VirtualThreadPinned", virtualThreadPinnedEventHandler::handle);

            // prevents memory leaks in long-running apps
            recordingStream.setMaxAge(Duration.ofSeconds(10));

            recordingStream.startAsync();
            running.set(true);
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            recordingStream.close();
            running.set(false);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

}