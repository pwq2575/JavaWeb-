package com.ers.export;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ers.asset.entity.Asset;
import com.ers.asset.entity.AssetRecord;
import com.ers.asset.mapper.AssetMapper;
import com.ers.asset.mapper.AssetRecordMapper;
import com.ers.common.ApiCode;
import com.ers.common.BizException;
import com.ers.common.Const;
import com.ers.user.entity.SysUser;
import com.ers.user.mapper.SysUserMapper;

/**
 * Excel 导出（API 契约 §10，PRD 4.7）：EasyExcel 生成 xlsx，空数据返回只有表头的空表。
 */
@Service
public class ExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AssetMapper assetMapper;
    private final AssetRecordMapper recordMapper;
    private final SysUserMapper userMapper;

    public ExportService(AssetMapper assetMapper, AssetRecordMapper recordMapper, SysUserMapper userMapper) {
        this.assetMapper = assetMapper;
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
    }

    /** #52 资产台账（管理员全局）。 */
    public ResponseEntity<byte[]> exportAssets() {
        List<Asset> assets = assetMapper.selectList(Wrappers.<Asset>lambdaQuery().orderByAsc(Asset::getCode));
        Map<Long, String> names = userNames(assets);
        List<List<String>> head = List.of(
                List.of("资产编号"), List.of("名称"), List.of("类别"), List.of("价值(元)"),
                List.of("状态"), List.of("当前持有人"), List.of("借出时间"), List.of("应归还日"),
                List.of("续借次数"), List.of("备注"));
        List<List<Object>> rows = new ArrayList<>();
        for (Asset a : assets) {
            List<Object> row = new ArrayList<>();
            row.add(a.getCode());
            row.add(a.getName());
            row.add(a.getCategory());
            row.add(a.getValue() == null ? "" : a.getValue().toPlainString());
            row.add(statusText(a.getStatus()));
            row.add(a.getHolderId() == null ? "" : names.getOrDefault(a.getHolderId(), ""));
            row.add(a.getBorrowAt() == null ? "" : a.getBorrowAt().format(DATETIME));
            row.add(a.getDueDate() == null ? "" : a.getDueDate().format(DATE));
            row.add(a.getRenewCount() == null ? 0 : a.getRenewCount());
            row.add(a.getDescription() == null ? "" : a.getDescription());
            rows.add(row);
        }
        return xlsx("资产台账", head, rows, "assets.xlsx");
    }

    /** #53 个人借用记录（管理员可指定 userId）。 */
    public ResponseEntity<byte[]> exportBorrows(Long targetUserId, Long currentUserId, boolean admin) {
        Long userId = targetUserId == null ? currentUserId : targetUserId;
        if (!admin && !userId.equals(currentUserId)) {
            throw new BizException(ApiCode.FORBIDDEN, "只能导出自己的借用记录");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ApiCode.NOT_FOUND, "用户不存在");
        }
        Map<Long, Asset> assets = new HashMap<>();
        for (Asset a : assetMapper.selectList(null)) {
            assets.put(a.getId(), a);
        }
        List<AssetRecord> records = recordMapper.selectList(Wrappers.<AssetRecord>lambdaQuery()
                .and(w -> w.eq(AssetRecord::getOperatorId, userId).or().eq(AssetRecord::getTargetUserId, userId))
                .orderByDesc(AssetRecord::getHappenedAt));
        Map<Long, String> names = userNames(assetMapper.selectList(null));
        List<List<String>> head = List.of(
                List.of("时间"), List.of("资产编号"), List.of("资产名称"),
                List.of("动作"), List.of("操作人"), List.of("目标用户"), List.of("备注"));
        List<List<Object>> rows = new ArrayList<>();
        for (AssetRecord r : records) {
            Asset a = assets.get(r.getAssetId());
            List<Object> row = new ArrayList<>();
            row.add(r.getHappenedAt() == null ? "" : r.getHappenedAt().format(DATETIME));
            row.add(a == null ? "" : a.getCode());
            row.add(a == null ? "" : a.getName());
            row.add(actionText(r.getAction()));
            row.add(names.getOrDefault(r.getOperatorId(), ""));
            row.add(r.getTargetUserId() == null ? "" : names.getOrDefault(r.getTargetUserId(), ""));
            row.add(r.getRemark() == null ? "" : r.getRemark());
            rows.add(row);
        }
        return xlsx("我的借用记录", head, rows, "borrows-" + user.getUsername() + ".xlsx");
    }

    // ------------------------------------------------------------- 内部

    private ResponseEntity<byte[]> xlsx(String sheetName, List<List<String>> head, List<List<Object>> rows, String fileName) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.alibaba.excel.EasyExcel.write(out).head(head).sheet(sheetName).doWrite(rows);
        byte[] bytes = out.toByteArray();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(encoded).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private Map<Long, String> userNames(List<Asset> assets) {
        Map<Long, String> names = new HashMap<>();
        for (SysUser u : userMapper.selectList(null)) {
            names.put(u.getId(), u.getRealName());
        }
        return names;
    }

    private String statusText(String status) {
        if (Const.AssetStatus.IN_STOCK.equals(status)) {
            return "在库";
        }
        if (Const.AssetStatus.BORROWED.equals(status)) {
            return "借出";
        }
        if (Const.AssetStatus.DISABLED.equals(status)) {
            return "停用";
        }
        return status;
    }

    private String actionText(String action) {
        if (Const.AssetAction.BORROW.equals(action)) {
            return "借出";
        }
        if (Const.AssetAction.RETURN.equals(action)) {
            return "归还";
        }
        if (Const.AssetAction.TRANSFER.equals(action)) {
            return "转交";
        }
        return action;
    }
}
