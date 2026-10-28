//package com.example.demo.service;
//
//import com.example.demo.dto.PaymentDtos.PaymentInitResponse;
//import com.example.demo.dto.PaymentDtos.PaymentRequest;
//import org.springframework.stereotype.Component;
//
///**
// * The ONE place that talks to Airpay for rental orders.
// *
// * ADAPT: call AirpayService exactly the way UserSubscriptionService.purchase(...) does (same method, same arguments).
// * Airpay creates the `transactions` row (order_id = the ORD... id) inside that call, like it does for subscriptions.
// */
//
//
//@Component
//public class RentalPaymentBridge {
//
//    private final AirpayService airpayService;
//
//    public RentalPaymentBridge(AirpayService airpayService) {
//        this.airpayService = airpayService;
//    }
//
//    public PaymentInitResponse init(PaymentRequest request, String clientIp) {
//        return airpayService.initiatePayment(request, clientIp); // ADAPT: name / signature as used by UserSubscriptionService
//    }
//}





package com.example.demo.service;

import com.example.demo.dto.PaymentDtos.PaymentInitResponse;
import com.example.demo.dto.PaymentDtos.PaymentRequest;
import org.springframework.stereotype.Component;

/**
 * The single place that communicates with Airpay for rental orders.
 *
 * Uses the existing AirpayService payment initialization logic,
 * consistent with the subscription purchase flow.
 */
@Component
public class RentalPaymentBridge {

    private final AirpayService airpayService;

    public RentalPaymentBridge(AirpayService airpayService) {
        this.airpayService = airpayService;
    }

    /**
     * Initializes an Airpay payment for a rental order.
     *
     * @param request payment initialization request
     * @param clientIp client IP address
     * @return Airpay payment initialization response
     * @throws Exception if payment initialization fails
     */
    public PaymentInitResponse init(
            PaymentRequest request,
            String clientIp
    ) throws Exception {

        return airpayService.initiatePayment(request, clientIp);
    }
}
