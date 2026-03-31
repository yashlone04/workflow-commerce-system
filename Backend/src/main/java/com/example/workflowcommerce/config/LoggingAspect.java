package com.example.workflowcommerce.config;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Logging Aspect for tracking request correlation and performance.
 * Adds correlation IDs for distributed tracing and logs execution time.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Log workflow service method executions with timing
     */
    @Around("execution(* com.example.workflowcommerce.service.workflow..*(..))")
    public Object logWorkflowExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("correlationId", correlationId);
        }

        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        logger.info("[{}] START: {}", correlationId, methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] END: {} - Duration: {}ms", correlationId, methodName, duration);
            
            // Log slow operations (> 500ms)
            if (duration > 500) {
                logger.warn("[{}] SLOW OPERATION: {} took {}ms", correlationId, methodName, duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[{}] ERROR: {} - Duration: {}ms - Error: {}", 
                correlationId, methodName, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Log controller method executions
     */
    @Around("execution(* com.example.workflowcommerce.controller..*(..))")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("correlationId", correlationId);

        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] API: {} - {}ms", correlationId, methodName, duration);
            return result;
        } catch (Exception e) {
            logger.error("[{}] API ERROR: {} - {}", correlationId, methodName, e.getMessage());
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
