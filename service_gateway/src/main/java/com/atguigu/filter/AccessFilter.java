package com.atguigu.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AccessFilter implements GlobalFilter {
    @Value("${filter.whileFilter}")
    public String filterWhileFilter;
    @Autowired
    private RedisTemplate redisTemplate;

    //匹配路径规则
    private AntPathMatcher antPathMatcher=new AntPathMatcher();
    /**
     *
     * @param exchange 服务于web交换机,主要对请求和响应做一个交互,是一个不可改变的实例
     * @param chain 用于链式调用,设计工厂模式中的责任链模式
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求
        ServerHttpRequest request = exchange.getRequest();
        //获取响应
        ServerHttpResponse response = exchange.getResponse();
        //获取访问路径
        String path = request.getURI().getPath();
        //处理登录时仍然让登录的情况,可以从token中获取到用户id说明已经登录
        String userId = getUserId(request);
        //如果没有登录的情况下需要生成一个临时Id
        String userTempId = getUserTempId(request);
        //如果是内部接口请求不能直接访问
        if(antPathMatcher.match("/sku/**",path)){
            return writeDataToBrowser(response, RetValCodeEnum.NO_PERMISSION);
        }
        //创建过滤器的目的是为了让用户可以在不登录状态访问添加购物车以及购物车页面
        String[] whileFilters = filterWhileFilter.split(",");
        for (String whileFilter : whileFilters) {
            //此处要判断用户是否登陆
            if( path.indexOf(whileFilter) != -1 && StringUtils.isEmpty(userId)){
                //跳转到登录页面,SEE_OTHER表示跳转到其他页面
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originalUrl=" + request.getURI());
                return response.setComplete();
            }
        }
        //跳转到shop-web之前,要把数据放到request中去
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)){
            if (!StringUtils.isEmpty(userId)){
                //request.mutate方法的作用
                request.mutate().header("userId", userId);
            }
            if (!StringUtils.isEmpty(userTempId)){
                request.mutate().header("userTempId",userTempId);
            }
            return chain.filter(exchange.mutate().request(request).build());
        }
        //过滤掉addCart.html
        //如果不做拦截的话就放开请求
        return chain.filter(exchange);
    }
    //判断是否为内部访问路径,如果是内部访问路径不能直接访问
    private Mono<Void> writeDataToBrowser(ServerHttpResponse response, RetValCodeEnum retValCodeEnum) {
        response.getHeaders().add("Constant-Type","application/json");
        //要知道写什么数据给浏览器, DataBuffer
        RetVal<Object> retVal = RetVal.build(null, retValCodeEnum);
        byte[] bytes = JSONObject.toJSONString(retVal).getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(dataBuffer));
    }

    //获取临时id
    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        List<String> headerValueList = request.getHeaders().get("userTempId");
        if (!StringUtils.isEmpty(headerValueList)){
            userTempId = headerValueList.get(0);
        }else{
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if (cookie!=null){
                userTempId = cookie.getValue();
            }
        }
        return userTempId;
    }

    //参考登录模块查看userId怎么拿
    private String getUserId(ServerHttpRequest request) {
        String token = "";
        List<String> headValueList = request.getHeaders().get("token");
        if (!CollectionUtils.isEmpty(headValueList)){
            token = headValueList.get(0);
        }else{
            //如果为空就要从token中去取
            HttpCookie cookie = request.getCookies().getFirst("token");
            if (cookie!= null){
                token = cookie.getValue();
            }
        }
        String userKey = "user:login:" + token;
        JSONObject redisUserInfoJson = (JSONObject) redisTemplate.opsForValue().get(userKey);
        if(redisUserInfoJson != null){
            //网络环境改变需要重新登录,所以这里要获取IP地址
            String loginIp = redisUserInfoJson.getString("loginIp");
            //使用工具类获取当前网络的Ip地址
            String currentIp = IpUtil.getGatwayIpAddress(request);
            if (loginIp.equals(currentIp)){

                return redisUserInfoJson.getString("userId");
            }else{
                return null;
            }
        }
        return "";
    }
}
