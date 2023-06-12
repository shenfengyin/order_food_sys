package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.SeckillVoucher;
import com.itheima.reggie.entity.Voucher;
import com.itheima.reggie.entity.VoucherOrder;
import com.itheima.reggie.mapper.VoucherMapper;
import com.itheima.reggie.service.SeckillVoucherService;
import com.itheima.reggie.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @Author:sfy
 * @Date: 2023/6/10 - 17:49
 * Description:
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {

    @Autowired
    private SeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisTemplate redisTemplate;
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 保存秒杀库存到Redis中
        redisTemplate.opsForValue().set("seckill:stock:" + voucher.getId(), voucher.getStock().toString());
    }


}
