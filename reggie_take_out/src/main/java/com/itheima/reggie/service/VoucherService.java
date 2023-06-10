package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Voucher;

/**
 * @Author:sfy
 * @Date: 2023/6/10 - 17:48
 * Description:
 */
public interface VoucherService extends IService<Voucher> {
    void addSeckillVoucher(Voucher voucher);

}
