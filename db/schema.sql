-- ============================================================================
-- 企业资源调度与协同治理平台 (ERS) — 数据库结构 v1.0
-- 目标库：MySQL 8.0+  /  InnoDB  /  utf8mb4  /  utf8mb4_0900_ai_ci
-- 执行顺序：01_schema.sql（本文件） → 02_data.sql
--
-- 约定
--   * 主键统一 BIGINT UNSIGNED AUTO_INCREMENT
--   * 时间统一 DATETIME（应用时区 Asia/Shanghai）；仅"日期"语义用 DATE
--   * 状态一律以英文枚举字符串存储，界面中文直出（IA §6.0 S1）
--   * 逻辑删除仅用于"采购申请删除已驳回"（deleted_at）；资产删除受业务约束
--   * 演示账号密码不在本文件出现：02_data.sql 写入占位符 '__DEMO__'，
--     由后端 DataInitializer 在首次启动时以 BCrypt 编码写入（见 DB 设计文档 §3.4）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS vote_record;
DROP TABLE IF EXISTS vote_option;
DROP TABLE IF EXISTS vote_topic;
DROP TABLE IF EXISTS procurement;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS credit_log;
DROP TABLE IF EXISTS transfer_request;
DROP TABLE IF EXISTS asset_record;
DROP TABLE IF EXISTS asset;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_user;

-- ---------------------------------------------------------------------------
-- 1. sys_user 用户与角色
-- ---------------------------------------------------------------------------
CREATE TABLE sys_user (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(32)  NOT NULL                COMMENT '登录账号，唯一',
  password      VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密文；种子值为 __DEMO__',
  real_name     VARCHAR(32)  NOT NULL                COMMENT '真实姓名，界面展示',
  role          VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
  credit_score  INT          NOT NULL DEFAULT 100    COMMENT '信誉分，下限 0（PRD 4.3）',
  status        TINYINT      NOT NULL DEFAULT 1      COMMENT '1=启用 0=停用（停用后不可登录）',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_role_status (role, status),
  CONSTRAINT ck_user_role   CHECK (role IN ('USER', 'ADMIN')),
  CONSTRAINT ck_user_credit CHECK (credit_score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与角色（管理员由种子写入，注册仅产生 USER）';

-- ---------------------------------------------------------------------------
-- 2. asset 资产台账（单一资产实体，IA B5）
-- ---------------------------------------------------------------------------
CREATE TABLE asset (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code         VARCHAR(32)   NOT NULL                COMMENT '资产编号，唯一，如 A-0201',
  name         VARCHAR(64)   NOT NULL                COMMENT '资产名称',
  category     VARCHAR(32)   NOT NULL                COMMENT '分类：电子设备 / 办公家具 / 工具 / 耗材',
  value        DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '购入价值（元），高价值判定依据（PRD 4.3）',
  status       VARCHAR(16)   NOT NULL DEFAULT 'IN_STOCK' COMMENT 'IN_STOCK 在库 / BORROWED 借出 / DISABLED 停用',
  holder_id    BIGINT UNSIGNED NULL                  COMMENT '当前持有者；在库/停用时为 NULL',
  borrow_at    DATETIME      NULL                    COMMENT '本次借出时间',
  due_date     DATE          NULL                    COMMENT '应归还日期 = 借出日 + 3 天；转交不变',
  renew_count  TINYINT       NOT NULL DEFAULT 0      COMMENT '本次借出已续借次数，上限 3（PRD 4.2）',
  description  VARCHAR(255)  NULL                    COMMENT '描述/备注',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_asset_code (code),
  KEY idx_asset_status (status),
  KEY idx_asset_holder (holder_id),
  KEY idx_asset_category (category),
  CONSTRAINT fk_asset_holder FOREIGN KEY (holder_id) REFERENCES sys_user (id),
  CONSTRAINT ck_asset_status CHECK (status IN ('IN_STOCK', 'BORROWED', 'DISABLED')),
  CONSTRAINT ck_asset_value  CHECK (value >= 0),
  CONSTRAINT ck_asset_renew  CHECK (renew_count BETWEEN 0 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产台账';

-- ---------------------------------------------------------------------------
-- 3. asset_record 资产流转记录（借出/归还/转交 统一审计事件流，IA A3）
-- ---------------------------------------------------------------------------
CREATE TABLE asset_record (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  asset_id       BIGINT UNSIGNED NOT NULL             COMMENT '资产',
  action         VARCHAR(16)     NOT NULL             COMMENT 'BORROW 借出 / RETURN 归还 / TRANSFER 转交',
  operator_id    BIGINT UNSIGNED NOT NULL             COMMENT '操作人（借用人/归还人/转交审批人）',
  target_user_id BIGINT UNSIGNED NULL                 COMMENT '目标用户：转交的接收方；借出时可空',
  from_status    VARCHAR(16)     NULL                 COMMENT '变更前状态',
  to_status      VARCHAR(16)     NULL                 COMMENT '变更后状态',
  remark         VARCHAR(255)    NULL                 COMMENT '备注（如"转交至李四，应归还日不变"）',
  happened_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间（时间线排序依据）',
  PRIMARY KEY (id),
  KEY idx_record_asset_time (asset_id, happened_at DESC),
  KEY idx_record_operator (operator_id),
  CONSTRAINT fk_record_asset    FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id),
  CONSTRAINT ck_record_action   CHECK (action IN ('BORROW', 'RETURN', 'TRANSFER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产流转记录（只增不改，审计源）';

-- ---------------------------------------------------------------------------
-- 4. transfer_request 转交申请（B 发起 → 当前持有者 A 审批，PRD 4.2）
-- ---------------------------------------------------------------------------
CREATE TABLE transfer_request (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  asset_id      BIGINT UNSIGNED NOT NULL              COMMENT '资产',
  from_user_id  BIGINT UNSIGNED NOT NULL              COMMENT '当前持有者（审批人 A）',
  to_user_id    BIGINT UNSIGNED NOT NULL              COMMENT '拟接收人（发起人 B）',
  status        VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待审批 / APPROVED 已同意 / REJECTED 已拒绝 / CANCELLED 已取消',
  apply_reason  VARCHAR(200)    NULL                  COMMENT '发起说明',
  reject_reason VARCHAR(200)    NULL                  COMMENT '拒绝理由（拒绝时必填）',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  decided_at    DATETIME        NULL                  COMMENT '审批时间',
  -- 同一资产同时只允许一条待审批（MySQL 无部分索引，用生成列 + 唯一键实现）
  pending_asset_id BIGINT UNSIGNED
      GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN asset_id END) STORED COMMENT '待审批资产的生成列',
  PRIMARY KEY (id),
  UNIQUE KEY uk_transfer_pending (pending_asset_id),
  KEY idx_transfer_from (from_user_id, status),
  KEY idx_transfer_to (to_user_id, status),
  CONSTRAINT fk_transfer_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
  CONSTRAINT fk_transfer_from  FOREIGN KEY (from_user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_transfer_to    FOREIGN KEY (to_user_id) REFERENCES sys_user (id),
  CONSTRAINT ck_transfer_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
  CONSTRAINT ck_transfer_self   CHECK (from_user_id <> to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产转交申请';

-- ---------------------------------------------------------------------------
-- 5. credit_log 信誉分流水（逾期每日 -5 去重、爽约 -5 一次性）
-- ---------------------------------------------------------------------------
CREATE TABLE credit_log (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT UNSIGNED NOT NULL              COMMENT '被扣分用户',
  delta      SMALLINT        NOT NULL              COMMENT '变动值，通常为 -5（负数表示扣分）',
  reason     VARCHAR(16)     NOT NULL              COMMENT 'OVERDUE 逾期 / NO_SHOW 爽约 / ADMIN_ADJUST 人工调整',
  ref_type   VARCHAR(16)     NOT NULL              COMMENT '业务类型：ASSET / RESERVATION',
  ref_id     BIGINT UNSIGNED NULL                  COMMENT '业务主键（资产或预约）',
  biz_date   DATE            NOT NULL              COMMENT '业务发生日，用于"当日去重"',
  remark     VARCHAR(128)    NULL                  COMMENT '说明',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_daily (user_id, reason, ref_type, ref_id, biz_date),
  KEY idx_credit_user_time (user_id, created_at DESC),
  CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT ck_credit_reason CHECK (reason IN ('OVERDUE', 'NO_SHOW', 'ADMIN_ADJUST'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信誉分流水（幂等去重靠唯一键）';

-- ---------------------------------------------------------------------------
-- 6. room 会议室 / 场地
-- ---------------------------------------------------------------------------
CREATE TABLE room (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  name       VARCHAR(32)     NOT NULL                COMMENT '场地名称',
  location   VARCHAR(64)     NULL                    COMMENT '位置（楼层/楼栋）',
  capacity   INT             NOT NULL DEFAULT 1      COMMENT '可容纳人数',
  status     TINYINT         NOT NULL DEFAULT 1      COMMENT '1=可用 0=停用（停用不影响已产生的预约）',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议室/场地';

-- ---------------------------------------------------------------------------
-- 7. reservation 场地预约（30 分钟切片，唯一索引防重）
-- ---------------------------------------------------------------------------
CREATE TABLE reservation (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  room_id      BIGINT UNSIGNED NOT NULL              COMMENT '场地',
  user_id      BIGINT UNSIGNED NOT NULL              COMMENT '预约人',
  reserve_date DATE            NOT NULL              COMMENT '预约日期',
  time_slot    TINYINT         NOT NULL              COMMENT '切片序号：0=09:00-09:30 … 17=17:30-18:00',
  status       VARCHAR(16)     NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED 已预约 / CANCELLED 已取消 / USED 已使用 / NO_SHOW 爽约',
  cancelled_at DATETIME        NULL                  COMMENT '取消时间',
  checked_in_at DATETIME       NULL                  COMMENT '标记已使用时间',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 仅"进行中"的预约占用切片：取消后置 NULL，允许重新预约同一格
  active_slot  TINYINT
      GENERATED ALWAYS AS (CASE WHEN status = 'BOOKED' THEN time_slot END) STORED COMMENT '占用切片生成列',
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_slot (room_id, reserve_date, active_slot),
  KEY idx_reservation_user (user_id, reserve_date),
  KEY idx_reservation_room_date (room_id, reserve_date),
  CONSTRAINT fk_reservation_room FOREIGN KEY (room_id) REFERENCES room (id),
  CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT ck_reservation_slot   CHECK (time_slot BETWEEN 0 AND 17),
  CONSTRAINT ck_reservation_status CHECK (status IN ('BOOKED', 'CANCELLED', 'USED', 'NO_SHOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='场地预约（30 分钟切片）';

-- ---------------------------------------------------------------------------
-- 8. procurement 采购申请（二态审批，本期不自动入库，IA B2）
-- ---------------------------------------------------------------------------
CREATE TABLE procurement (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        VARCHAR(64)     NOT NULL              COMMENT '申请名称',
  reason       VARCHAR(200)    NOT NULL              COMMENT '申请理由（≤200 字）',
  status       VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待审批 / APPROVED 已同意入库 / REJECTED 已驳回',
  applicant_id BIGINT UNSIGNED NOT NULL              COMMENT '申请人',
  approver_id  BIGINT UNSIGNED NULL                  COMMENT '审批管理员',
  approve_note VARCHAR(200)    NULL                  COMMENT '审批备注；驳回时必填',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  decided_at   DATETIME        NULL                  COMMENT '审批时间',
  deleted_at   DATETIME        NULL                  COMMENT '逻辑删除（仅已驳回可删）',
  PRIMARY KEY (id),
  KEY idx_proc_status (status, deleted_at),
  KEY idx_proc_applicant (applicant_id),
  CONSTRAINT fk_proc_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user (id),
  CONSTRAINT fk_proc_approver  FOREIGN KEY (approver_id) REFERENCES sys_user (id),
  CONSTRAINT ck_proc_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购申请';

-- ---------------------------------------------------------------------------
-- 9. vote_topic 投票活动
-- ---------------------------------------------------------------------------
CREATE TABLE vote_topic (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(64)     NOT NULL               COMMENT '活动标题',
  description VARCHAR(500)    NULL                   COMMENT '说明',
  deadline    DATETIME        NOT NULL               COMMENT '截止时间（到点自动停收）',
  status      VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿 / PUBLISHED 进行中 / CLOSED 已关闭',
  created_by  BIGINT UNSIGNED NOT NULL               COMMENT '发布管理员',
  published_at DATETIME       NULL                   COMMENT '发布时间',
  closed_at   DATETIME        NULL                   COMMENT '提前关闭时间',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_vote_status_deadline (status, deadline),
  CONSTRAINT fk_vote_creator FOREIGN KEY (created_by) REFERENCES sys_user (id),
  CONSTRAINT ck_vote_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投票活动';

-- ---------------------------------------------------------------------------
-- 10. vote_option 投票选项
-- ---------------------------------------------------------------------------
CREATE TABLE vote_option (
  id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  topic_id BIGINT UNSIGNED NOT NULL                COMMENT '所属活动',
  label    VARCHAR(64)     NOT NULL                COMMENT '选项文案',
  sort_no  INT             NOT NULL DEFAULT 0      COMMENT '排序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_option_topic_label (topic_id, label),
  CONSTRAINT fk_option_topic FOREIGN KEY (topic_id) REFERENCES vote_topic (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投票选项';

-- ---------------------------------------------------------------------------
-- 11. vote_record 投票记录（每人每活动一票，不可改）
-- ---------------------------------------------------------------------------
CREATE TABLE vote_record (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  topic_id   BIGINT UNSIGNED NOT NULL              COMMENT '活动',
  option_id  BIGINT UNSIGNED NOT NULL              COMMENT '所选选项',
  user_id    BIGINT UNSIGNED NOT NULL              COMMENT '投票人',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_vote_user (topic_id, user_id),
  KEY idx_vote_option (option_id),
  CONSTRAINT fk_record_topic  FOREIGN KEY (topic_id) REFERENCES vote_topic (id) ON DELETE CASCADE,
  CONSTRAINT fk_record_option FOREIGN KEY (option_id) REFERENCES vote_option (id),
  CONSTRAINT fk_record_user   FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投票记录';

-- ---------------------------------------------------------------------------
-- 12. sys_config 系统配置（阈值等策略层，IA B4 / S5）
-- ---------------------------------------------------------------------------
CREATE TABLE sys_config (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key   VARCHAR(32)     NOT NULL              COMMENT '配置键',
  config_value VARCHAR(64)     NOT NULL              COMMENT '配置值（字符串存储，应用按类型解析）',
  description  VARCHAR(128)    NULL                  COMMENT '说明',
  updated_by   BIGINT UNSIGNED NULL                  COMMENT '最后修改管理员',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置（信誉阈值/高价值阈值/借期/续借上限）';

SET FOREIGN_KEY_CHECKS = 1;
