package com.wh.haobi.model.controller;

import com.wh.haobi.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChartGenController implements Serializable {

    /**
     * 图标名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 登录的用户
     */
    private User loginUser;

    public Long getLoginUserId() {
        return loginUser.getId();
    }
}
