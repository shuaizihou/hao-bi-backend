package com.wh.haobi.service;

import com.wh.haobi.model.controller.ChartGenController;
import com.wh.haobi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wh.haobi.model.vo.BiResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
public interface ChartService extends IService<Chart> {
    
    BiResponse getChart(final MultipartFile multipartFile, final ChartGenController chartGenController);


    BiResponse getChartASYNC(MultipartFile multipartFile, ChartGenController chartGenController);

    BiResponse getChartMQ(MultipartFile multipartFile, ChartGenController chartGenController);
}
