package com.mikemybytes.jfr.pinning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@RestController
@RequestMapping("/pinning")
class ThreadPinningController {

    private static final Logger log = LoggerFactory.getLogger(ThreadPinningController.class);

    @PostMapping
    void pinCarrierThread() throws InterruptedException {
        var cdl = new CountDownLatch(1);
        Thread.ofVirtual()
                .name("web-vt-" + UUID.randomUUID())
                .start(() -> {
                    synchronized (this) {
                        log.info("Causing thread pinning for example purposes");
                        sleep(Duration.ofMillis(250));
                    }
                    cdl.countDown();
                });
        cdl.await();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new IllegalStateException("Unexpected InterruptedException", e);
        }
    }

}
