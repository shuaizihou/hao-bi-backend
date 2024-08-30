package com.wh.haobi.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.haobi.common.BaseResponse;
import com.wh.haobi.common.DeleteRequest;
import com.wh.haobi.common.ErrorCode;
import com.wh.haobi.common.ResultUtils;
import com.wh.haobi.constant.CommonConstant;
import com.wh.haobi.exception.BusinessException;
import com.wh.haobi.exception.ThrowUtils;
import com.wh.haobi.manager.RedisLimiterManager;
import com.wh.haobi.model.controller.ChartGenController;
import com.wh.haobi.model.dto.chart.*;
import com.wh.haobi.model.entity.Chart;
import com.wh.haobi.model.entity.User;
import com.wh.haobi.model.vo.BiResponse;
import com.wh.haobi.service.ChartService;
import com.wh.haobi.service.UserService;
import com.wh.haobi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 图表接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 智能分析 (同步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 final GenChartByAiRequest genChartByAiRequest,
                                                 HttpServletRequest request) {
        validFile(multipartFile);
        User loginUser = userService.getLoginUser(request);
        // 增加限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());
        ChartGenController chartGenController = new ChartGenController(genChartByAiRequest.getName(), genChartByAiRequest.getGoal(), genChartByAiRequest.getChartType(), loginUser);
        BiResponse chart = chartService.getChart(multipartFile, chartGenController);
        return ResultUtils.success(chart);
    }

    /**
     * 智能分析 (异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      final GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        validFile(multipartFile);
        User loginUser = userService.getLoginUser(request);
        // 增加限流器
        redisLimiterManager.doRateLimit("genChartByAiAsync_" + loginUser.getId());
        ChartGenController chartGenController = new ChartGenController(genChartByAiRequest.getName(), genChartByAiRequest.getGoal(), genChartByAiRequest.getChartType(), loginUser);
        BiResponse chart = chartService.getChartASYNC(multipartFile, chartGenController);
        return ResultUtils.success(chart);
    }

    /**
     * 智能分析 (异步消息队列)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        final GenChartByAiRequest genChartByAiRequest,
                                                        HttpServletRequest request) {
        validFile(multipartFile);
        User loginUser = userService.getLoginUser(request);
        // 增加限流器
        redisLimiterManager.doRateLimit("genChartByAiMQ_" + loginUser.getId());
        ChartGenController chartGenController = new ChartGenController(genChartByAiRequest.getName(), genChartByAiRequest.getGoal(), genChartByAiRequest.getChartType(), loginUser);
        BiResponse chart = chartService.getChartMQ(multipartFile, chartGenController);
        return ResultUtils.success(chart);
    }


    /**
     * 校验文件
     *
     * @param multipartFile 文件类型
     */
    private void validFile(MultipartFile multipartFile) {
        // 文件大小
        final long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long TEN_MAX = 1024 * 1024 * 10L;
        if (fileSize > TEN_MAX) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 10M");
        }
        if (!Arrays.asList("xlsx", "xls").contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
    }
}
