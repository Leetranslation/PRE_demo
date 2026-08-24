package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.Dictionary;
import com.token.erp.mapper.DictionaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final DictionaryMapper dictionaryMapper;

    public Page<Dictionary> page(long page, long pageSize, String dictType, String keyword) {
        LambdaQueryWrapper<Dictionary> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictType)) {
            qw.eq(Dictionary::getDictType, dictType);
        }
        if (StringUtils.hasText(keyword)) {
            qw.like(Dictionary::getDictLabel, keyword);
        }
        qw.orderByAsc(Dictionary::getDictType).orderByAsc(Dictionary::getSort);
        return dictionaryMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    /** 按类型查询字典项（供下拉框使用） */
    public List<Dictionary> listByType(String dictType) {
        return dictionaryMapper.selectList(
                new LambdaQueryWrapper<Dictionary>()
                        .eq(Dictionary::getDictType, dictType)
                        .eq(Dictionary::getStatus, 1)
                        .orderByAsc(Dictionary::getSort));
    }

    public Dictionary create(Dictionary dictionary) {
        if (!StringUtils.hasText(dictionary.getDictType())) {
            throw new BizException("字典类型不能为空");
        }
        if (!StringUtils.hasText(dictionary.getDictLabel())) {
            throw new BizException("字典标签不能为空");
        }
        dictionary.setId(null);
        dictionary.setStatus(dictionary.getStatus() == null ? 1 : dictionary.getStatus());
        dictionaryMapper.insert(dictionary);
        return dictionary;
    }

    public Dictionary update(Dictionary dictionary) {
        if (dictionaryMapper.selectById(dictionary.getId()) == null) {
            throw new BizException(404, "字典项不存在");
        }
        dictionaryMapper.updateById(dictionary);
        return dictionaryMapper.selectById(dictionary.getId());
    }
}
