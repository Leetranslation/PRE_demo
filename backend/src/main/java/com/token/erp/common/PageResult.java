package com.token.erp.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long page;
    private long pageSize;
    private long total;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setPage(page.getCurrent());
        r.setPageSize(page.getSize());
        r.setTotal(page.getTotal());
        r.setRecords(page.getRecords());
        return r;
    }
}
