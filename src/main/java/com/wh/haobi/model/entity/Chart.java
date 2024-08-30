package com.wh.haobi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.wh.haobi.common.ErrorCode;
import com.wh.haobi.exception.ThrowUtils;
import com.wh.haobi.model.enums.ResultEnum;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

/**
 * 图表信息表
 * @TableName chart
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@TableName(value ="chart")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Chart implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 生成的分析结论
     */
    private String genResult;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 执行信息
     */
    private String execMessage;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public Chart(String name, String goal, String chartType, String genChart, String genResult, Long userId) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(goal, chartData, chartType, genChart, genResult) && (userId == null || userId < 0), ErrorCode.PARAMS_ERROR);
        this.name = name;
        this.goal = goal;
        this.chartType = chartType;
        this.genChart = genChart;
        this.genResult = genResult;
        this.userId = userId;
    }

    public Chart(Long id, String status, String execMessage) {
        ThrowUtils.throwIf((id == null || id < 0) || StringUtils.isAnyEmpty(status), ErrorCode.PARAMS_ERROR);
        this.id = id;
        this.status = status;
        this.execMessage = execMessage;
    }
    public Chart(String status, String execMessage, String genChart, String genResult, Long id) {
        ThrowUtils.throwIf((id == null || id < 0) || StringUtils.isAnyEmpty(status), ErrorCode.PARAMS_ERROR);
        this.id = id;
        this.status = status;
        this.execMessage = execMessage;
        this.genChart = genChart;
        this.genResult =  genResult;
    }

    public Chart(String goal, String chartType, Long userId, String status) {
        this.goal = goal;
        this.chartType = chartType;
        this.userId = userId;
        this.status = status;
    }

    public Chart(Long id, String genChart, String genResult, String status, String execMessage) {
        ThrowUtils.throwIf((id == null || id < 0) || StringUtils.isAnyEmpty(genChart, genResult, status), ErrorCode.PARAMS_ERROR);
        this.id = id;
        this.genChart = genChart;
        this.genResult = genResult;
        this.status = status;
        this.execMessage = execMessage;
    }

    public Chart(String name, String goal, String chartType, Long userId) {
        this.name = name;
        this.goal = goal;
        this.chartType = chartType;
        this.userId = userId;
    }

    public static Chart successChart(String genChart, String genResult, String name, Long userId) {
        return Chart.builder()
                .userId(userId)
                .name(name)
                .genChart(genChart)
                .genResult(genResult)
                .status(ResultEnum.SUCCEED.getDes())
                .build();
    }
}