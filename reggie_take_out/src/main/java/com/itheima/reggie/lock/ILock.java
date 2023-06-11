package com.itheima.reggie.lock;

/**
 * @Author:sfy
 * @Date: 2023/6/11 - 23:24
 * Description:
 */
public interface ILock {
    /**
    * Description:
    * date: 2023/6/11 23:25
    * @author: sfy
    */
    boolean tryLock(Long timeoutSec);

    void unlock();
}
