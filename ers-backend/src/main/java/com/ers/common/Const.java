package com.ers.common;

/**
 * 业务枚举字面量（数据库以字符串存储，界面中文直出，IA §6.0 S1 / B6）。
 */
public final class Const {

    private Const() {
    }

    /** 资产状态 */
    public static final class AssetStatus {
        public static final String IN_STOCK = "IN_STOCK";
        public static final String BORROWED = "BORROWED";
        public static final String DISABLED = "DISABLED";

        private AssetStatus() {
        }
    }

    /** 资产流转动作 */
    public static final class AssetAction {
        public static final String BORROW = "BORROW";
        public static final String RETURN = "RETURN";
        public static final String TRANSFER = "TRANSFER";

        private AssetAction() {
        }
    }

    /** 转交申请状态 */
    public static final class TransferStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String CANCELLED = "CANCELLED";

        private TransferStatus() {
        }
    }

    /** 场地预约状态 */
    public static final class ReservationStatus {
        public static final String BOOKED = "BOOKED";
        public static final String CANCELLED = "CANCELLED";
        public static final String USED = "USED";
        public static final String NO_SHOW = "NO_SHOW";

        private ReservationStatus() {
        }
    }

    /** 采购申请状态 */
    public static final class ProcurementStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";

        private ProcurementStatus() {
        }
    }

    /** 投票活动状态 */
    public static final class VoteStatus {
        public static final String DRAFT = "DRAFT";
        public static final String PUBLISHED = "PUBLISHED";
        public static final String CLOSED = "CLOSED";

        private VoteStatus() {
        }
    }

    /** 信誉分变动原因 */
    public static final class CreditReason {
        public static final String OVERDUE = "OVERDUE";
        public static final String NO_SHOW = "NO_SHOW";
        public static final String ADMIN_ADJUST = "ADMIN_ADJUST";

        private CreditReason() {
        }
    }

    /** 信誉分业务类型 */
    public static final class CreditRefType {
        public static final String ASSET = "ASSET";
        public static final String RESERVATION = "RESERVATION";

        private CreditRefType() {
        }
    }

    /** 系统配置键（PRD 4.3 / 4.4，IA S5 策略层） */
    public static final class ConfigKey {
        public static final String CREDIT_THRESHOLD = "credit.threshold";
        public static final String HIGH_VALUE_THRESHOLD = "high_value.threshold";
        public static final String BORROW_DAYS = "borrow.days";
        public static final String RENEW_MAX_TIMES = "renew.max.times";

        private ConfigKey() {
        }
    }

    /** 角色 */
    public static final class Role {
        public static final String USER = "USER";
        public static final String ADMIN = "ADMIN";

        private Role() {
        }
    }
}
