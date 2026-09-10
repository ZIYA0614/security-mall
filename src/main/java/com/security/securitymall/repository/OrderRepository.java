package com.security.securitymall.repository;

import com.security.securitymall.domain.Order;
import com.security.securitymall.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedDateDesc(User user);
    Order findByMerchantUid(String merchantUid);
}