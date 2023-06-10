package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Voucher;
import com.itheima.reggie.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * @Author:sfy
 * @Date: 2023/6/10 - 17:51
 * Description:
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {
    @Autowired
    private VoucherService voucherService;

    @PostMapping("seckill")
    public R<String> addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return R.success("添加秒杀券成功");
    }

//    /**
//     * 查询店铺的优惠券列表
//     * @param shopId 店铺id
//     * @return 优惠券列表
//     */
//    @GetMapping("/list/{shopId}")
//    public R queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
//        return voucherService.queryVoucherOfShop(shopId);
//    }
}
