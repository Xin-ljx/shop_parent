package com.atguigu.controller;

import com.atguigu.service.BaseBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product/")
public class ConCurrentController {
    @Autowired
    private BaseBrandService baseBrandService;

    @GetMapping("/setNum")
    public String SetNum(){
        baseBrandService.setNum();
        return "success";
    }
}
