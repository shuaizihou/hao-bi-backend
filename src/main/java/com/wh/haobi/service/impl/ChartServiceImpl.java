package com.wh.haobi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wh.haobi.bizmq.BiMessageProducer;
import com.wh.haobi.common.ErrorCode;
import com.wh.haobi.constant.BIMQConstant;
import com.wh.haobi.exception.BusinessException;
import com.wh.haobi.exception.ThrowUtils;
import com.wh.haobi.manager.AiManager;
import com.wh.haobi.model.controller.ChartGenController;
import com.wh.haobi.model.dto.chart.ChartGenResult;
import com.wh.haobi.model.entity.Chart;
import com.wh.haobi.model.enums.ResultEnum;
import com.wh.haobi.model.vo.BiResponse;
import com.wh.haobi.service.ChartService;
import com.wh.haobi.mapper.ChartMapper;
import com.wh.haobi.utils.ChartDataUtil;
import com.wh.haobi.utils.ExcelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表服务
 *
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private AiManager aiManager;
    @Resource
    private ChartMapper chartMapper;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 同步 生成图表
     *
     * @param multipartFile
     * @param chartGenController
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 事务错误，回滚
    public BiResponse getChart(final MultipartFile multipartFile,
                               final ChartGenController chartGenController) {
        ThrowUtils.throwIf(chartGenController == null, ErrorCode.PARAMS_ERROR);
        final String goal = chartGenController.getGoal();
        final String chartType = chartGenController.getChartType();
        // 分析 xlsx 文件
        String cvsData = ExcelUtils.excelToCsv(multipartFile);
        // 发送给 AI 分析数据
        ChartGenResult chartGenResult = ChartDataUtil.getGenResult(aiManager, goal, cvsData, chartType);
        Result result = saveChart(chartGenController, chartGenResult, cvsData);
        return new BiResponse(result.chartId, result.genChart, result.genResult);
    }

    /**
     * 异步 生成图表
     *
     * @param multipartFile
     * @param chartGenController
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BiResponse getChartASYNC(final MultipartFile multipartFile,
                                    final ChartGenController chartGenController) {
        ThrowUtils.throwIf(chartGenController == null, ErrorCode.PARAMS_ERROR);
        final String goal = chartGenController.getGoal();
        final String chartType = chartGenController.getChartType();
        // 分析 xlsx 文件
        String cvsData = ExcelUtils.excelToCsv(multipartFile);
        // 首先保存到数据库中
        Chart beforeGenChart = new Chart(chartGenController.getGoal(), chartGenController.getChartType(), chartGenController.getLoginUserId(), ResultEnum.WAIT.getDes());
        boolean beforeSavedResult = this.save(beforeGenChart);
        ThrowUtils.throwIf(!beforeSavedResult, ErrorCode.SYSTEM_ERROR);
        asyncProcessChartData(goal, chartType, cvsData, beforeGenChart);
        Long chartId = beforeGenChart.getId();
        saveCVSData(cvsData, chartId);
        return new BiResponse(chartId);
    }

    @Override
    public BiResponse getChartMQ(MultipartFile multipartFile, ChartGenController chartGenController) {
        ThrowUtils.throwIf(chartGenController == null, ErrorCode.PARAMS_ERROR);
        final String goal = chartGenController.getGoal();
        final String chartType = chartGenController.getChartType();
        // 分析 xlsx 文件
        String cvsData = ExcelUtils.excelToCsv(multipartFile);
        Chart chart = new Chart(chartGenController.getName(), goal, chartType, chartGenController.getLoginUserId());
        boolean saveResult = this.save(chart);
        Long chartId = chart.getId();
        saveCVSData(cvsData, chartId);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "保存图表信息失败");
        this.updateById(new Chart(chartId, ResultEnum.WAIT.getDes(), ""));
        biMessageProducer.sendMessage(BIMQConstant.BI_EXCHANGE_NAME, BIMQConstant.BI_ROUTING_KEY, String.valueOf(chartId));
        return new BiResponse(chartId);
    }

    /**
     * 并发保存任务到数据库
     */
    public void asyncProcessChartData(final String goal, final String chartType, final String cvsData, final Chart beforeGenChart) {
        CompletableFuture.runAsync(() -> {
            Long chartId = beforeGenChart.getId();
            this.updateById(new Chart(beforeGenChart.getId(), ResultEnum.RUNNING.getDes(), ""));
            try {
                ChartGenResult result = ChartDataUtil.getGenResult(aiManager, goal, cvsData, chartType);
                Chart afterGenChart = new Chart(chartId, result.getGenChart(), result.getGenResult(), ResultEnum.SUCCEED.getDes(), "");
                this.updateById(afterGenChart);
            } catch (Exception e) {
                Chart chart = new Chart(chartId, ResultEnum.FAILED.getDes(), ExceptionUtils.getStackTrace(e));
                this.updateById(chart);
            }
        }, threadPoolExecutor);
    }


    @NotNull
    private Result saveChart(ChartGenController chartGenController, ChartGenResult chartGenResult, String cvsData) {
        String genChart = chartGenResult.getGenChart();
        String genResult = chartGenResult.getGenResult();
        Chart chart = Chart.successChart(genChart, genResult, chartGenController.getName(), chartGenController.getLoginUserId());
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "保存图表信息失败");
        // 创建表、保存数据
        Long chartId = chart.getId();
        saveCVSData(cvsData, chartId);
        return new Result(genChart, genResult, chartId);
    }

    private static class Result {
        public final String genChart;
        public final String genResult;
        public final Long chartId;

        public Result(String genChart, String genResult, Long chartId) {
            this.genChart = genChart;
            this.genResult = genResult;
            this.chartId = chartId;
        }
    }


    /**
     * 生成建表格 SQL 并且插入 cvs 数据到数据库
     *
     * @param cvsData
     * @param chartId
     */
    private void saveCVSData(final String cvsData, final Long chartId) {
        String[] columnHeaders = cvsData.split("\n")[0].split(",");
        StringBuilder sqlColumns = new StringBuilder();
        for (int i = 0; i < columnHeaders.length; i++) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(columnHeaders[i]), ErrorCode.PARAMS_ERROR);
            sqlColumns.append("`").append(columnHeaders[i]).append("`").append(" varchar(50) NOT NULL");
            if (i != columnHeaders.length - 1) {
                sqlColumns.append(", ");
            }
        }
        String sql = String.format("CREATE TABLE charts_%d ( %s )", chartId, sqlColumns);
        String[] columns = cvsData.split("\n");
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO charts_").append(chartId).append(" VALUES ");
        for (int i = 1; i < columns.length; i++) {
            String[] strings = columns[i].split(",");
            insertSql.append("(");
            for (int j = 0; j < strings.length; j++) {
                insertSql.append("'").append(strings[j]).append("'");
                if (j != strings.length - 1) {
                    insertSql.append(", ");
                }
            }
            insertSql.append(")");
            if (i != columns.length - 1) {
                insertSql.append(", ");
            }
        }
        try {
            chartMapper.createTable(sql);
            chartMapper.insertValue(insertSql.toString());
        } catch (Exception e) {
            log.error("插入数据报错 " + e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

}




