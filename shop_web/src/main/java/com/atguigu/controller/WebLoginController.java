package com.atguigu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebLoginController {
    @RequestMapping("login.html")
    public String loginHtml(HttpServletRequest request){
        String originalUrl = request.getParameter("originalUrl");
        request.setAttribute("originalUrl",originalUrl);
        return "login";
    }
}
