# Datadog Integration Guide

**Phase 2: Datadog Monitoring & APM Setup**

This guide covers integrating Datadog for full-stack Application Performance Monitoring (APM), infrastructure metrics, and log aggregation.

---

## Table of Contents

1. [What is Datadog?](#what-is-datadog)
2. [Setup Datadog Locally](#setup-datadog-locally)
3. [Java Spring Boot Instrumentation](#java-spring-boot-instrumentation)
4. [APM Tracing](#apm-tracing)
5. [Custom Metrics](#custom-metrics)
6. [Log Correlation](#log-correlation)
7. [Datadog Dashboards](#datadog-dashboards)
8. [Troubleshooting](#troubleshooting)

---

## What is Datadog?

Datadog is a cloud-based monitoring platform that provides:

### 🔍 APM (Application Performance Monitoring)
- Distributed traces of requests through your system
- Latency metrics (p50, p95, p99)
- Error rates and exception tracking
- Database query performance
- Example: See exactly which service is slow when a request takes 5 seconds

### 📊 Infrastructure Monitoring
- CPU, memory, disk usage per container/host
- Network I/O metrics
- Process-level monitoring
- Example: Identify when application slows down due to memory pressure

### 📝 Log Aggregation
- Centralized log collection from all services
- Structured logging (JSON) for easy searching
- Log correlation with traces (via `dd.trace_id`)
- Full-text search and filtering
- Example: Find all errors related to a specific order ID across 3 environments

### 🚨 Alerts & Notifications
- Alert rules based on metrics/logs
- Multi-channel notifications (Slack, PagerDuty, email)
- Example: Alert on error rate > 5% or response time p95 > 1000ms

---

## Setup Datadog Locally

### 1. Create Datadog Account

Visit: https://www.datadoghq.com/free-datadog-trial/

- **Free tier includes:** 3 user roles, custom dashboards, 15-day retention
- **Education:** Students sometimes get extended free tier (check with Datadog)

### 2. Get API Key

After signup:
1. Go to https://app.datadoghq.com/organization/settings/api-keys
2. Create new API key
3. Copy the key (keep it secret!)

### 3. Add Datadog Agent to local docker-compose.yml

Update [docker-compose.yml](docker-compose.yml):

```yaml
version: '3.8'

services:
  # ... existing services ...

  datadog:
    image: gcr.io/datadog-api/agent:latest
    container_name: connectly-datadog-agent
    environment:
      DD_API_KEY: ${DD_API_KEY}           # From your Datadog account
      DD_SITE: datadoghq.com              # Or datadoghq.eu if you're in Europe
      DD_LOGS_ENABLED: "true"
      DD_APM_ENABLED: "true"
      DD_AGENT_HOST: 127.0.0.1
      DD_DOCKER_LABELS_AS_TAGS: "true"
    ports:
      - "8126:8126/tcp"                   # APM traces
      - "8125:8125/udp"                   # Metrics
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    networks:
      - connectly-stage-network
      - connectly-demo-network
      - connectly-prod-network

volumes:
  # ... existing volumes ...

networks:
  # ... existing networks ...
```

### 4. Set Environment Variables

Create `.env` in project root:

```bash
DD_API_KEY=your-api-key-here
DD_SITE=datadoghq.com

# Or use .env file
export $(cat .env | xargs)
```

### 5. Start Services with Datadog Agent

```bash
docker-compose up -d datadog

# Verify agent is running
docker-compose logs datadog | grep "Agent is up to date"

# Check health
curl -s http://localhost:8126/v1/config | jq .
```

---

## Java Spring Boot Instrumentation

### Dependencies Already Added ✅

Your `build.gradle` now includes:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.micrometer:micrometer-registry-datadog:1.12.2'
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

### Enable Datadog in Application Profiles

#### **application-dev.properties**
```properties
# Datadog disabled for local development
management.metrics.export.datadog.enabled=false
```

#### **application-stage.properties**
```properties
# Datadog enabled for staging
management.metrics.export.datadog.enabled=true
management.metrics.export.datadog.api-key=${DD_API_KEY}
management.metrics.export.datadog.application-key=${DD_APP_KEY}
management.metrics.export.datadog.step=1m
management.metrics.tags.service=connectly-api
management.metrics.tags.env=stage
```

#### **application-prod.properties**
```properties
# Datadog fully enabled for production
management.metrics.export.datadog.enabled=true
management.metrics.export.datadog.api-key=${DD_API_KEY}
management.metrics.export.datadog.application-key=${DD_APP_KEY}
management.metrics.export.datadog.step=30s
management.metrics.tags.service=connectly-api
management.metrics.tags.env=prod
management.metrics.tags.version=1.0.0
```

### Datadog APM Java Agent (Optional - For Enhanced Tracing)

For more detailed APM tracing, add Datadog Java Agent:

```dockerfile
# In Dockerfile, download agent during build
RUN curl -L https://dtdg.co/latest-java-tracer -o dd-java-agent.jar

# In entrypoint:
ENTRYPOINT ["java", "-javaagent:dd-java-agent.jar", "-jar", "app.jar"]
```

---

## APM Tracing

### Automatic Tracing (Out of the Box)

Spring Boot with MVC autom atically traces:
- ✅ HTTP requests (`@RestController` endpoints)
- ✅ Database queries (JPA/Hibernate)
- ✅ Spring Service method calls
- ✅ HTTP client calls (RestTemplate, WebClient)

No code changes needed!

### Manual Tracing (For Custom Operations)

Add traces to custom business logic:

```java
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.SpanCustomizer;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final Tracer tracer;
    
    public OrderService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public Order createOrder(Order order) {
        // Automatically traced
        return orderRepository.save(order);
    }
    
    public void processComplexLogic(String orderId) {
        // Create custom span
        var span = tracer.nextSpan().name("process-complex-logic");
        try (var scope = tracer.withSpan(span.start())) {
            span.tag("order.id", orderId);
            
            // Your business logic here
            doComplexStuff(orderId);
            
        } finally {
            span.end();
        }
    }
}
```

### View Traces in Datadog

1. Go to https://app.datadoghq.com/apm/service-map
2. Click on `connectly-api` service
3. View traces for each endpoint
4. Click on individual trace to see:
   - Request timeline (HTTP → DB → responses)
   - SQL queries executed
   - Response time breakdown
   - Errors and exceptions

---

## Custom Metrics

### Emit Counters

```java
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final MeterRegistry meterRegistry;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public Order createOrder(Order order) {
        order = orderRepository.save(order);
        
        // Increment counter
        meterRegistry.counter("connectly.orders.created", 
            "status", order.getStatus(),
            "user_type", order.getUser().getType()
        ).increment();
        
        return order;
    }
    
    public void completeOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        
        // Increment business metric
        meterRegistry.counter("connectly.orders.completed").increment();
    }
}
```

### Emit Gauges (Current Value)

```java
@Component
public class InventoryMetrics {
    
    private final InventoryService inventoryService;
    
    public InventoryMetrics(InventoryService inventoryService, MeterRegistry registry) {
        this.inventoryService = inventoryService;
        
        // Register gauge (called periodically)
        Gauge.gauge("connectly.inventory.available",
            () -> inventoryService.getAvailableCount(),
            registry
        );
    }
}
```

### Emit Timers (Execution Time)

```java
import io.micrometer.core.instrument.Timer;

@Service
public class PaymentService {
    
    private final MeterRegistry meterRegistry;
    
    public PaymentService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public PaymentResult processPayment(Payment payment) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Call external payment gateway
            PaymentResult result = paymentGateway.charge(payment);
            
            return result;
        } finally {
            sample.stop(Timer.builder("connectly.payment.processing_time")
                .tag("status", result.getStatus())
                .register(meterRegistry)
            );
        }
    }
}
```

### Query Metrics in Datadog

```
# Average order creation rate per minute
avg:connectly.orders.created.rate{service:connectly-api}

# Current inventory
gauge:connectly.inventory.available{service:connectly-api}

# Payment processing time percentiles
p95:connectly.payment.processing_time{service:connectly-api}
```

---

## Log Correlation

### Structured Logging (Already Configured ✅)

Your `logback-spring.xml` outputs JSON logs:

```json
{
  "timestamp": "2026-05-16T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.jomariabejo.connectly_api.service.OrderService",
  "message": "Order created successfully",
  "thread": "http-nio-8080-exec-1",
  "service": "connectly-api",
  "environment": "stage",
  "dd.trace_id": "123456789",
  "dd.span_id": "987654321"
}
```

### Use MDC for Custom Context

```java
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // Add custom context
        MDC.put("order.id", order.getId());
        MDC.put("customer.email", order.getCustomer().getEmail());
        
        logger.info("Processing order");
        
        try {
            Order saved = orderService.createOrder(order);
            logger.info("Order created successfully");
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Failed to create order", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

In Datadog, query:

```
service:connectly-api customer.email:john@example.com
```

---

## Datadog Dashboards

### Create Application Performance Dashboard

1. Go to Dashboards → New Dashboard → Timeseries
2. Add widgets:

**Request Latency**
```
Metric: trace.web.request.duration
Aggregation: p95
Filter: service:connectly-api env:prod
```

**Error Rate**
```
Metric: trace.web.request.errors
Formula: (errors / total_requests) * 100
```

**JVM Memory**
```
Metric: jvm.memory.used
Aggregation: avg
Filter: service:connectly-api
```

**Database Queries**
```
Metric: trace.db.query.duration
Aggregation: p99
Filter: service:connectly-api
```

### Create Business Metrics Dashboard

**Order Creation Rate**
```
Metric: connectly.orders.created.rate
Breakdown: group by status
```

**Payment Success Rate**
```
Formula: (successful_payments / total_payments) * 100
Filter: service:connectly-api env:prod
```

---

## Alerts & Notifications

### Create Alert: High Error Rate

1. Monitors → New Monitor → Metric
2. **Metric:** `trace.web.request.errors`
3. **Condition:** `> 5%` for last `5 minutes`
4. **Notification:** Slack `@connectly #alerts`

### Create Alert: High Latency

1. **Metric:** `trace.web.request.duration`
2. **Condition:** `p95 > 1000ms` (1 second)
3. **Notification:** Slack `@devops-oncall`

### Slack Integration

1. In Datadog: Integrationss → Slack
2. Click "Install"
3. Choose your Slack workspace
4. In alerts, set notification: `@slack-connectly-alerts`

---

## Troubleshooting

### No Traces in Datadog

```bash
# 1. Check Datadog agent is running
docker-compose logs datadog | grep "Agent is up to date"

# 2. Verify API key is correct
docker-compose exec datadog agent status | grep "API Key"

# 3. Check application is sending traces
curl -X GET http://localhost:8126/v1/config 2>/dev/null | jq .

# 4. Restart agent
docker-compose restart datadog

# 5. Check application logs
docker-compose logs api-stage | grep -i "datadog\|trace"
```

### Traces Not Correlated with Logs

```bash
# Enable trace ID injection in logs
# Already configured in logback-spring.xml

# Verify logs contain dd.trace_id
docker-compose logs api-stage | grep "dd.trace_id"

# If missing, check:
# 1. Spring profile is set (not 'dev')
# 2. Datadog agent is reachable
# 3. Logs are in JSON format
```

### Metrics Not Appearing

```bash
# 1. Check metrics endpoint
curl http://localhost:8081/api/actuator/metrics

# 2. Verify Datadog config in application-stage.properties
# - management.metrics.export.datadog.enabled=true
# - API key set

# 3. Restart application
docker-compose restart api-stage

# 4. Wait 60 seconds (metrics flush interval)
# 5. Check Datadog UI: Metrics → Explorer
```

### High Data Volume / Costs

Datadog charges by volume. To reduce:

```properties
# Increase flush interval (slower updates)
management.metrics.export.datadog.step=5m

# Disable individual meters
management.metrics.enable.jvm=false
management.metrics.enable.process.uptime=false

# Filter tags
management.metrics.tags.env=prod
```

---

## Example: End-to-End Order Trace

### 1. Create Order via API

```bash
curl -X POST http://localhost:8083/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"product":"book","quantity":2}]}'
```

### 2. Trace in Datadog

1. APM → Service Map → Trace
2. See the flow:
   ```
   POST /api/v1/orders (50ms)
      ├─ JPA: INSERT into orders (15ms)
      ├─ JPA: INSERT into order_items (10ms)
      ├─ HTTP: POST payment-gateway (20ms)
      └─ Cache set (5ms)
   ```

### 3. View Logs

1. Logs → Facets:
   - `service:connectly-api`
   - `order.id:12345`
   - `environment:prod`

### 4. Check Metrics

```
connectly.orders.created{service:connectly-api} = 1
trace.web.request.duration{service:connectly-api,resource:POST /api/v1/orders} = 50ms
```

---

## Best Practices

✅ **DO:**
- Tag metrics consistently (service, environment, user_type)
- Use structured logging (JSON format)
- Correlate traces with logs via trace IDs
- Set up alerts before production
- Regular review of dashboards

❌ **DON'T:**
- Commit API keys to git (use environment variables)
- Create too many custom metrics (costs money)
- Log sensitive data (passwords, credit cards)
- Forget to set environment tags

---

## Next Steps

- Implement GitHub Actions CI/CD (see [Deployment](deployment.md) Phase 3)
- Deploy to DigitalOcean (see [Deployment](deployment.md) Phase 4)

[← Documentation index](../README.md)
- Monitor Datadog dashboards in production

---

**Happy Monitoring! 📊**
