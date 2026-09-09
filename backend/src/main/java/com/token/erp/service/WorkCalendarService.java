package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.dto.WorkCalendarRequest;
import com.token.erp.entity.WorkCalendar;
import com.token.erp.mapper.WorkCalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkCalendarService {

    private final WorkCalendarMapper workCalendarMapper;

    public Page<WorkCalendar> page(long page, long pageSize, String yearMonth) {
        LambdaQueryWrapper<WorkCalendar> qw = new LambdaQueryWrapper<>();
        if (yearMonth != null && !yearMonth.isEmpty()) {
            YearMonth ym = YearMonth.parse(yearMonth);
            qw.ge(WorkCalendar::getWorkDate, ym.atDay(1))
                    .le(WorkCalendar::getWorkDate, ym.atEndOfMonth());
        }
        qw.orderByAsc(WorkCalendar::getWorkDate);
        return workCalendarMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /**
     * 批量设置某月的工作日/非工作日
     */
    @Transactional
    public void batchSet(WorkCalendarRequest req) {
        if (req.getYearMonth() == null || req.getYearMonth().isEmpty()) {
            throw new BizException("月份不能为空");
        }
        if (req.getIsWorkday() == null) {
            throw new BizException("是否工作日不能为空");
        }
        YearMonth ym = YearMonth.parse(req.getYearMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<WorkCalendar> list = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // 查是否存在
            WorkCalendar existing = workCalendarMapper.selectOne(
                    new LambdaQueryWrapper<WorkCalendar>().eq(WorkCalendar::getWorkDate, d));
            if (existing != null) {
                existing.setIsWorkday(req.getIsWorkday());
                workCalendarMapper.updateById(existing);
            } else {
                WorkCalendar wc = new WorkCalendar();
                wc.setWorkDate(d);
                wc.setIsWorkday(req.getIsWorkday());
                wc.setRemark(req.getRemark());
                list.add(wc);
            }
        }
        if (!list.isEmpty()) {
            for (WorkCalendar wc : list) {
                workCalendarMapper.insert(wc);
            }
        }
    }
}
