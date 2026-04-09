package com.ingmarma.idempotency;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentConcurrencyService concurrencyService;

    public PaymentController(PaymentService paymentService,
                              PaymentConcurrencyService concurrencyService) {
        this.paymentService = paymentService;
        this.concurrencyService = concurrencyService;
    }

    // Endpoint SIN idempotencia
    @PostMapping("/unsafe")
    public String processUnsafe(@RequestParam String orderId,
                                 @RequestParam double amount) {
        return paymentService.processPaymentUnsafe(orderId, amount);
    }

    // Endpoint CON idempotencia
    @PostMapping("/safe")
    public String processSafe(@RequestParam String orderId,
                               @RequestParam double amount) {
        return paymentService.processPaymentSafe(orderId, amount);
    }

    // Estadísticas
    @GetMapping("/stats")
    public String getStats() {
        return paymentService.getStats();
    }

    // Reset
    @PostMapping("/reset")
    public String reset() {
        paymentService.reset();
        return "Stats reseteadas";
    }

    // HashMap vs ConcurrentHashMap con N threads concurrentes
    @GetMapping("/concurrency-test")
    public String concurrencyTest(@RequestParam(defaultValue = "100") int threads)
            throws InterruptedException {
        String orderId = "ORDER-CONCURRENT-001";

        PaymentConcurrencyService.ConcurrencyResult hashMapResult =
                concurrencyService.testWithHashMap(threads, orderId);

        PaymentConcurrencyService.ConcurrencyResult concurrentResult =
                concurrencyService.testWithConcurrentHashMap(threads, orderId);

        return String.format(
            "=== %d THREADS CONCURRENTES ===%n%s%n%s",
            threads, hashMapResult, concurrentResult
        );
    }
}