package com.amos.ms.service.impl;

import com.amos.ms.service.BusinessService;
import com.amos.ms.type.CacheLock;
import com.amos.ms.type.LockedObject;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 实际的业务处理接口
 * @author zhuqb
 */
@Service
public class BusinessServiceImpl implements BusinessService {
    /**
     * 模拟库存数据
     */
    public static Map<Long, Long> inventory = new HashMap<>();
    static{
        inventory.put(10000001L, 10000L);
        inventory.put(10000002L, 10000L);
    }
    @CacheLock(lockedPrefix = "TEST_PREFIX",expireTime = 1000)
    @Override
    public void secKill(String userID, @LockedObject Long commidityID) {
        this.reduceInventory(commidityID);
    }

    //模拟秒杀操作，姑且认为一个秒杀就是将库存减一，实际情景要复杂的多
    public Long reduceInventory(Long commodityId){
        inventory.put(commodityId,inventory.get(commodityId) - 1);
        return inventory.get(commodityId);
    }

}
