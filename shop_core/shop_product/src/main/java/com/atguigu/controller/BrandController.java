package com.atguigu.controller;


import com.atguigu.entity.BaseBrand;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.atguigu.utils.MinioUploader;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-10-31
 */
@RestController
@RequestMapping("/product/brand")
public class BrandController {
    @Autowired
    private BaseBrandService baseBrandService;
    @Autowired
    private MinioUploader minioUploader;
    //分页查询品牌列表
    //http://127.0.0.1:8000/product/brand/queryBrandByPage/1/10
    @GetMapping("/queryBrandByPage/{pageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Long pageNum,
                                   @PathVariable Long pageSize){
        Page<BaseBrand> page = new Page<>(pageNum,pageSize);
        baseBrandService.page(page, null);
        return RetVal.ok(page);
    }
    //添加品牌列表
    //http://127.0.0.1/product/brand
    @PostMapping()
    public RetVal insert(@RequestBody BaseBrand baseBrand){
        baseBrandService.save(baseBrand);
        return RetVal.ok();
    }
    //修改品牌列表
    //http://127.0.0.1/product/brand/1
    @PutMapping()
    public RetVal update(@RequestBody BaseBrand baseBrand){
        baseBrandService.updateById(baseBrand);
        return RetVal.ok();
    }

    //删除品牌列表
    //http://127.0.0.1/product/brand/4
    @DeleteMapping("/{brandId}")
    public RetVal delete(@PathVariable Long brandId){
        baseBrandService.removeById(brandId);
        return RetVal.ok();
    }

    //根据id查询品牌信息
    //http://127.0.0.1/product/brand/4
    @GetMapping("{brandId}")
    public RetVal saveBrand(@PathVariable Long brandId) {
        BaseBrand brand = baseBrandService.getById(brandId);
        return RetVal.ok(brand);
    }

    //查询所有的品牌
    @GetMapping("getAllBrand")
    public RetVal getAllBrand() {
        List<BaseBrand> brandList = baseBrandService.list(null);
        return RetVal.ok(brandList);
    }
    //文件上传minIO方式
    //http://api.gmall.com/product/brand/fileUpload
    @PostMapping("/fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {
        String retUrl = minioUploader.uploadFile(file);
        return RetVal.ok(retUrl);
    }
    @GetMapping("/getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId) {
        BaseBrand brand = baseBrandService.getById(brandId);
        return brand;
    }
        //文件上传fastdfs方式
    //http://api.gmall.com/product/brand/fileUpload
/*    @PostMapping("/fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception{
        //需要一个配置文件告诉fastdfs在哪里
        String configFilePath = this.getClass().getResource("/tracker.conf").getFile();
        //初始化
        ClientGlobal.init(configFilePath);
        //创建trackerClient 客户端
        TrackerClient trackerClient = new TrackerClient();
        //用trackerClient获取连接
        TrackerServer trackerServer = trackerClient.getConnection();
        //创建StorageClient1
        StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
        //对文件实现上传
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String url = storageClient1.upload_appender_file1(file.getBytes(), extension, null);
        //这个前缀需要自己拼接 拼接的地址是有可能变化的 所以说最好写到配置文件 作业
        String path = "http://" + FastdfsUtils.URL + "/" + url;
        return RetVal.ok(path);
    }*/


}

