package com.itheima.reggie.lock;


import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author:sfy
 * @Date: 2023/6/11 - 23:27
 * Description:
 */
public class SimpleRedisLock implements ILock{
    //业务锁名称
    private String name;
    private RedisTemplate redisTemplate;

    public SimpleRedisLock(String name, RedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

//    private static final String KEY_PREFIX = "lock:";
    //不同JVM的ID_pre不一样
    //相同的JVM有线程号区分，不同的JVM有uuid区分
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    @Override
    public boolean tryLock(Long timeoutSec) {
        //获取线程标识
        //相同的JVM有线程号区分，不同的JVM有uuid区分(作为value
        String jvmThreadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + name, jvmThreadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);

        /*
        这里可以实现
        1.可重入：<k, <jvmThreadId, state>>
        2.锁重试：while(true) + 订阅锁的释放信息
        3.超时释放：设置超时时间，但是内部反复重置超时时间，从而保证工作期间不会因为ttl而释放锁，
        把“因为ttl而释放锁”放在线程结束或者宕机的这个阶段
         */
    }

    @Override
    public void unlock() {
        //调用lua脚本-实现-保证“判断+释放”是原子操作
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList("lock:" + name),
                ID_PREFIX + Thread.currentThread().getId());
    }


    //释放锁-解决误删问题(根据业务锁名K，判断v是否一样（即是不是自己的锁




//    @Override
//    public void unlock() {
//        //获取线程标识和锁的标识是否一致
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        String id = (String) redisTemplate.opsForValue().get("lock:" + name);
//        //极端情况下，刚好判断完再释放的时候：
//        //此时线程1业务逻辑阻塞了，线程1的锁超时释放了；线程2进来获取到锁；然后线程1业务逻辑继续，删除了锁
//        //因此要改进：保证“判断+释放”是原子操作
//        if(threadId.equals(id)) {
//            redisTemplate.delete("lock:" + name);
//        }
//
//    }
}
