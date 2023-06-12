package com.itheima.reggie.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.VoucherOrder;
import com.itheima.reggie.mapper.VoucherOrderMapper;
import com.itheima.reggie.service.SeckillVoucherService;
import com.itheima.reggie.service.VoucherOrderService;
import com.itheima.reggie.utils.BaseContext;
import com.itheima.reggie.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.*;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private SeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //阻塞队列：一个线程尝试获取当前队列的元素时，如果没有元素，会被阻塞；只有队列出现元素时才会唤醒线程
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    //多线程，线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //线程任务从项目一启动就开始执行，阻塞消费订单
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }


    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //注意，此时线程是线程池里面的，不能用threadLocal了
        Long userId = voucherOrder.getUserId();
        //创建锁对象（以当前用户ID作为key
        RLock lock = redissonClient.getLock("order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        if(!isLock) {
            //获取锁失败
            log.error("不容许重复下单");
        }

        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    private VoucherOrderService proxy;

    @Override
    //    一人一单
    public R<String> seckillVoucher(Long voucherId) {
        //获取用户
        Long userId = BaseContext.getCurrentId();
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本-redis的热点数据逻辑
        /*
        这里使用RedisTemplate出错了
        Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception
        未解决
         */
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return R.error(r == 1 ? "库存不足" : "不能重复下单");
        }

        // 2.1.订单id
        VoucherOrder voucherOrder = new VoucherOrder();
//        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.2.用户id
        voucherOrder.setUserId(userId);
        // 2.3.代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.4.放入阻塞队列
        //TODO 这里用mq优化
        orderTasks.add(voucherOrder);

        //获取代理对象（事务）
        proxy = (VoucherOrderService) AopContext.currentProxy();

        // 3.返回订单id
        return R.success(String.valueOf(orderId));



    }
/*
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
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, redisTemplate);
        RLock lock = redissonClient.getLock("order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();

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


    }*/

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //5.1 获得用户id，是否可以扣减库存
        Long userId = voucherOrder.getUserId();
//        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder).count();
//        if(count > 0) {
//            // 用户已经购买过了
//            return R.error("用户已经购买过一次！");
//        }
        //5.2 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")
                .eq("voucher_id", voucherOrder.getVoucherId()).update(); //where id = ? and stock > 0
//        if (!success) {
//            //扣减库存
//            return R.error("库存不足！");
//        }
        //上述代码可以加可以不加，加的话改成日志防止redis出错导致的意外
        //6. 创建订单
        save(voucherOrder);

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
