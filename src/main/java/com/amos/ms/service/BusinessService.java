package com.amos.ms.service;


/**
 * 秒杀义务类接口
 *
 * @author zhuqb
 */
public interface BusinessService {


    /**
     * cacheLock注解可能产生并发的方法
     * 最简单的秒杀方法，参数是用户ID和商品ID。可能有多个线程争抢一个商品，所以商品ID加上LockedObject注解
     *
     * @param userID
     * @param commidityID
     */
    void secKill(String userID, Long commidityID);

}
