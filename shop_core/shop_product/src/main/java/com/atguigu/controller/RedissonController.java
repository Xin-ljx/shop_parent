package com.atguigu.controller;

import com.atguigu.exception.SleepUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/product")
public class RedissonController {
    @Autowired
    private RedissonClient redissonClient;

    /*
    * 最简单的分布式锁方式
    * 默认的是30s过期,每隔10s续期一次
    * */
    @GetMapping("/lock")
    public String lock(){
        //获取锁,名字可以随便指定
        RLock lock = redissonClient.getLock("lock");
        String uuid = UUID.randomUUID().toString();
        try {
            //上锁
            lock.lock();
            SleepUtils.sleep(50);
            System.out.println(Thread.currentThread().getName() + "在执行任务" + uuid);
        } finally {
            //解锁
            lock.unlock();
        }
        return "success";
    }

    /*
    * 读锁
    *
    * 先读才可以写读几个写几个,读读共享,读写互斥,写写互斥
    * */
    String uuid = "";

    @GetMapping("/read")
    public String read() {
        //获取锁,名字可以随便指定
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        RLock readLock = rwLock.readLock();
        try {
            readLock.lock();
            return uuid;
        } finally {
            readLock.unlock();
        }
    }
    /*
    * 写锁
    * */
    @GetMapping("/write")
    public String write() {
        //获取写锁
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        RLock writeLock = rwLock.writeLock();
        writeLock.lock();
        SleepUtils.sleep(5);
        uuid = UUID.randomUUID().toString();
        writeLock.unlock();
        return uuid;
    }
    /*
    * 出入车库
    * */
    @GetMapping("/park")
    public String park() throws Exception {
        RSemaphore parkStation = redissonClient.getSemaphore("park_station");
        parkStation.acquire(1);
        return Thread.currentThread().getName()+"找到车位";
    }

    @GetMapping("/left")
    public String left() throws Exception{
        RSemaphore parkStation = redissonClient.getSemaphore("park_station");
        parkStation.release(1);
        return Thread.currentThread().getName()+"left";
    }

    @GetMapping("/lockDoor")
    public String lockDoor() throws InterruptedException {
        RCountDownLatch leftClass = redissonClient.getCountDownLatch("left_class");
        leftClass.trySetCount(6);
        leftClass.await();
        return Thread.currentThread().getName()+"刘广辉吃屎去了";
    }
    @GetMapping("/leftClassRoom")
    public String leftClassRoom(){
        RCountDownLatch leftClass = redissonClient.getCountDownLatch("left_class");
        leftClass.countDown();
        return Thread.currentThread().getName() + "有一个人上完厕所了";
    }
    /*
    * 公平锁和非公平锁
    * */
    //公平锁
    @GetMapping("/fairLock/{id}")
    public String fairLock(@PathVariable Long id){
        RLock fairLock = redissonClient.getFairLock("fair_lock");
        fairLock.lock();
        SleepUtils.sleep(5);
        System.out.println("公平锁:"  + id);
        fairLock.unlock();
        return "success";
    }
    //非公平锁
    @GetMapping("/unFairLock/{id}")
    public String unFairLock(@PathVariable Long id){
        RLock unfairLock = redissonClient.getLock("unfair_lock");
        unfairLock.lock();
        SleepUtils.sleep(5);
        System.out.println("非公平锁"+id);
        unfairLock.unlock();
        return "success";
    }
}

