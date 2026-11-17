package com.ers.common;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 统一分页结构：{ records, total, page, size }，page 从 1 开始。
 */
public record PageResult<T>(List<T> records, long total, long page, long size) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }
}
