package com.mao.maocodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mao.maocodemother.annotation.AuthCheck;
import com.mao.maocodemother.common.BaseResponse;
import com.mao.maocodemother.common.DeleteRequest;
import com.mao.maocodemother.common.ResultUtils;
import com.mao.maocodemother.constant.UserConstant;
import com.mao.maocodemother.exception.BusinessException;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.SysSensitiveWordMapper;
import com.mao.maocodemother.model.dto.sensitive.SensitiveWordAddRequest;
import com.mao.maocodemother.model.dto.sensitive.SensitiveWordBatchRequest;
import com.mao.maocodemother.model.dto.sensitive.SensitiveWordQueryRequest;
import com.mao.maocodemother.model.entity.SysSensitiveWord;
import com.mao.maocodemother.service.SensitiveWordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 敏感词 控制层。
 */
@RestController
@RequestMapping("/admin/sensitive")
public class SensitiveWordController {

    @Resource
    private SysSensitiveWordMapper sensitiveWordMapper;

    @Resource
    private SensitiveWordService sensitiveWordService;

    /**
     * 分页查询敏感词
     */
    @PostMapping("/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SysSensitiveWord>> page(@RequestBody SensitiveWordQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (StrUtil.isNotBlank(request.getKeyword())) {
            queryWrapper.like("word", request.getKeyword());
        }
        if (StrUtil.isNotBlank(request.getCategory())) {
            queryWrapper.eq("category", request.getCategory());
        }
        if (request.getEnabled() != null) {
            queryWrapper.eq("enabled", request.getEnabled());
        }
        queryWrapper.orderBy("createTime", false);
        Page<SysSensitiveWord> page = sensitiveWordMapper.paginate(
                Page.of(request.getPageNum(), request.getPageSize()),
                queryWrapper
        );
        return ResultUtils.success(page);
    }

    /**
     * 获取所有敏感词列表
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<SysSensitiveWord>> list() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .orderBy("createTime", false);
        List<SysSensitiveWord> list = sensitiveWordMapper.selectListByQuery(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 新增敏感词
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> add(@RequestBody SensitiveWordAddRequest request) {
        ThrowUtils.throwIf(request == null || StrUtil.isBlank(request.getWord()), ErrorCode.PARAMS_ERROR);
        // 检查是否已存在
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("word", request.getWord().trim());
        Long count = sensitiveWordMapper.selectCountByQuery(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该敏感词已存在");
        }
        SysSensitiveWord word = new SysSensitiveWord();
        BeanUtil.copyProperties(request, word);
        word.setWord(request.getWord().trim());
        if (word.getEnabled() == null) {
            word.setEnabled(1);
        }
        int result = sensitiveWordMapper.insert(word);
        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
        // 刷新缓存
        sensitiveWordService.refreshCache();
        // 雪花 ID 超过 JS 安全整数范围，转字符串返回避免前端精度丢失
        return ResultUtils.success(String.valueOf(word.getId()));
    }

    /**
     * 更新敏感词
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update(@RequestBody SensitiveWordAddRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        SysSensitiveWord word = new SysSensitiveWord();
        BeanUtil.copyProperties(request, word);
        if (request.getWord() != null) {
            word.setWord(request.getWord().trim());
        }
        int result = sensitiveWordMapper.update(word);
        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
        // 刷新缓存
        sensitiveWordService.refreshCache();
        return ResultUtils.success(true);
    }

    /**
     * 删除敏感词
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        int result = sensitiveWordMapper.deleteById(request.getId());
        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
        // 刷新缓存
        sensitiveWordService.refreshCache();
        return ResultUtils.success(true);
    }

    /**
     * 批量导入敏感词
     */
    @PostMapping("/batch/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> batchAdd(@RequestBody SensitiveWordBatchRequest request) {
        ThrowUtils.throwIf(request == null || request.getWords() == null
                || request.getWords().isEmpty(), ErrorCode.PARAMS_ERROR);
        int successCount = 0;
        String category = StrUtil.isNotBlank(request.getCategory()) ? request.getCategory() : "OTHER";
        for (String word : request.getWords()) {
            if (StrUtil.isBlank(word)) {
                continue;
            }
            String trimmed = word.trim();
            // 检查是否已存在
            QueryWrapper queryWrapper = QueryWrapper.create().eq("word", trimmed);
            Long count = sensitiveWordMapper.selectCountByQuery(queryWrapper);
            if (count != null && count > 0) {
                continue;
            }
            SysSensitiveWord sensitiveWord = SysSensitiveWord.builder()
                    .word(trimmed)
                    .category(category)
                    .enabled(1)
                    .build();
            int result = sensitiveWordMapper.insert(sensitiveWord);
            if (result > 0) {
                successCount++;
            }
        }
        // 刷新缓存
        sensitiveWordService.refreshCache();
        return ResultUtils.success(successCount);
    }

    /**
     * 刷新敏感词缓存
     */
    @PostMapping("/refresh")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> refresh() {
        sensitiveWordService.refreshCache();
        return ResultUtils.success(true);
    }

    /**
     * 检测文本是否包含敏感词（公开接口，供前端预检）
     */
    @PostMapping("/check")
    public BaseResponse<Object> checkText(@RequestBody String text) {
        Set<String> words = sensitiveWordService.findSensitiveWords(text);
        return ResultUtils.success(words);
    }
}
