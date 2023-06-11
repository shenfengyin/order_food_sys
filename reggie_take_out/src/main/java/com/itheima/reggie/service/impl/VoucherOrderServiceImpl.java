package com.itheima.reggie.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.SeckillVoucher;
import com.itheima.reggie.entity.Voucher;
import com.itheima.reggie.entity.VoucherOrder;
import com.itheima.reggie.lock.SimpleRedisLock;
import com.itheima.reggie.mapper.VoucherOrderMapper;
import com.itheima.reggie.service.SeckillVoucherService;
import com.itheima.reggie.service.VoucherOrderService;
import com.itheima.reggie.utils.BaseContext;
import com.itheima.reggie.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private SeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedisTemplate redisTemplate;
    @Override
    //    一人一单
    public R<String> seckillVoucher(Long voucherId) {
        // 1.查询优惠券（此时快照了
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return R.error("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return R.error("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return R.error("库存不足！");
        }
        Long userId = BaseContext.getCurrentId();
        //创建锁对象（以当前用户ID作为key
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, redisTemplate);
        //获取锁
        boolean isLock = lock.tryLock(1200L);

        if(!isLock) {
            //获取锁失败
            return R.error("不容许重复下单");
        }

        try {
            //获取代理对象（事务）
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }


    }

    @Transactional
    public R<String> createVoucherOrder(Long voucherId) {
        //5.1 获得用户id，是否可以扣减库存
        Long userId = BaseContext.getCurrentId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count > 0) {
            // 用户已经购买过了
            return R.error("用户已经购买过一次！");
        }
        //5.2 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1")
                .eq("voucher_id", voucherId).update(); //where id = ? and stock > 0
        if (!success) {
            //扣减库存
            return R.error("库存不足！");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2.用户id
        voucherOrder.setUserId(userId);
        // 6.3.代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        return R.success("success");


    }

//    @Transactional
//    public R<String> createVoucherOrder(Long voucherId) {
//        //5.1 获得用户id，是否可以扣减库存
//        Long userId = BaseContext.getCurrentId();
//        //此时的问题是事务未提交前，锁释放了，同一个用户又会进来得到锁
//        synchronized (userId.toString().intern()) {
//            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//            if(count > 0) {
//                // 用户已经购买过了
//                return R.error("用户已经购买过一次！");
//            }
//            //5.2 扣减库存
//            boolean success = seckillVoucherService.update()
//                    .setSql("stock= stock -1")
//                    .eq("voucher_id", voucherId).update(); //where id = ? and stock > 0
//            if (!success) {
//                //扣减库存
//                return R.error("库存不足！");
//            }
//            //6.创建订单
//            VoucherOrder voucherOrder = new VoucherOrder();
//            // 6.1.订单id
//            long orderId = redisIdWorker.nextId("order");
//            voucherOrder.setId(orderId);
//            // 6.2.用户id
//            voucherOrder.setUserId(userId);
//            // 6.3.代金券id
//            voucherOrder.setVoucherId(voucherId);
//            save(voucherOrder);
//
//            return R.success("success");
//        }
//
//    }


//    一人多单解决超卖问题
//    public R<String> seckillVoucher(Long voucherId) {
//        // 1.查询优惠券（此时快照了
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2.判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 尚未开始
//            return R.error("秒杀尚未开始！");
//        }
//        // 3.判断秒杀是否已经结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            // 尚未开始
//            return R.error("秒杀已经结束！");
//        }
//        // 4.判断库存是否充足
//        if (voucher.getStock() < 1) {
//            // 库存不足
//            return R.error("库存不足！");
//        }
//        //5，扣减库存
////        boolean success = seckillVoucherService.update()
////                .setSql("stock= stock -1")    //set语句
////                .eq("voucher_id", voucherId).update();  //where 条件
//        boolean success = seckillVoucherService.update()
//                .setSql("stock= stock -1")
////                .eq("voucher_id", voucherId).eq("stock",voucher.getStock()).update(); //where id = ？ and stock = ?
//                .eq("voucher_id", voucherId).gt("stock",0).update(); //where id = ? and stock > 0
//        if (!success) {
//            //扣减库存
//            return R.error("库存不足！");
//        }
//        //6.创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 6.1.订单id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        // 6.2.用户id
//        Long userId = BaseContext.getCurrentId();
//        voucherOrder.setUserId(userId);
//        // 6.3.代金券id
//        voucherOrder.setVoucherId(voucherId);
//        save(voucherOrder);
//
//        return R.success("success");
//
//    }
}
