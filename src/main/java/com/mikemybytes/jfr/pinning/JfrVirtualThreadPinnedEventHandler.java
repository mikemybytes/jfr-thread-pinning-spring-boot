package com.mikemybytes.jfr.pinning;

import io.micrometer.core.instrument.MeterRegistry;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Component
class JfrVirtualThreadPinnedEventHandler {

    // improvement idea: expose as a configuration
    private static final int STACK_TRACE_MAX_DEPTH = 25;

    private final Logger log = LoggerFactory.getLogger(JfrVirtualThreadPinnedEventHandler.class);

    private final MeterRegistry meterRegistry;

    JfrVirtualThreadPinnedEventHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    void handle(RecordedEvent event) {
        // marked as nullable in Javadoc
        var thread = event.getThread() != null ? event.getThread().getJavaName() : "<unknown>";
        var duration = event.getDuration();
        var startTime = LocalDateTime.ofInstant(event.getStartTime(), ZoneId.systemDefault());
        var stackTrace = formatStackTrace(event.getStackTrace(), STACK_TRACE_MAX_DEPTH);

        log.warn(
                "Thread '{}' pinned for: {}ms at {}, stacktrace: \n{}",
                thread,
                duration.toMillis(),
                startTime,
                stackTrace
        );

        var timer = meterRegistry.timer("jfr.thread.pinning");
        timer.record(duration);
    }

    private String formatStackTrace(RecordedStackTrace stackTrace, int maxDepth) {
        if (stackTrace == null) {
            return "\t<not available>";
        }
        String formatted = "\t" + stackTrace.getFrames().stream()
                .limit(maxDepth)
                .map(JfrVirtualThreadPinnedEventHandler::formatStackTraceFrame)
                .collect(Collectors.joining("\n\t"));
        if (maxDepth < stackTrace.getFrames().size()) {
            return formatted + "\n\t(...)"; // truncated
        }
        return formatted;
    }

    private static String formatStackTraceFrame(RecordedFrame frame) {
        return frame.getMethod().getType().getName() + "#" + frame.getMethod().getName() + ": " + frame.getLineNumber();
    }

}
