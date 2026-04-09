package com.ingmarma.idempotency;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentConcurrencyService {

    // Test con HashMap — NO thread-safe
    public ConcurrencyResult testWithHashMap(int threads, String orderId)
            throws InterruptedException {
        HashMap<String, String> unsafeMap = new HashMap<>();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger duplicates = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        ready.countDown();
                        start.await(); // todos arrancan al mismo tiempo
                        // get + put NO es atómico — race condition garantizada
                        String existing = unsafeMap.get(orderId);
                        Thread.sleep(1); // simula latencia — maximiza race conditions
                        if (existing == null) {
                            unsafeMap.put(orderId, "PROCESSED");
                            processed.incrementAndGet();
                        } else {
                            duplicates.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await();
            start.countDown(); // disparo simultáneo
            done.await(10, TimeUnit.SECONDS);
        }

        return new ConcurrencyResult("HashMap (NO thread-safe)",
                threads, processed.get(), duplicates.get(), errors.get());
    }

    // Test con ConcurrentHashMap — thread-safe
    public ConcurrencyResult testWithConcurrentHashMap(int threads, String orderId)
            throws InterruptedException {
        ConcurrentHashMap<String, String> safeMap = new ConcurrentHashMap<>();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger duplicates = new AtomicInteger(0);

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        ready.countDown();
                        start.await(); // todos arrancan al mismo tiempo
                        // putIfAbsent es ATÓMICO — garantiza exactamente 1
                        String existing = safeMap.putIfAbsent(orderId, "PROCESSED");
                        Thread.sleep(1);
                        if (existing == null) {
                            processed.incrementAndGet();
                        } else {
                            duplicates.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await();
            start.countDown(); // disparo simultáneo
            done.await(10, TimeUnit.SECONDS);
        }

        return new ConcurrencyResult("ConcurrentHashMap (thread-safe)",
                threads, processed.get(), duplicates.get(), 0);
    }

    public record ConcurrencyResult(
            String scenario,
            int totalRequests,
            int processed,
            int duplicatesBlocked,
            int errors
    ) {
        @Override
        public String toString() {
            return String.format(
                "[%s] Threads: %d | Procesados: %d | Bloqueados: %d | Errores: %d",
                scenario, totalRequests, processed, duplicatesBlocked, errors
            );
        }
    }
}

