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
public class ChartRetryController implements Serializable {


    private static final long serialVersionUID = 2645307609377346713L;
    /**
     * chartId
     */
    private Long chartId;

    /**
     * 登录的用户
     */
    private User loginUser;

    public Long getLoginUserId() {
        return loginUser.getId();
    }
}
