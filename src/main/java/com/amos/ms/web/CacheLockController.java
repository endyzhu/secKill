package com.amos.ms.web;

import com.amos.ms.service.BusinessService;
import com.amos.ms.service.RedisService;
import com.amos.ms.service.impl.BusinessServiceImpl;
import com.amos.ms.util.CacheLockUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CountDownLatch;

@RestController
@RequestMapping(value = "/lock")
public class CacheLockController {
    @Autowired
    RedisService redisService;
    @Autowired
    BusinessService businessService;

    @GetMapping(value = "/{key}")
    public String setKey(@PathVariable("key") String key) {
        return redisService.setnx(key,System.currentTimeMillis()+"",10)+"";
    }
    @GetMapping(value = "/do")
    public String doAll() {
        businessService.secKill("test",10000001L);
        return "12313";
    }
    @GetMapping(value = "/secKill")
    public String secKill() {
        int threadCount = 1000;
        int splitPoint = 500;
        CountDownLatch endCount = new CountDownLatch(threadCount);
        CountDownLatch beginCount = new CountDownLatch(1);
        Thread[] threads = new Thread[threadCount];
        //起500个线程，秒杀第一个商品
        for (int i = 0; i < splitPoint; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 等待在一个信号量上，挂起
                        beginCount.await();
                        businessService.secKill("test", 10000001L);
                        endCount.countDown();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            threads[i].start();

        }
        //再起500个线程，秒杀第二件商品
        for (int i = splitPoint; i < threadCount; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 等待在一个信号量上，挂起
                        beginCount.await();
                        businessService.secKill("test", 10000002L);
                        endCount.countDown();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            threads[i].start();

        }


        long startTime = System.currentTimeMillis();
        //主线程释放开始信号量，并等待结束信号量，这样做保证1000个线程做到完全同时执行，保证测试的正确性
        beginCount.countDown();

        try {
            //主线程等待结束信号量
            endCount.await();
            //观察秒杀结果是否正确
            System.out.println(BusinessServiceImpl.inventory.get(10000001L));
            System.out.println(BusinessServiceImpl.inventory.get(10000002L));
            System.out.println("error count" + CacheLockUtils.count);
            System.out.println("total cost " + (System.currentTimeMillis() - startTime));
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "aaaa";
    }

}
