package com.ers.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

import com.ers.asset.dto.AssetVO;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.entity.TransferRequest;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.AssetRecordMapper;
import com.ers.asset.mapper.TransferRequestMapper;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.config.ConfigService;
import com.ers.credit.CreditService;
import com.ers.user.mapper.SysUserMapper;

/**
 * AssetService 单元测试（Mockito，无 Spring 上下文）：借出/续借/归还的服务层守卫。
 * 信誉阈值与高价值判定走 PRD 4.3，转交联动走 PRD 4.2(3)，端到端见 MockMvc 冒烟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssetService 单元测试")
class AssetServiceUnitTest extends com.ers.UnitTestSupport {

    private static final long ASSET_ID = 7L;
    private static final long HOLDER = 2L;
    private static final long OTHER = 4L;
    private static final long ADMIN = 1L;

    @Mock
    private AssetMapper assetMapper;
    @Mock
    private AssetRecordMapper recordMapper;
    @Mock
    private TransferRequestMapper transferMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private ConfigService configService;
    @Mock
    private CreditService creditService;

    @InjectMocks
    private AssetService service;

    @BeforeEach
    void setUp() {
        lenient().when(configService.borrowDays()).thenReturn(3);
        lenient().when(configService.renewMaxTimes()).thenReturn(3);
        lenient().when(configService.highValueThreshold()).thenReturn(1000);
        lenient().when(configService.creditThreshold()).thenReturn(60);
        lenient().when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        lenient().when(transferMapper.selectOne(any())).thenReturn(null);
    }

    // ------------------------------------------------------------------ 借出

    @Test
    @DisplayName("借出：停用资产 → 409")
    void borrowRejectsDisabled() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.DISABLED, null, 100, 0));

        assertBiz(() -> service.borrow(ASSET_ID, OTHER), ApiCode.CONFLICT, "资产已停用，无法借出");
    }

    @Test
    @DisplayName("借出：已被借出 → 409")
    void borrowRejectsAlreadyBorrowed() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER, 100, 0));

        assertBiz(() -> service.borrow(ASSET_ID, OTHER), ApiCode.CONFLICT, "资产当前已被借出，无法重复借出");
    }

    @Test
    @DisplayName("借出：高价值 + 信誉分低于阈值 → 40301，且不改资产状态")
    void borrowBlocksLowCreditHighValue() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.IN_STOCK, null, 1599, 0));
        when(creditService.scoreOf(OTHER)).thenReturn(55);

        assertBiz(() -> service.borrow(ASSET_ID, OTHER), ApiCode.CREDIT_INSUFFICIENT,
                "信誉分不足（当前 55 分，低于阈值 60 分），暂不能借出高价值资产");
        verify(assetMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("借出：低价值资产不查信誉分，成功后写 BORROW 记录并算归还日 +3 天")
    void borrowLowValueWritesRecord() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.IN_STOCK, null, 99, 0));
        Asset rented = asset(Const.AssetStatus.BORROWED, OTHER, 99, 0);
        rented.setDueDate(LocalDate.now().plusDays(3));
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.IN_STOCK, null, 99, 0), rented);
        when(assetMapper.update(any(), any())).thenReturn(1);

        AssetVO vo = service.borrow(ASSET_ID, OTHER);

        verify(creditService, never()).scoreOf(any());
        assertThat(vo.status()).isEqualTo(Const.AssetStatus.BORROWED);
        assertThat(vo.dueDate()).isEqualTo(LocalDate.now().plusDays(3));

        ArgumentCaptor<AssetRecord> captor = ArgumentCaptor.forClass(AssetRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(Const.AssetAction.BORROW);
        assertThat(captor.getValue().getOperatorId()).isEqualTo(OTHER);
        assertThat(captor.getValue().getRemark()).startsWith("借出，应归还日 ");
    }

    @Test
    @DisplayName("借出：条件更新 0 行 → 409 冲突")
    void borrowRejectsConcurrentChange() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.IN_STOCK, null, 99, 0));
        when(assetMapper.update(any(), any())).thenReturn(0);

        assertBiz(() -> service.borrow(ASSET_ID, OTHER), ApiCode.CONFLICT, "资产状态已变化，请刷新后重试");
    }

    // ------------------------------------------------------------------ 续借

    @Test
    @DisplayName("续借：非持有者 → 403")
    void renewRejectsNonHolder() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER, 99, 0));

        assertBiz(() -> service.renew(ASSET_ID, OTHER), ApiCode.FORBIDDEN, "只有当前持有者可以续借");
    }

    @Test
    @DisplayName("续借：已达上限 3 次 → 422")
    void renewRejectsAtMaxTimes() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER, 99, 3));

        assertBiz(() -> service.renew(ASSET_ID, HOLDER), ApiCode.UNPROCESSABLE, "续借次数已达上限（3 次）");
    }

    @Test
    @DisplayName("续借：存在待审批转交 → 422（PRD 4.2(3) 互斥）")
    void renewRejectsPendingTransfer() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER, 99, 0));
        TransferRequest t = new TransferRequest();
        t.setAssetId(ASSET_ID);
        t.setStatus(Const.TransferStatus.PENDING);
        when(transferMapper.selectOne(any())).thenReturn(t);

        assertBiz(() -> service.renew(ASSET_ID, HOLDER), ApiCode.UNPROCESSABLE,
                "该资产存在待审批的转交申请，暂不可续借");
        verify(assetMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("续借：成功一次 dueDate +1 天、renewCount +1")
    void renewAddsOneDay() {
        Asset before = asset(Const.AssetStatus.BORROWED, HOLDER, 99, 0);
        before.setDueDate(LocalDate.of(2026, 11, 20));
        Asset after = asset(Const.AssetStatus.BORROWED, HOLDER, 99, 1);
        after.setDueDate(LocalDate.of(2026, 11, 21));
        AtomicInteger calls = new AtomicInteger();
        when(assetMapper.selectById(ASSET_ID)).thenAnswer(inv -> calls.getAndIncrement() == 0 ? before : after);
        when(assetMapper.update(any(), any())).thenReturn(1);

        AssetVO vo = service.renew(ASSET_ID, HOLDER);

        assertThat(vo.dueDate()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(vo.renewCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ 归还

    @Test
    @DisplayName("归还：非持有者且非管理员 → 403")
    void returnRejectsNonHolder() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(Const.AssetStatus.BORROWED, HOLDER, 99, 0));

        assertBiz(() -> service.returnAsset(ASSET_ID, OTHER, false), ApiCode.FORBIDDEN,
                "只有当前持有者可以归还该资产");
    }

    @Test
    @DisplayName("归还：管理员可代还，写 RETURN 记录，并取消该资产待审转交")
    void returnByAdminWritesRecordAndCancelsTransfers() {
        Asset before = asset(Const.AssetStatus.BORROWED, HOLDER, 99, 0);
        before.setDueDate(LocalDate.now().plusDays(1));
        Asset after = asset(Const.AssetStatus.IN_STOCK, null, 99, 0);
        AtomicInteger calls = new AtomicInteger();
        when(assetMapper.selectById(ASSET_ID)).thenAnswer(inv -> calls.getAndIncrement() == 0 ? before : after);
        when(assetMapper.update(any(), any())).thenReturn(1);

        AssetVO vo = service.returnAsset(ASSET_ID, ADMIN, true);

        assertThat(vo.status()).isEqualTo(Const.AssetStatus.IN_STOCK);
        assertThat(vo.holderId()).isNull();
        ArgumentCaptor<AssetRecord> captor = ArgumentCaptor.forClass(AssetRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(Const.AssetAction.RETURN);
        assertThat(captor.getValue().getOperatorId()).isEqualTo(ADMIN);
        assertThat(captor.getValue().getTargetUserId()).isEqualTo(HOLDER);
        assertThat(captor.getValue().getRemark()).isEqualTo("归还");
        verify(transferMapper).update(any(), any());
    }

    @Test
    @DisplayName("归还：逾期归还的备注为「逾期归还」")
    void returnOverdueRemark() {
        Asset before = asset(Const.AssetStatus.BORROWED, HOLDER, 99, 0);
        before.setDueDate(LocalDate.now().minusDays(2));
        Asset after = asset(Const.AssetStatus.IN_STOCK, null, 99, 0);
        AtomicInteger calls = new AtomicInteger();
        when(assetMapper.selectById(ASSET_ID)).thenAnswer(inv -> calls.getAndIncrement() == 0 ? before : after);
        when(assetMapper.update(any(), any())).thenReturn(1);

        service.returnAsset(ASSET_ID, HOLDER, false);

        ArgumentCaptor<AssetRecord> captor = ArgumentCaptor.forClass(AssetRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getRemark()).isEqualTo("逾期归还");
    }

    // ------------------------------------------------------------------ helpers

    private Asset asset(String status, Long holderId, int value, int renewCount) {
        Asset a = new Asset();
        a.setId(ASSET_ID);
        a.setCode("A-0211");
        a.setName("测试资产");
        a.setCategory("电子设备");
        a.setStatus(status);
        a.setHolderId(holderId);
        a.setValue(BigDecimal.valueOf(value));
        a.setRenewCount(renewCount);
        return a;
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
