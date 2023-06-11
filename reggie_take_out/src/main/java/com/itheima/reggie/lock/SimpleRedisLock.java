package com.itheima.reggie.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

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
    //每台JVM的ID_pre不一样
    //相同的JVM有线程号区分，不同的JVM有uuid区分
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";


    @Override
    public boolean tryLock(Long timeoutSec) {
        //获取线程标识
        //相同的JVM有线程号区分，不同的JVM有uuid区分
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }


    //释放锁-解决误删问题(根据业务锁名K，判断v是否一样（即是不是自己的锁
    @Override
    public void unlock() {
        //获取线程标识和锁的标识是否一致
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        String id = (String) redisTemplate.opsForValue().get("lock:" + name);

        if(threadId.equals(id)) {
            redisTemplate.delete("lock:" + name);
        }

    }
}
