package com.atguigu.redis_consumer;

import com.atguigu.constant.RedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SecKillMsgReciver {
    //把的到的消息放入到内存中去
    //public Map<String,String> seckillState=new HashMap<>();
    @Autowired
    private RedisTemplate redisTemplate;

    public void receiveChannelMessage(String message){
        message=message.replaceAll("\"","");
        String[] splitMessage = message.split(":");
        if(splitMessage.length==2){
            //seckillState.put(RedisConst.SECKILL_STATE_PREFIX+splitMessage[0],splitMessage[1]);
            redisTemplate.opsForValue().set(RedisConst.SECKILL_STATE_PREFIX+splitMessage[0],splitMessage[1]);
        }
    }
}
