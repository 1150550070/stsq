package com.sht.stsq.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.sht.stsq.config.OssClientConfig;
import java.io.File;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Oss 对象存储操作
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 * 
 */
@Component
@Slf4j
public class OssManager {

    @Autowired
    private OssClientConfig ossClientConfig;

    @Autowired
    private OSS ossClient;

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param localFilePath 本地文件路径
     * @return
     */
    public PutObjectResult putObject(String key, String localFilePath) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossClientConfig.getBucket(), key,
                new File(localFilePath));
        return ossClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 OSS 并返回访问 URL
     *
     * @param key  OSS对象键（完整路径名）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        try {
            // 上传文件
            PutObjectResult result = putObject(key, file.getAbsolutePath());
            if (result != null) {
                // 构建访问URL - 使用标准的OSS访问格式
                // 确保key不以/开头，避免双斜杠
                String cleanKey = key.startsWith("/") ? key.substring(1) : key;
                // 阿里云OSS标准URL格式：https://bucket-name.endpoint/object-key
                String url = String.format("https://%s.%s/%s",
                        ossClientConfig.getBucket(),
                        ossClientConfig.getHost(),
                        cleanKey);
                log.info("生成的OSS访问URL: {}", url);
                log.info("文件上传OSS成功: {} -> {}", file.getName(), url);
                return url;
            } else {
                log.error("文件上传OSS失败，返回结果为空");
                return null;
            }
        } catch (Exception e) {
            log.error("文件上传OSS异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
