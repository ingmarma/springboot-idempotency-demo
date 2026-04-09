package com.ingmarma.idempotency;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentService {

    // Contador de pagos procesados
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger duplicatesBlocked = new AtomicInteger(0);

    // Store de idempotencia — en producción usarías Redis
    private final ConcurrentHashMap<String, String> idempotencyStore = new ConcurrentHashMap<>();

    // Procesar pago SIN idempotencia — procesa siempre
    public String processPaymentUnsafe(String orderId, double amount) {
        simulatePaymentProcessing();
        int count = totalProcessed.incrementAndGet();
        return String.format("Pago procesado [SIN idempotencia] — orderId: %s, monto: %.2f, total procesados: %d", 
                orderId, amount, count);
    }

    // Procesar pago CON idempotencia — procesa solo una vez por orderId
    public String processPaymentSafe(String orderId, double amount) {
        // Verificar si ya fue procesado
        String existing = idempotencyStore.putIfAbsent(orderId, "PROCESSING");
        
        if (existing != null) {
            int blocked = duplicatesBlocked.incrementAndGet();
            return String.format("DUPLICADO BLOQUEADO [CON idempotencia] — orderId: %s ya procesado. Duplicados bloqueados: %d", 
                    orderId, blocked);
        }

        simulatePaymentProcessing();
        int count = totalProcessed.incrementAndGet();
        idempotencyStore.put(orderId, "COMPLETED");
        
        return String.format("Pago procesado [CON idempotencia] — orderId: %s, monto: %.2f, total procesados: %d", 
                orderId, amount, count);
    }

    public String getStats() {
        return String.format("Total procesados: %d | Duplicados bloqueados: %d", 
                totalProcessed.get(), duplicatesBlocked.get());
    }

    public void reset() {
        totalProcessed.set(0);
        duplicatesBlocked.set(0);
        idempotencyStore.clear();
    }

    private void simulatePaymentProcessing() {
        try {
            Thread.sleep(50); // simula procesamiento real
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
