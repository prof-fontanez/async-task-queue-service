package com.acme.api.asynctaskqueue.worker;

import org.springframework.stereotype.Component;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created to simulate side-effectful case with retries and compensation.
 */
@Component
public class EmailJobHandler implements JobHandler {
    private static final Logger logger = LoggerFactory.getLogger(EmailJobHandler.class);

    @Override
    public void execute(Map<String, Object> payload) throws Exception {
        // Simulate a long-running job
        Thread.sleep(3000);
        // Extract email fields if present (optional, for logging)
        String to = (String) payload.getOrDefault("to", "unknown@acme.com");
        String subject = (String) payload.getOrDefault("subject", "<no-subject>");

        logger.info("Executing email job to {} with subject '{}'", to, subject);

        // Simulate flakiness (for retries demo)
        if (Math.random() < 0.4) {
            logger.warn("Simulated SMTP temp failure for {}", to);
            throw new RuntimeException("SMTP temp failure");
        }

        // Simulate successful send
        logger.info("Email sent successfully to {}", to);
    }

    @Override
    public void compensate(Map<String, Object> lastKnownState) {
        // Extract job info if available
        Map<String, Object> payload = (Map<String, Object>) lastKnownState.get("payload");
        String to = (String) (payload != null ? payload.getOrDefault("to", "unknown@acme.com") : "unknown@acme.com");

        // Log compensation action
        logger.info("Compensating email job for recipient {}. Undoing any side effects.", to);

        // In a real scenario, you might send a “sorry/ignore previous email” message or reverse DB changes
        // Here we just log for demonstration
    }
}
