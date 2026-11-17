package com.ers.asset.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/编辑资产的入参（PRD 4.2(7) 管理端 CRUD）。
 */
@Data
public class AssetSaveRequest {

    @NotBlank(message = "资产编号不能为空")
    @Size(max = 32, message = "资产编号不能超过 32 个字符")
    private String code;

    @NotBlank(message = "资产名称不能为空")
    @Size(max = 64, message = "资产名称不能超过 64 个字符")
    private String name;

    @NotBlank(message = "资产分类不能为空")
    @Size(max = 32, message = "资产分类不能超过 32 个字符")
    private String category;

    @DecimalMin(value = "0.00", message = "资产价值必须大于等于 0")
    private BigDecimal value;

    @Size(max = 255, message = "描述不能超过 255 个字符")
    private String description;
}
