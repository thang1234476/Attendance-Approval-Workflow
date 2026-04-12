package com.example.attendance.entity;

public enum CheckoutStatus {
    ON_TIME,    // Đúng giờ (checkout trong khoảng cho phép)
    EARLY,      // Về sớm (trước giờ tan làm - 5 phút)
    LATE        // Về muộn (sau giờ tan làm + 20 phút)
}