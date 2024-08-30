package com.wh.haobi.model.enums;

import lombok.Getter;

/**
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@Getter
public enum ResultEnum {
    WAIT("wait"),
    RUNNING("running"),
    SUCCEED("succeed"),
    FAILED("failed");

    private final String des;

    ResultEnum(String des) {
        this.des = des;
    }
}
