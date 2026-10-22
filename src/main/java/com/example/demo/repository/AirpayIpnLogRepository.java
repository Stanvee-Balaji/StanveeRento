package com.example.demo.repository;

import com.example.demo.entity.AirpayIpnLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirpayIpnLogRepository extends JpaRepository<AirpayIpnLog, Long> {
    List<AirpayIpnLog> findByOrderIdOrderByReceivedAtAsc(String orderId);
}
