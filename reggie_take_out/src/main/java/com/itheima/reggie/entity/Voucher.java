package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author:sfy
 * @Date: 2023/6/10 - 17:12
 * Description:
 */
@Data
@ApiModel("优惠券")
public class Voucher implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private long id;
    private long dish_id;
    private String title;
    private String rules;
    private int payValue;
    private int actualValue;
    private int type;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /**
    * Description:以下是不在表中的字段，但是前后端交互需要
    * date: 2023/6/10 18:19
    * @author: sfy
    */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    private LocalDateTime endTime;
}
