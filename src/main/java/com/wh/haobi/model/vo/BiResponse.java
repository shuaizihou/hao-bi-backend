package com.wh.haobi.model.vo;

import com.wh.haobi.common.ErrorCode;
import com.wh.haobi.exception.ThrowUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Bi 的返回结果
 *
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@Data
@NoArgsConstructor
public class BiResponse {

    private String genChart;

    private String genResult;

    private Long chartId;

    /**
     * 这里可以校验 AI 生成的内容
     */
    public BiResponse(Long chartId, String genChart, String genResult) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(genChart, genResult) || (chartId != null && chartId <= 0), ErrorCode.PARAMS_ERROR);
        this.chartId = chartId;
        this.genChart = genChart;
        this.genResult = genResult;
    }

    public BiResponse(Long chartId) {
        this.chartId = chartId;
    }
}