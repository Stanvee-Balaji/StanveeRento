



package com.example.demo.service;

import com.example.demo.dto.PaymentDtos.DuplicateOrderException;
import com.example.demo.dto.PaymentDtos.PaymentInitResponse;
import com.example.demo.dto.PaymentDtos.PaymentRequest;
import com.example.demo.dto.SubscriptionPurchaseDto.InitiateResponse;
import com.example.demo.dto.SubscriptionPurchaseDto.PurchaseRequest;
import com.example.demo.dto.SubscriptionPurchaseDto.UserPurchaseResponse;
import com.example.demo.entity.SubscriptionPlanEntity;
import com.example.demo.entity.UserSubscriptionEntity;
import com.example.demo.entity.UserSubscriptionEntity.Status;
import com.example.demo.repository.SubscriptionPlanRepository;
import com.example.demo.repository.UserSubscriptionRepository;
import com.example.demo.util.LocalINDTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User-facing purchase flow. Every method is scoped to the calling username: a user can only ever
 * start, read or poll their OWN purchases. The price is read from the plan in the DB, never from the request.
 */


@Service
public class UserSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(UserSubscriptionService.class);

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository repo;
    private final SubscriptionPurchaseService purchaseService;
    private final AirpayService airpayService;

    public UserSubscriptionService(SubscriptionPlanRepository planRepository,
                                   UserSubscriptionRepository repo,
                                   SubscriptionPurchaseService purchaseService,
                                   AirpayService airpayService) {
        this.planRepository = planRepository;
        this.repo = repo;
        this.purchaseService = purchaseService;
        this.airpayService = airpayService;
    }

    // NOT @Transactional on purpose: we must not hold a DB transaction open during the HTTP call to Airpay.
    public InitiateResponse purchase(String username, PurchaseRequest req, String clientIp) {

        // same visibility rule as the public plan APIs: ACTIVE + active flag + not deleted
        SubscriptionPlanEntity plan = planRepository.findByIdAndDeletedAtIsNull(req.getPlanId())
                .filter(pl -> "ACTIVE".equals(pl.getStatus()) && pl.isActive())
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Subscription plan not found: " + req.getPlanId()));

        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SuperAdminService.BadRequestException("This plan has no payable price.");
        }

        LocalDateTime startedAt = LocalINDTime.now();
        UserSubscriptionEntity p = purchaseService.createOrReusePending(username, plan);
        boolean isNewRow = !p.getCreatedAt().isBefore(startedAt);

        PaymentRequest pay = new PaymentRequest(
                p.getOrderId(),
                nvl(req.getBuyerEmail()),
                nvl(req.getBuyerPhone()),
                nvl(req.getBuyerFirstName()),
                nvl(req.getBuyerLastName()),
                nvl(req.getBuyerAddress()),
//                p.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),   // <- from DB snapshot
                p.getPrice()
                .add(plan.isDepositRequired() && plan.getDepositAmount() != null
                        ? plan.getDepositAmount() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP).toPlainString(),   // price + deposit
                nvl(req.getBuyerCity()),
                nvl(req.getBuyerState()),
                nvl(req.getBuyerPinCode()),
                nvl(req.getBuyerCountry()),
                "subscription",
                null);

        try {
            PaymentInitResponse init = airpayService.initiatePayment(pay, clientIp);
            return new InitiateResponse(purchaseService.toUserResponse(p), init);
        } catch (DuplicateOrderException e) {
            throw new SuperAdminService.BadRequestException(
                    "This payment is already completed. Please refresh your subscription status.");
        } catch (Exception e) {
            log.error("orderId={} payment initiation failed: {}", p.getOrderId(), e.getMessage(), e);
            if (isNewRow) purchaseService.markInitFailed(p.getOrderId(), e.getMessage());
            throw new RuntimeException("Could not start the payment. Please try again.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<UserPurchaseResponse> myPurchases(String username) {
        return repo.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(purchaseService::toUserResponse).toList();
    }

    /** The subscription that is running right now (null if none). */
    @Transactional(readOnly = true)
    public UserPurchaseResponse myCurrent(String username) {
        LocalDateTime now = LocalINDTime.now();
        return repo.findFirstByUsernameAndStatusAndStartDateLessThanEqualAndEndDateAfterOrderByEndDateDesc(
                        username, Status.ACTIVE, now, now)
                .map(purchaseService::toUserResponse).orElse(null);
    }

    /** Polling endpoint after the Airpay iframe closes. 404 for other people's orders (no info leak). */
    @Transactional(readOnly = true)
    public UserPurchaseResponse myPurchase(String username, String orderId) {
        return repo.findByOrderId(orderId)
                .filter(p -> p.getUsername().equals(username))
                .map(purchaseService::toUserResponse)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Subscription purchase not found: " + orderId));
    }

    /** null-safe + trimmed, so the checksum is always built on exactly what the frontend will post. */
    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}