package com.security.securitymall.domain;

public enum OrderStatus {
    PENDING,   // 결제 대기
    PAID,      // 결제 완료
    CANCELLED, // 결제 취소
    FAILED     // 결제 실패
}