package com.example.workflowcommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Retry Configuration for fault-tolerant operations.
 * Automatically retries failed database operations and external calls.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Retry is enabled via annotations on service methods:
    // @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    // @Recover methods handle final failures
}
