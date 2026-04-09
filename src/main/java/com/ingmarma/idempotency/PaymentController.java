package com.ingmarma.idempotency;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Endpoint SIN idempotencia — vulnerable a duplicados
    @PostMapping("/unsafe")
    public String processUnsafe(@RequestParam String orderId,
                                 @RequestParam double amount) {
        return paymentService.processPaymentUnsafe(orderId, amount);
    }

    // Endpoint CON idempotencia — seguro ante reintentos
    @PostMapping("/safe")
    public String processSafe(@RequestParam String orderId,
                               @RequestParam double amount) {
        return paymentService.processPaymentSafe(orderId, amount);
    }

    // Ver estadísticas
    @GetMapping("/stats")
    public String getStats() {
        return paymentService.getStats();
    }

    // Reset para nueva prueba
    @PostMapping("/reset")
    public String reset() {
        paymentService.reset();
        return "Stats reseteadas";
    }
}
