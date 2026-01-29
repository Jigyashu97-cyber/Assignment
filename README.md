# Payment Gateway Routing Service

A Spring Boot-based payment service that integrates with multiple payment gateways (Razorpay, PayU, Cashfree) to provide high availability, redundancy, and seamless payment experience during peak traffic.

## 🌟 Features

- **Intelligent Gateway Routing**: Distributes load across gateways based on configurable percentage weights
- **Real-time Health Monitoring**: Tracks success/failure rates with time-window analysis
- **Auto-recovery**: Temporarily disables gateways with < 90% success rate for 30 minutes
- **Thread-safe In-memory Storage**: Uses ConcurrentHashMap for transaction and health data
- **Comprehensive Logging**: Detailed logging at all transaction lifecycle stages
- **RESTful APIs**: Clean REST endpoints for transaction initiation and callbacks
- **Production-ready**: Proper error handling, validation, and configuration management

## 🏗️ Architecture

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
       v
┌──────────────────────────────────┐
│  TransactionController           │
│  - POST /transactions/initiate   │
│  - POST /transactions/callback   │
└──────┬───────────────────────────┘
       │
       v
┌──────────────────────────────────┐
│  TransactionService              │
│  - Initiate transactions         │
│  - Process callbacks             │
└──────┬───────────────────────────┘
       │
       ├─────────────────────┬──────────────────────┐
       │                     │                      │
       v                     v                      v
┌─────────────────┐  ┌──────────────────┐  ┌─────────────────┐
│ PaymentRouting  │  │ GatewayHealth    │  │ Transaction     │
│ Service         │  │ Service          │  │ Repository      │
│ - Weight-based  │  │ - Health checks  │  │ - In-memory     │
│   routing       │  │ - Metrics        │  │   storage       │
└─────────────────┘  └──────────────────┘  └─────────────────┘
```

## 📋 Project Structure

```
src/main/java/com/payment/gateway/
├── controller/
│   └── TransactionController.java          # REST API endpoints
├── service/
│   ├── PaymentRoutingService.java          # Gateway selection logic
│   ├── TransactionService.java             # Transaction management
│   └── GatewayHealthService.java           # Health monitoring
├── repository/
│   ├── TransactionRepository.java          # Transaction storage
│   └── GatewayHealthRepository.java        # Health data storage
├── model/
│   ├── Transaction.java                    # Transaction entity
│   ├── PaymentInstrument.java              # Payment details
│   └── GatewayHealth.java                  # Health metrics
├── dto/
│   ├── InitiateTransactionRequest.java     # API request DTOs
│   ├── InitiateTransactionResponse.java
│   ├── CallbackRequest.java
│   └── CallbackResponse.java
├── config/
│   └── GatewayConfig.java                  # Configuration properties
├── exception/
│   ├── NoHealthyGatewayException.java      # Custom exceptions
│   └── GlobalExceptionHandler.java         # Error handling
└── PaymentGatewayApplication.java          # Main application
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/Jigyashu97-cyber/Assignment.git
cd Assignment
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📡 API Documentation

### 1. Initiate Transaction

**Endpoint:** `POST /transactions/initiate`

**Request Body:**
```json
{
  "orderId": "ORD123",
  "amount": 499.0,
  "paymentInstrument": {
    "type": "card",
    "cardNumber": "****1234",
    "expiry": "12/25"
  }
}
```

**Success Response (201 Created):**
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "ORD123",
  "amount": 499.0,
  "gateway": "razorpay",
  "status": "pending",
  "attemptNumber": 1,
  "createdAt": "2024-01-29T18:30:00",
  "message": "Transaction initiated successfully"
}
```

**Error Response (503 Service Unavailable):**
```json
{
  "timestamp": "2024-01-29T18:30:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "All payment gateways are currently unavailable. Please try again later."
}
```

### 2. Process Callback

**Endpoint:** `POST /transactions/callback`

**Request Body:**
```json
{
  "orderId": "ORD123",
  "status": "success",
  "gateway": "razorpay",
  "reason": null
}
```

**Success Response (200 OK):**
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "ORD123",
  "status": "success",
  "gateway": "razorpay",
  "updatedAt": "2024-01-29T18:31:00",
  "message": "Callback processed successfully"
}
```

**Error Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-29T18:31:00",
  "status": 400,
  "error": "Bad Request",
  "message": "No pending transaction found for orderId: ORD123 with gateway: razorpay"
}
```

## ⚙️ Configuration

### Gateway Weights (application.yml)

```yaml
payment:
  gateway:
    weights:
      razorpay: 40    # 40% of traffic
      payu: 35        # 35% of traffic
      cashfree: 25    # 25% of traffic
    health:
      success-threshold: 90.0      # Minimum success rate %
      time-window-minutes: 15      # Time window for metrics
      cooldown-minutes: 30         # Recovery cooldown period
```

### Customizing Configuration

You can override default values by:

1. **Modifying application.yml:**
```yaml
payment:
  gateway:
    weights:
      razorpay: 50
      payu: 30
      cashfree: 20
```

2. **Using environment variables:**
```bash
export PAYMENT_GATEWAY_WEIGHTS_RAZORPAY=50
export PAYMENT_GATEWAY_HEALTH_SUCCESS_THRESHOLD=85
```

3. **Command-line arguments:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--payment.gateway.weights.razorpay=50"
```

## 🔍 Key Features Explained

### 1. Weighted Gateway Selection

The routing service distributes traffic based on configured weights:
- Razorpay: 40% (default)
- PayU: 35% (default)
- Cashfree: 25% (default)

Only healthy gateways participate in routing. Weights are automatically normalized if some gateways are unhealthy.

### 2. Health Monitoring

The system tracks gateway health in real-time:
- **Success Rate Calculation**: Based on transactions in the last 15 minutes
- **Health Threshold**: 90% success rate required
- **Automatic Recovery**: 30-minute cooldown after marking unhealthy
- **Time-window Analysis**: Old transactions automatically cleaned up

### 3. Transaction Management

- **Multiple Attempts**: Supports multiple payment attempts for the same order
- **Attempt Tracking**: Each attempt gets a unique transaction ID and attempt number
- **Status Tracking**: Pending → Success/Failure
- **Comprehensive Logging**: All state changes logged

### 4. Error Handling

- **Validation Errors**: Returns 400 with field-level errors
- **No Healthy Gateway**: Returns 503 when all gateways are down
- **Not Found**: Returns 400 when transaction not found
- **Server Errors**: Returns 500 for unexpected errors

## 📊 Logging

The application provides comprehensive logging:

- **DEBUG**: Gateway selection logic, health calculations
- **INFO**: Transaction state changes, gateway status changes
- **WARN**: Gateway failures, validation issues
- **ERROR**: Critical errors, all gateways down

Logs are written to:
- Console (with colored output)
- File: `logs/payment-gateway.log` (with rotation)

## 🧪 Testing the Application

### Using cURL

1. **Initiate a transaction:**
```bash
curl -X POST http://localhost:8080/transactions/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD123",
    "amount": 499.0,
    "paymentInstrument": {
      "type": "card",
      "cardNumber": "****1234",
      "expiry": "12/25"
    }
  }'
```

2. **Send a success callback:**
```bash
curl -X POST http://localhost:8080/transactions/callback \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD123",
    "status": "success",
    "gateway": "razorpay"
  }'
```

3. **Send a failure callback:**
```bash
curl -X POST http://localhost:8080/transactions/callback \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD456",
    "status": "failure",
    "gateway": "payu",
    "reason": "Insufficient funds"
  }'
```

### Testing Health Degradation

1. Create several failed transactions to trigger health degradation:
```bash
# Initiate transaction
curl -X POST http://localhost:8080/transactions/initiate \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ORD'$i'", "amount": 100.0, "paymentInstrument": {"type": "card", "cardNumber": "****", "expiry": "12/25"}}'

# Mark as failed (repeat 10+ times with different orderIds)
curl -X POST http://localhost:8080/transactions/callback \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ORD'$i'", "status": "failure", "gateway": "razorpay", "reason": "Test"}'
```

2. Watch the logs to see gateway health degradation and exclusion from routing.

## 🎯 Success Metrics

- ✅ Working REST APIs with proper validation
- ✅ Intelligent routing based on weights and health
- ✅ Real-time health tracking with time-window analysis
- ✅ Gateway auto-recovery after cooldown period
- ✅ Comprehensive logging at all levels
- ✅ Thread-safe in-memory storage
- ✅ Proper exception handling and error responses
- ✅ Clean, modular, production-ready code

## 🔮 Future Enhancements

- [ ] Docker containerization
- [ ] Unit and integration tests
- [ ] Persistent database storage
- [ ] Metrics and monitoring (Prometheus/Grafana)
- [ ] Admin API for viewing health status
- [ ] Circuit breaker pattern
- [ ] Distributed tracing
- [ ] API authentication/authorization
- [ ] Rate limiting

## 📝 License

This project is part of a technical assignment.

## 👤 Author

Jigyashu

---

For questions or issues, please open an issue on GitHub.
