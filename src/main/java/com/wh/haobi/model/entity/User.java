package com.wh.haobi.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * 用户
 * @TableName user
 *
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@TableName(value ="user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

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

    public User(String userAccount, String userPassword) {
        this.userAccount = userAccount;
        this.userPassword = userPassword;
    }

    public User(Long userId, String userName, String userAvatar) {
        this.id = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
    }

    public static User newUser(String userAccount, String userPassword) {
        return User.builder()
                .userAvatar("https://yupi.icu/logo.png")
                .userAccount(userAccount)
                .userPassword(userPassword)
                .build();
    }
}