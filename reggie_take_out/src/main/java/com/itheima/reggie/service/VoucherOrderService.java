package com.itheima.reggie.service;

import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.VoucherOrder;

/**
 * @Author:sfy
 * @Date: 2023/6/10 - 20:31
 * Description:
 */
public interface VoucherOrderService {
    R<String> seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
