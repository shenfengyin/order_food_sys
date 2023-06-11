package com.itheima.reggie.dto;

import com.itheima.reggie.entity.User;
import lombok.Data;

/**
 * @Author:sfy
 * @Date: 2023/6/11 - 13:06
 * Description:
 */
@Data
public class UserDto extends User {
    private String token;
}
