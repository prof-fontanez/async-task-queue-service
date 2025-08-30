package com.acme.api.asynctaskqueue.worker;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created to simulate non-side-effectful case with retries.
 */
@Component
public class ReportJobHandler implements JobHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportJobHandler.class);

    @Override
    public void execute(Map<String, Object> payload) throws Exception {
        // Simulate a long-running job
        Thread.sleep(3000);

        String reportName = (String) payload.getOrDefault("reportName", "default-report");

        logger.info("Executing report job: {}", reportName);

        // Simulate random failure for retry demonstration
        if (Math.random() < 0.3) {
            logger.warn("Simulated report generation failure for {}", reportName);
            throw new RuntimeException("Report generation temporary failure");
        }

        // Simulate successful report generation
        logger.info("Report {} generated successfully", reportName);
    }

    @Override
    public void compensate(Map<String, Object> lastKnownState) {
        String reportName = "unknown-report";
        Map<String, Object> payload = (Map<String, Object>) lastKnownState.get("payload");
        if (payload != null) {
            reportName = (String) payload.getOrDefault("reportName", "unknown-report");
        }

        // No real side effects, just log
        logger.info("Compensation for report job {} called, nothing to undo", reportName);
    }
}
