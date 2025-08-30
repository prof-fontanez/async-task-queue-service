package com.acme.api.asynctaskqueue.worker;

import org.springframework.context.annotation.Configuration;

@Configuration
public class JobHandlerBootstrap {
    public JobHandlerBootstrap(JobHandlerRegistry registry, EmailJobHandler email, ReportJobHandler report) {
        registry.register("sendEmail", email);
        registry.register("generateReport", report);
    }
}
