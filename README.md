# Spring Boot Idempotency Demo

Demostración real del impacto de la idempotencia y la diferencia entre
HashMap y ConcurrentHashMap bajo carga concurrente real.

---

## Resultado 1 — Idempotencia básica

| Escenario | Pagos procesados | Duplicados bloqueados |
|---|---|---|
| Sin idempotencia — `/api/payments/unsafe` | **5** | 0 |
| Con idempotencia — `/api/payments/safe` | **1** | **4** |

**Sin idempotencia: el mismo pago de $100 se procesa 5 veces = $500 cobrados.**
**Con idempotencia: se procesa 1 vez y 4 duplicados son bloqueados = $100 cobrados.**

---

## Resultado 2 — HashMap vs ConcurrentHashMap · 100 threads concurrentes

| Escenario | Threads | Procesados | Bloqueados | Errores |
|---|---|---|---|---|
| HashMap (NO thread-safe) | 100 | **100** | 0 | 0 |
| ConcurrentHashMap (thread-safe) | 100 | **1** | **99** | 0 |

**HashMap procesó el mismo pago 100 veces = $10.000 cobrados.**
**ConcurrentHashMap procesó exactamente 1 vez = $100 cobrados.**

Resultado consistente en 3 corridas consecutivas — no es aleatoriedad,
es comportamiento determinístico de estructuras thread-safe vs no thread-safe.

---

## ¿Por qué HashMap falla bajo concurrencia?
```java
// HashMap — get + put NO es atómico — race condition garantizada
String existing = unsafeMap.get(orderId);  // Thread A lee null
Thread.sleep(1);                            // Thread B también lee null
if (existing == null) {
    unsafeMap.put(orderId, "PROCESSED");   // Ambos escriben — duplicado
    processed.incrementAndGet();
}
```

Con 100 threads ejecutando simultáneamente, todos leen `null` antes de
que cualquiera escriba — todos procesan el pago.

---

## ¿Por qué ConcurrentHashMap funciona?
```java
// ConcurrentHashMap — putIfAbsent es ATÓMICO
String existing = safeMap.putIfAbsent(orderId, "PROCESSED");
if (existing == null) {
    processed.incrementAndGet(); // Solo 1 thread llega aquí
} else {
    duplicates.incrementAndGet(); // Los 99 restantes son bloqueados
}
```

`putIfAbsent` es una operación atómica — aunque lleguen 100 threads
simultáneamente, solo uno gana. Los demás reciben el valor existente.

---

## Idempotencia completa con ConcurrentHashMap
```java
@Service
public class PaymentService {

    private final ConcurrentHashMap<String, String> idempotencyStore
        = new ConcurrentHashMap<>();

    public String processPaymentSafe(String orderId, double amount) {
        String existing = idempotencyStore.putIfAbsent(orderId, "PROCESSING");

        if (existing != null) {
            return "DUPLICADO BLOQUEADO — ya procesado";
        }

        simulatePaymentProcessing();
        idempotencyStore.put(orderId, "COMPLETED");
        return "Pago procesado";
    }
}
```

En producción reemplazás el `ConcurrentHashMap` por **Redis** con TTL
para que el store sea distribuido entre múltiples instancias.

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
| `/api/payments/reset` | POST | Resetear contadores |
| `/api/payments/concurrency-test?threads=100` | GET | HashMap vs ConcurrentHashMap bajo carga |

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
# Test de concurrencia — HashMap vs ConcurrentHashMap
curl "http://localhost:8080/api/payments/concurrency-test?threads=100"

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