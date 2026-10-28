package com.example.demo.service;

import com.example.demo.dto.PaymentDtos.PaymentInitResponse;
import com.example.demo.dto.RentalOrderDto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * User-facing orchestrator. NOT @Transactional on purpose: the order (+ held units) is committed by
 * RentalOrderService.createBooking first, then Airpay is called without holding any DB lock.
 * If Airpay cannot be reached the order is marked PAYMENT_FAILED and its units are released (no retry endpoint).
 */
@Service
public class UserRentalService {

    private static final Logger log = LoggerFactory.getLogger(UserRentalService.class);

    private final RentalOrderService orders;
    private final RentalPaymentBridge payments;

    public UserRentalService(RentalOrderService orders, RentalPaymentBridge payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public ConfigResponse config() { return orders.getConfig(); }

    public AvailabilityResponse availability(UUID productId, String size, String colour, LocalDate from, LocalDate to) {
        return orders.availability(productId, size, colour, from, to);
    }

    public SummaryResponse summary(String username, OrderRequest req) { return orders.summary(username, req); }

    public BookResponse book(String username, OrderRequest req, String clientIp) {
        BookingResult r = orders.createBooking(username, req);
        if (!r.paymentRequired()) return new BookResponse(r.order(), false, null);
        try {
            PaymentInitResponse pay = payments.init(r.paymentRequest(), clientIp);
            return new BookResponse(r.order(), true, pay);
        } catch (Exception e) {
            log.error("orderId={} payment init failed", r.order().orderId(), e);
            orders.markInitFailed(r.order().orderId(), e.getMessage());
            throw new SuperAdminService.BadRequestException("Could not start the payment, so the booking was not placed. Please try again.");
        }
    }

    public PageResult<OrderResponse> my(String username, String status, int page, int size) {
        return orders.myList(username, status, page, size);
    }

    public OrderDetailResponse myOrder(String username, String orderId) { return orders.myOrder(username, orderId); }

    public OrderResponse cancel(String username, String orderId, String reason) { return orders.cancelMine(username, orderId, reason); }

    public OrderResponse requestReturn(String username, String orderId, String note) { return orders.requestReturn(username, orderId, note); }
}
