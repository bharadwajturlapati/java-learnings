package org.example.javasselearnings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class SSEController {

    @GetMapping("/stream-sse")
    public SseEmitter streamSse() {
        // Create an SseEmitter with a timeout (if necessary)
        SseEmitter emitter = new SseEmitter();

        // ScheduledExecutorService to send events at fixed intervals
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        // Counter to limit the number of events sent
        final int[] counter = {0};
        final int maxEvents = 10;

        // Schedule tasks to send events every 2 seconds
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (counter[0] < maxEvents) {
                    // Send event with current UTC time
                    String currentTime = ZonedDateTime.now(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ISO_INSTANT);
                    emitter.send(//message)
                            SseEmitter.event().data("Current time: " + currentTime)
                    );

                    counter[0]++;  // Increment event counter
                } else {
                    emitter.complete();  // Complete the SSE connection after sending maxEvents
                    scheduledExecutorService.shutdown();  // Shutdown the executor service
                }
                //fetch stock data push to client
            } catch (IOException e) {
                //emitter.onError(messaage -> pipetoKafka(client, message));
                emitter.completeWithError(e);  // Complete the emitter with an error if any issue occurs
                scheduledExecutorService.shutdown();  // Shutdown the executor on error
            }
        }, 0, 2, TimeUnit.MILLISECONDS);  // Initial delay of 0 seconds, and send every 2 seconds

        // Ensure the ScheduledExecutorService is properly shut down on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!scheduledExecutorService.isShutdown()) {
                scheduledExecutorService.shutdown();
            }
        }));

        return emitter;  // Return the SseEmitter to the client
    }
}