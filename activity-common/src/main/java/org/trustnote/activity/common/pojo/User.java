package org.trustnote.activity.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author zhuxl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Integer id;

    private String realname;

    private String password;

    private String phone;

    private String walletAddress;

    private Integer state;

    private String userDesc;

    private Date crtTime;

    private Date uptTime;

    private LocalDateTime lastLoginTime;

}