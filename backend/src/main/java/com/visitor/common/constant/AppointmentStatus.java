package com.visitor.common.constant;

/**
 * 预约状态常量，消除魔法值
 */
public final class AppointmentStatus {
    public static final String PENDING   = "pending";
    public static final String APPROVED  = "approved";
    public static final String REJECTED  = "rejected";
    public static final String CANCELLED = "cancelled";
    public static final String CONFIRMED = "confirmed";

    /** 有效的终态/可核验状态 */
    public static final java.util.List<String> VERIFIABLE =
            java.util.List.of(APPROVED, CONFIRMED);

    private AppointmentStatus() {}
}
