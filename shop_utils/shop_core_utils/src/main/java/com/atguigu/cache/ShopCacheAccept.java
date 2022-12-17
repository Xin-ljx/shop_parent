package com.atguigu.cache;


import com.atguigu.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class ShopCacheAccept {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter skuBloomFilter;

    //1给shopCache添加一个环绕通知
//    @Around(value = "@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint target) throws Throwable {
        //获取到目标方法上的参数
        Object[] methodParams = target.getArgs();
        //拿到目标方法
        MethodSignature methodSignature = (MethodSignature) target.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //拿到注解上的参数
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        String prefix = shopCache.value();
        //拼接缓存key的名称
        Object firstParam = methodParams[0];
        //拼接字符串,redis中的名称
        String cacheKey = prefix + ":" + firstParam;
        //获取缓存内容
        Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
        //判断redis是否为空,若redis为空则从数据库获取数据
        if (cacheObject == null){
            String lockKey = "lock-" + firstParam;
            synchronized (lockKey.intern()){
                if (cacheObject==null){
                    //执行目标方法
                    Object objectDb = target.proceed();
                    //把数据放入缓存
                    redisTemplate.opsForValue().set(cacheKey,objectDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return objectDb;
                }
            }
        }
        return cacheObject;
    }
    //切面编程+双重检查
    //@Around(value = "@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice1(ProceedingJoinPoint target) throws Throwable {
        Object cacheObject = null;
                //获取到目标方法上的参数
        Object[] methodParams = target.getArgs();
        //拿到目标方法
        MethodSignature methodSignature = (MethodSignature) target.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //拿到注解上的参数
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        String prefix = shopCache.value();
        //拼接缓存key的名称
        Object firstParam = methodParams[0];
        //拼接字符串,redis中的名称
        String cacheKey = prefix + ":" + firstParam;
        //获取缓存内容
        cacheObject = redisTemplate.opsForValue().get(cacheKey);
        //判断redis是否为空,若redis为空则从数据库获取数据
        //第一个作用是判断是否需要加锁
        if (cacheObject == null) {
            String lockKey = "lock-" + firstParam;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                lock.lock();
                //再次获取redis中的信息,判断是否为空
                cacheObject = redisTemplate.opsForValue().get(cacheKey);
                //第二个所用是判断是否需要执行目标方法
                if (cacheObject == null) {
                    boolean contains = skuBloomFilter.contains(firstParam);
                    if (contains) {
                        //执行目标方法
                        Object objectDb = target.proceed();
                        //把数据放入缓存
                        redisTemplate.opsForValue().set(cacheKey, objectDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return objectDb;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return cacheObject;
    }
    /*
    * 读写锁
    * */
    //@Around(value = "@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice2(ProceedingJoinPoint target) throws Throwable {
        //获取到目标方法上的参数
        Object[] methodParams = target.getArgs();
        //拿到目标方法
        MethodSignature methodSignature = (MethodSignature) target.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //拿到注解上的参数
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        String prefix = shopCache.value();
        //拼接缓存key的名称
        Object firstParam = methodParams[0];
        //拼接字符串,redis中的名称
        String cacheKey = prefix + ":" + firstParam;
        //判断redis是否为空,若redis为空则从数据库获取数据
        String lockKey = "lock-" + firstParam;
        //获取读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        try {
            //读锁
            readWriteLock.readLock().lock();
            Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
            readWriteLock.readLock().unlock();
            if (cacheObject==null){
                readWriteLock.writeLock().lock();
                boolean flag = skuBloomFilter.contains(firstParam);
                if (flag){
                    //能进来说明拿到锁了就执行目标方法
                    //执行目标方法
                    Object objectDb = target.proceed();
                    //把数据放入缓存
                    redisTemplate.opsForValue().set(cacheKey, objectDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    readWriteLock.writeLock().unlock();
                    return objectDb;
                }else{
                    return cacheObject;
                }
            }
            //为了下面finally在此加锁
            readWriteLock.readLock().lock();
            readWriteLock.writeLock().lock();
        } finally {
            readWriteLock.readLock().unlock();
            readWriteLock.writeLock().unlock();
        }
        return null;
    }
    //本地锁+判断是否开启布隆
    @Around(value = "@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice3(ProceedingJoinPoint target) throws Throwable {
        //获取到目标方法上的参数
        Object[] methodParams = target.getArgs();
        //拿到目标方法
        MethodSignature methodSignature = (MethodSignature) target.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //拿到注解上的参数
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        String prefix = shopCache.value();
        //拼接缓存key的名称
        Object firstParam = methodParams[0];
        //拼接字符串,redis中的名称
        String cacheKey = prefix + ":" + firstParam;
        //获取缓存内容
        Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
        //判断redis是否为空,若redis为空则从数据库获取数据
        if (cacheObject == null){
            String lockKey = "lock-" + firstParam;
            synchronized (lockKey.intern()){
                if (cacheObject==null){
                    boolean enableBloom = shopCache.enableBloom();
                    Object objectDb = null;
                    if (enableBloom){
                        //能进来说明开启bloom
                        boolean flag = skuBloomFilter.contains(firstParam);
                        if (flag){
                            //能进来说明有缓存
                            //执行目标方法
                            objectDb = target.proceed();
                        }
                    }else{
                        //执行目标方法
                        objectDb = target.proceed();
                    }
                    //把数据放入缓存
                    redisTemplate.opsForValue().set(cacheKey,objectDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return objectDb;
                }
            }
        }
        return cacheObject;
    }
}
