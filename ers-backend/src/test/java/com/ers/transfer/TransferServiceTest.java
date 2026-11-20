package com.ers.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.entity.TransferRequest;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.AssetRecordMapper;
import com.ers.asset.mapper.TransferRequestMapper;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.transfer.dto.TransferCreateRequest;
import com.ers.transfer.dto.TransferVO;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * TransferService 单元测试（Mockito，无 Spring 上下文）：
 * 校验 D-01 语义在服务层的守卫与落库约定——B 申请、A 审批、换人不换期。
 * 端到端链路见 {@code D01TransferSemanticsTest}（MockMvc + MySQL）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransferService 单元测试")
class TransferServiceTest extends com.ers.UnitTestSupport {

    private static final long ASSET_ID = 3L;
    private static final long HOLDER = 3L;      // 李四
    private static final long APPLICANT = 2L;   // 张三
    private static final long OTHER = 4L;       // 王五

    @Mock
    private TransferRequestMapper transferMapper;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private AssetRecordMapper recordMapper;
    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private TransferService service;

    @BeforeEach
    void setUp() {
        lenient().when(userMapper.selectById(HOLDER)).thenReturn(user(HOLDER, "李四"));
        lenient().when(userMapper.selectById(APPLICANT)).thenReturn(user(APPLICANT, "张三"));
        lenient().when(userMapper.selectById(OTHER)).thenReturn(user(OTHER, "王五"));
    }

    // ------------------------------------------------------------------ 发起

    @Test
    @DisplayName("申请：资产不存在 → 404")
    void createAssetNotFound() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(null);

        assertBiz(() -> service.create(new TransferCreateRequest(ASSET_ID, "想接手"), APPLICANT),
                ApiCode.NOT_FOUND, "资产不存在");
        verify(transferMapper, never()).insert(any(TransferRequest.class));
    }

    @Test
    @DisplayName("申请：资产在库 → 422「该资产当前未借出」")
    void createRejectsInStockAsset() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.IN_STOCK, null));

        assertBiz(() -> service.create(new TransferCreateRequest(ASSET_ID, "想接手"), APPLICANT),
                ApiCode.UNPROCESSABLE, "该资产当前未借出，无需申请接手");
    }

    @Test
    @DisplayName("申请：持有者自申请 → 422「你正持有该资产」")
    void createRejectsHolderSelfApply() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER));

        assertBiz(() -> service.create(new TransferCreateRequest(ASSET_ID, "自己申请"), HOLDER),
                ApiCode.UNPROCESSABLE, "你正持有该资产，无需申请接手");
        verify(transferMapper, never()).insert(any(TransferRequest.class));
    }

    @Test
    @DisplayName("申请：已有待审 → 409")
    void createRejectsDuplicatePending() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER));
        when(transferMapper.selectCount(any())).thenReturn(1L);

        assertBiz(() -> service.create(new TransferCreateRequest(ASSET_ID, "重复"), APPLICANT),
                ApiCode.CONFLICT, "该资产已有待审批的转交申请");
    }

    @Test
    @DisplayName("申请：from=持有者 A、to=申请人 B，申请理由去空格后落库")
    void createPersistsApplicantAsToUser() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER));
        when(transferMapper.selectCount(any())).thenReturn(0L);

        TransferVO vo = service.create(new TransferCreateRequest(ASSET_ID, "  我下周要用  "), APPLICANT);

        ArgumentCaptor<TransferRequest> captor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferMapper).insert(captor.capture());
        TransferRequest saved = captor.getValue();
        assertThat(saved.getFromUserId()).as("from 必须是当前持有者（审批人）").isEqualTo(HOLDER);
        assertThat(saved.getToUserId()).as("to 必须是申请人（接手人）").isEqualTo(APPLICANT);
        assertThat(saved.getStatus()).isEqualTo(Const.TransferStatus.PENDING);
        assertThat(saved.getApplyReason()).isEqualTo("我下周要用");
        assertThat(vo.fromUserId()).isEqualTo(HOLDER);
        assertThat(vo.toUserId()).isEqualTo(APPLICANT);
        assertThat(vo.toUserName()).isEqualTo("张三");
    }

    // ------------------------------------------------------------------ 审批

    @Test
    @DisplayName("审批：非当前持有者 → 403")
    void approveRejectsNonHolder() {
        when(transferMapper.selectById(1L)).thenReturn(pending(1L, HOLDER, APPLICANT));

        assertBiz(() -> service.approve(1L, OTHER), ApiCode.FORBIDDEN, "只有当前持有者可以审批该转交申请");
        verify(assetMapper, never()).update(any(), any());
        verify(recordMapper, never()).insert(any(AssetRecord.class));
    }

    @Test
    @DisplayName("审批：单据已处理 → 409")
    void approveRejectsDecidedRequest() {
        TransferRequest t = pending(1L, HOLDER, APPLICANT);
        t.setStatus(Const.TransferStatus.APPROVED);
        when(transferMapper.selectById(1L)).thenReturn(t);

        assertBiz(() -> service.approve(1L, HOLDER), ApiCode.CONFLICT, "该转交申请已处理，请刷新后重试");
    }

    @Test
    @DisplayName("审批：条件更新 0 行（资产已被归还/再次易手）→ 409，且不写流转记录")
    void approveRejectsWhenAssetChanged() {
        when(transferMapper.selectById(1L)).thenReturn(pending(1L, HOLDER, APPLICANT));
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER));
        when(assetMapper.update(any(), any())).thenReturn(0);

        assertBiz(() -> service.approve(1L, HOLDER), ApiCode.CONFLICT, "资产状态已变化，请刷新后重试");
        verify(recordMapper, never()).insert(any(AssetRecord.class));
    }

    @Test
    @DisplayName("审批：同意后持有者换 B、dueDate 不动、写 1 条 TRANSFER 记录")
    void approveMovesHolderAndKeepsDueDate() {
        when(transferMapper.selectById(1L)).thenReturn(pending(1L, HOLDER, APPLICANT));
        Asset asset = asset(Const.AssetStatus.BORROWED, HOLDER);
        asset.setDueDate(java.time.LocalDate.of(2026, 11, 14));
        asset.setRenewCount(0);
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(assetMapper.update(any(), any())).thenReturn(1);

        TransferVO vo = service.approve(1L, HOLDER);

        assertThat(vo.status()).isEqualTo(Const.TransferStatus.APPROVED);
        assertThat(vo.decidedAt()).isNotNull();

        ArgumentCaptor<AssetRecord> captor = ArgumentCaptor.forClass(AssetRecord.class);
        verify(recordMapper).insert(captor.capture());
        AssetRecord record = captor.getValue();
        assertThat(record.getAction()).isEqualTo(Const.AssetAction.TRANSFER);
        assertThat(record.getOperatorId()).as("操作人是审批的持有者 A").isEqualTo(HOLDER);
        assertThat(record.getTargetUserId()).as("记录目标是接手人 B").isEqualTo(APPLICANT);
        assertThat(record.getFromStatus()).isEqualTo(Const.AssetStatus.BORROWED);
        assertThat(record.getToStatus()).isEqualTo(Const.AssetStatus.BORROWED);
        assertThat(record.getRemark()).isEqualTo("转交至张三，应归还日不变");
        assertThat(asset.getDueDate()).as("审批不触碰应归还日").isEqualTo(java.time.LocalDate.of(2026, 11, 14));
    }

    // ------------------------------------------------------------------ 驳回

    @Test
    @DisplayName("驳回：非当前持有者 → 403")
    void rejectRejectsNonHolder() {
        when(transferMapper.selectById(1L)).thenReturn(pending(1L, HOLDER, APPLICANT));

        assertBiz(() -> service.reject(1L, "不给", OTHER), ApiCode.FORBIDDEN, "只有当前持有者可以审批该转交申请");
    }

    @Test
    @DisplayName("驳回：状态置 REJECTED 并记录理由，持有者不变")
    void rejectMarksStatusAndReason() {
        when(transferMapper.selectById(1L)).thenReturn(pending(1L, HOLDER, APPLICANT));
        when(transferMapper.update(any(), any())).thenReturn(1);
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER));

        TransferVO vo = service.reject(1L, "  我自己还要用  ", HOLDER);

        assertThat(vo.status()).isEqualTo(Const.TransferStatus.REJECTED);
        assertThat(vo.rejectReason()).isEqualTo("我自己还要用");
        verify(assetMapper, never()).update(any(), any());
    }

    // ------------------------------------------------------------------ helpers

    private Asset asset(String status, Long holderId) {
        Asset a = new Asset();
        a.setId(ASSET_ID);
        a.setCode("A-0203");
        a.setName("投影仪");
        a.setCategory("电子设备");
        a.setStatus(status);
        a.setHolderId(holderId);
        return a;
    }

    private TransferRequest pending(long id, long from, long to) {
        TransferRequest t = new TransferRequest();
        t.setId(id);
        t.setAssetId(ASSET_ID);
        t.setFromUserId(from);
        t.setToUserId(to);
        t.setStatus(Const.TransferStatus.PENDING);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private SysUser user(long id, String name) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRealName(name);
        u.setStatus(1);
        return u;
    }

    private void assertBiz(Runnable action, int code, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException biz = (BizException) e;
                    assertThat(biz.getCode()).isEqualTo(code);
                    assertThat(biz.getMessage()).isEqualTo(message);
                });
    }
}
