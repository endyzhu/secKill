## 《基于redis分布式锁实现秒杀》——总结
本文章参考简书上：
[基于redis分布式锁实现“秒杀”](https://www.jianshu.com/p/7a0f24e3d90f)
> 秒杀：
    1. 从业务角度来说，是用户对同一资源进行争抢
    2. 从业务角度来说，是多个线程对资源进行操作
<br/>总结：秒杀是对控制线程对资源的争抢，**既要保证高效并发，也要保证操作的正确性**

### 1.实现的方法
对于线程的控制，常用的可以有以下三种方法：

1. 对争夺资源的方法入口加锁synchronized，这个方法是最粗暴的，会降低系统的性能。
2. 在上面的基础上进行优化，我们可以对操作数据库的代码进行加锁，也就是对代码块进行加锁。这个加锁的的粒度也是比较大的，假设两个用户对不同的资源进行操作，比如说购买不同商品，从业务逻辑上来说是对不同的资源进行争抢，所以应该不是秒杀的业务实现，但是从技术层面来说，都对于商品这张表进行了操作，所以也就产生了竞争关系，所以也会降低系统的性能。
3. 既然是并发的问题，理论上说将所有的请求进行串行，使用队列进行管理，自然就不会有并发问题，这样的话对于队列的负载就会很大，一旦消息出错，容易造成消息阻塞和消息丢失情况。这也不是一个理想的方法。

### 2.解决思维
针对上面出现的问题，我们可以深入思考下，秒杀所出现的竞争关系是对同一个商品进行争抢，对于不同的商品是不应该出现竞争关系，所以我们需要在同一个商品上进行加锁。**分布式锁**可以解决上面的问题。

### 3.分布式锁
> 分布式锁：控制分布式系统之间同步访问共享资源的一种方式。

很官方的解释，理解起来的话就是不同系统或者同一系统不同主机共享资源，那么访问这些资源，需要互斥来彼此进行干扰，保持一致性。

### 4.模拟场景
目前分布式锁使用比较广泛的是redis，redis是key-value存储系统，他的特性很适合用来处理高并发：<br/>

1. 数据存储在内存中，处理速度非常快
2. 键可以设置过期时间，使用redis键来操作锁，设置过期时间可以有效的防止死锁
3. 单线程，消除了传统数据库串行控制的开销
4. 支持事务，操作都是原子性

现在我们来模拟秒杀的场景：<br/>

数据库里有一张表，column分别是商品ID，和商品ID对应的库存量，秒杀成功就将此商品库存量-1。现在假设有1000个线程来秒杀两件商品，500个线程秒杀第一个商品，500个线程秒杀第二个商品

### 5. 具体的实现
#### 5.1 redis的命令

```
## 如果key不存在就设置key以及对应的value，
## 如果存在就不做任何操作
SETNX key value 

## 设置键的过期时间
EXPIRE key sceonds

## 删除键
DEL key
```
#### 5.2 需要思考的问题
1. java如何操作redis
2. 怎么实现加锁
3. 如果释放锁
4. 阻塞还是非阻塞
5. 针对异常的处理

>在Spring中已经针对Redis的操作封装了jar包<br/>我们针对商品的操作，其实是针对数据库中对应商品的id进行操作，对商品加锁，可以将商品对应的id来作为key存储在redis中，在对该商品进行操作时，先查看下是否在redis中存在，如果存在的话说明已经有用户在对该商品进行操作了，此时需要等待上面的用户处理完成。用户处理完成之后，可以操作删除redis中对应的键，相当于释放了锁。<br>采用阻塞方式，当发现已经上锁了，在特定的时间里轮询锁<br/>业务由于种种原因导致失败，没有及时的释放锁，也就是删除redis中对应的key，我们可以添加键的失效时间来自动让锁释放。这样的话就避免了死锁的问题

#### 5.3 代码实现
以上都是理论性的讨论，现在开始基于之前的思考，来使用代码实现（代码基于博客上的代码进行了修改）：
#### 5.3.1 自定义AOP需要切入的注解

```java
/**
 * 方法级注解
 *
 * @author zhuqb
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheLock {
    /**
     * redis 锁key的前缀
     *
     * @return
     */
    String lockedPrefix() default "";

    /**
     * 轮询锁的时间 默认是2s
     *
     * @return
     */
    long timeOut() default 2000;

    /**
     * key在redis里存在的时间，1000S
     *
     * @return
     */
    int expireTime() default 1000;//

}

```
----
```java
/**
 * 参数级注解
 * 自定义注解
 *
 * @author zhuqb
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockedComplexObject {
    /**
     * 含有成员变量的复杂对象中需要加锁的成员变量，如一个商品对象的商品ID
     *
     * @return
     */
    String field() default "";
}

```
----
```java
/**
 * 参数级注解
 *
 * @author zhuqb
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockedObject {
    /**
     * 传入的参数值
     *
     * @return
     */
    String value() default "";
}
```
注解比较简单，下面贴上主要的AOP切入代码,在切入方法执行前进行加锁，方法执行之后释放锁
```java
/**
 * 针对 含有 @CacheLock 注解的方法 进行切入
 *
 * @author zhuqb
 */
@Component
@Aspect
@ComponentScan
@EnableAspectJAutoProxy
public class AspectAop {

    public static Logger logger = LoggerFactory.getLogger(AspectAop.class);

    @Autowired
    CacheLockService cacheLockService;

    /**
     * 对所有方法注解了CacheLock 进行拦截
     */
    @Pointcut("@annotation(com.amos.ms.type.CacheLock)")
    public void intercepter() {

    }

    /**
     * 方法执行前进行加锁
     *
     * @param joinPoint
     */
    @Before("intercepter()")
    public void doBeforeAdvice(JoinPoint joinPoint) {
        logger.info("这是前置通知");
        MethodParams params = this.getValuesFromMethod(joinPoint);
        // 加锁
        boolean lock = cacheLockService.lock(params.getKey(), params.getTimeout(), params.getExpireTime());
        if (!lock) {
            CacheLockUtils.count++;
            logger.info("获取锁失败");
            // 这里不能抛出异常 否则会造成程序死锁  不知道为什么
//            throw new CacheLockException("获取锁失败");
        }
    }

    /**
     * 后置通知  只要方法执行完成了 就会执行该操作
     *
     * @param joinPoint
     */
    @After("intercepter()")
    public void doAfterAdvice(JoinPoint joinPoint) {
        logger.info("这是后置通知");
        MethodParams params = this.getValuesFromMethod(joinPoint);
        cacheLockService.unlock(params.getKey());
    }

    /**
     * 获取锁操作 需要的参数
     *
     * @param joinPoint
     * @return
     */
    private MethodParams getValuesFromMethod(JoinPoint joinPoint) {
        // 获取所有的参数值
        Object[] paramVaules = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取 @CacheLock
        Annotation annotation = method.getAnnotation(CacheLock.class);

        // 拦截的参数
        String key = CacheLockUtils.getCacheLockKey(method,paramVaules);
        if (StringUtils.isBlank(key)) {
            throw new CacheLockException("没有需要加锁的参数");
        }

        // 对拦截的参数进行封装
        String lockKey = LockUtils.getLockKey(((CacheLock) annotation).lockedPrefix(), key);

        MethodParams params = new MethodParams();
        params.setExpireTime(((CacheLock) annotation).expireTime());
        params.setTimeout(((CacheLock) annotation).timeOut());
        params.setPrefix(((CacheLock) annotation).lockedPrefix());
        params.setKey(lockKey);
        return params;
    }

    @Data
    private static class MethodParams {
        /**
         * 拦截锁前缀
          */

        private String prefix;
        /**
         * 拦截对象
         */
        private String key;
        /**
         * 轮询时间
         */
        private long timeout;
        /**
         * 过期时间
         */
        private long expireTime;

    }
}
```
加锁采用的是向redis数据库添加键，通过判断键是否存在来判断是否已经加锁，如果已经加锁了，需要在轮询时间内看看是否释放锁，加锁的代码如下：
```java
 /**
     * 给键添加锁
     *
     * @param key
     * @param timeout 轮询时间
     * @param expireSeconds 过期时间
     * @return
     */
    @Override
    public boolean lock(String key, long timeout, long expireSeconds) {
        boolean flag = false;
        long nanoTime = System.nanoTime();
        // 轮询时间
        timeout *= MILLI_NANO_TIME;

        try{
            while (System.nanoTime() - nanoTime < timeout) {
                if (redisService.setnx(key,System.currentTimeMillis()+"",expireSeconds)) {
                    return true;
                }
                logger.info("出现锁等待");
                // 短暂休眠，避免可能的活锁
                Thread.sleep(3);
            }
        }catch (Exception e) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            flag = false;
        }
        return flag;
    }
```
释放锁即是删除redis中对应的键
```java
/**
     * 释放锁
     *
     * @param key
     * @return
     */
    @Override
    public boolean unlock(String key) {
        return redisService.del(key);
    }
```
SpringBoot可以集成对于Redis的操作
```java
@Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 设置键，并且设置过期时间
     *
     * @param key           键的名称
     * @param value         键的值
     * @param expireSeconds 过期时间 单位 ms
     * @return
     */
    @Override
    public boolean setnx(String key, String value, long expireSeconds) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key,value,expireSeconds,TimeUnit.MILLISECONDS);
    }

    /**
     * 删除键
     *
     * @param key
     * @return
     */
    @Override
    public boolean del(String key) {
        return stringRedisTemplate.delete(key);
    }
```

以上就是主要的业务代码，下面可以通过编写测试方法来测试，测试代码如下：
```java
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
```
本文是参考理解《基于redis分布式锁实现秒杀》一文进行的总结以及调整，在此感谢简书作者：lsfire，感谢分享秒杀业务类的实现思路和方法。