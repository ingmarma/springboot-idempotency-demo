# Spring Boot Idempotency Demo

Demostración real del impacto de la idempotencia en APIs de pagos.
El mismo request procesado 5 veces — con y sin protección.

---

## Resultados

| Escenario | Pagos procesados | Duplicados bloqueados |
|---|---|---|
| Sin idempotencia — `/api/payments/unsafe` | **5** | 0 |
| Con idempotencia — `/api/payments/safe` | **1** | **4** |

**Sin idempotencia: el mismo pago de $100 se procesa 5 veces = $500 cobrados.**
**Con idempotencia: se procesa 1 vez y 4 duplicados son bloqueados = $100 cobrados.**

---

## ¿Qué es idempotencia?

Una operación es idempotente cuando ejecutarla múltiples veces produce
el mismo resultado que ejecutarla una sola vez.

En APIs de pagos es crítico — los clientes reintentan requests ante
timeouts, errores de red o fallos de conectividad. Sin idempotencia,
cada reintento genera un cobro adicional.

---

## Cómo funciona
```java
// SIN idempotencia — procesa siempre
public String processPaymentUnsafe(String orderId, double amount) {
    simulatePaymentProcessing();
    return "Pago procesado";
}

// CON idempotencia — procesa solo una vez por orderId
public String processPaymentSafe(String orderId, double amount) {
    // putIfAbsent es atómico — thread-safe
    String existing = idempotencyStore.putIfAbsent(orderId, "PROCESSING");
    
    if (existing != null) {
        return "DUPLICADO BLOQUEADO — ya procesado";
    }
    
    simulatePaymentProcessing();
    idempotencyStore.put(orderId, "COMPLETED");
    return "Pago procesado";
}
```

---

## La clave — putIfAbsent

`ConcurrentHashMap.putIfAbsent()` es atómico — garantiza que aunque
lleguen 100 requests concurrentes con el mismo `orderId`, solo uno
pasa. Los demás son bloqueados inmediatamente.

En producción reemplazás el `ConcurrentHashMap` por **Redis** con TTL
para que el store de idempotencia sea distribuido y no se llene de memoria.

---

## Cuándo usar idempotencia

✅ Pagos y transacciones financieras
✅ Envío de emails o notificaciones
✅ Creación de recursos únicos
✅ Webhooks con reintentos automáticos (Stripe, PayPal, etc.)

❌ Consultas de lectura (GET) — ya son idempotentes por naturaleza
❌ Operaciones donde cada llamada debe ejecutarse (logs, métricas)

---

## Endpoints

| Endpoint | Método | Descripción |
|---|---|---|
| `/api/payments/unsafe` | POST | Sin idempotencia — procesa siempre |
| `/api/payments/safe` | POST | Con idempotencia — procesa una vez por orderId |
| `/api/payments/stats` | GET | Ver totales procesados y duplicados bloqueados |
| `/api/payments/reset` | POST | Resetear contadores para nueva prueba |

---

## Cómo reproducirlo
```bash
git clone https://github.com/ingmarma/springboot-idempotency-demo.git
cd springboot-idempotency-demo
mvn clean package -q
java -jar target/springboot-idempotency-demo-1.0-SNAPSHOT.jar --logging.level.root=INFO
```

En otra terminal:
```bash
# Sin idempotencia — 5 requests, 5 pagos procesados
for i in {1..5}; do
  curl -X POST "http://localhost:8080/api/payments/unsafe?orderId=ORDER-001&amount=100"
done

# Con idempotencia — 5 requests, 1 pago procesado
curl -X POST "http://localhost:8080/api/payments/reset"
for i in {1..5}; do
  curl -X POST "http://localhost:8080/api/payments/safe?orderId=ORDER-001&amount=100"
done
```

---

## Entorno

- Spring Boot 3.4.4
- Java 21.0.9 (Eclipse Adoptium)
- Maven 3.9.11
- Windows 11 — AMD64

## Stack

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat&logo=apache-maven&logoColor=white)

## Serie completa

- [java21-benchmarks](https://github.com/ingmarma/java21-benchmarks) — Virtual Threads, GC, Concurrencia
- [springboot-startup-benchmark](https://github.com/ingmarma/springboot-startup-benchmark) — Startup 44% más rápido
- [springboot-endpoint-benchmark](https://github.com/ingmarma/springboot-endpoint-benchmark) — @Cacheable 97% más rápido

## Autor

**Matías Martínez** — SRE & Backend Engineer
[linkedin.com/in/ingmarma](https://linkedin.com/in/ingmarma) · [github.com/ingmarma](https://github.com/ingmarma)