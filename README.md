## Thread pinning detection with JFR and Spring Boot

This application illustrates how to integrate [JFR (Java Flight Recorder) Event Streaming](https://openjdk.org/jeps/349)
with [Spring Boot](https://spring.io/projects/spring-boot) for monitoring of [pinned threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html#GUID-704A716D-0662-4BC7-8C7F-66EE74B1EDAD).

The approach implemented here has been described in details in the [dedicated blog post](https://mikemybytes.com/2024/04/17/continuous-monitoring-of-pinned-threads-with-spring-boot-and-jfr/).

## High-level overview

- JFR event streaming session running in background ([JfrEventLifecycle.java](src/main/java/com/mikemybytes/jfr/pinning/JfrEventLifecycle.java))
- Custom `jdk.VirtualThreadPinned` JFR event handler, logging event details and reporting its duration as a [Micrometer `Timer`](https://docs.micrometer.io/micrometer/reference/concepts/timers.html) ([JfrVirtualThreadPinnedEventHandler.java](/src/main/java/com/mikemybytes/jfr/pinning/JfrVirtualThreadPinnedEventHandler.java))
- HTTP endpoint for causing thread pinning on demand ([ThreadPinningController.java](src/main/java/com/mikemybytes/jfr/pinning/ThreadPinningController.java))

## Requirements

- Java 21+
- Maven 3.9+ (compatible wrapper provided)

## Running the application locally

```bash
# building the project
./mvnw clean verify

# running the app (port 8080)
./mvnw spring-boot:run 
```

The application exposes a dedicated `POST /pinning` endpoint, which causes thread pinning by [blocking from within the 
`synchronized` block](https://mikemybytes.com/2024/02/28/curiosities-of-java-virtual-threads-pinning-with-synchronized/):

```
curl -X POST localhost:8080/pinning
```

Detected pinning should be logged like this:

```
2024-04-16T22:15:49.321+02:00  WARN 20226 --- [jfr-thread-pinning-spring-boot] [ Event Stream 1] m.j.p.JfrVirtualThreadPinnedEventHandler : Thread 'web-vt-5773efb7-e4f8-404a-8859-590b0ff1f1cb' pinned for: 254ms at 2024-04-16T22:15:48.974019792, stacktrace:
	java.lang.VirtualThread#parkOnCarrierThread: 675
	java.lang.VirtualThread#parkNanos: 634
	java.lang.VirtualThread#sleepNanos: 791
	java.lang.Thread#sleep: 507
	com.mikemybytes.jfr.pinning.ThreadPinningController#sleep: 36
	com.mikemybytes.jfr.pinning.ThreadPinningController#lambda$pinCarrierThread$0: 27
	com.mikemybytes.jfr.pinning.ThreadPinningController$$Lambda+0x000000c0014ec010.615676186#run: -1
	java.lang.Thread#runWith: 1596
	java.lang.VirtualThread#run: 309
	java.lang.VirtualThread$VThreadContinuation$1#run: 190
	jdk.internal.vm.Continuation#enter0: 320
	jdk.internal.vm.Continuation#enter: 312
	jdk.internal.vm.Continuation#enterSpecial: -1
```

Additionally, the duration of the recorded pinning events should be exposed as a timer
in the `/actuator/prometheus` endpoint output:
```
(...)
# HELP jfr_thread_pinning_seconds_max  
# TYPE jfr_thread_pinning_seconds_max gauge
jfr_thread_pinning_seconds_max 0.25093325
# HELP jfr_thread_pinning_seconds  
# TYPE jfr_thread_pinning_seconds summary
jfr_thread_pinning_seconds_count 1.0
jfr_thread_pinning_seconds_sum 0.25093325
(...)
```

For more details, please refer to the original article [_Continuous monitoring of pinned threads with Spring Boot and JFR_](https://mikemybytes.com/2024/04/17/continuous-monitoring-of-pinned-threads-with-spring-boot-and-jfr/).
