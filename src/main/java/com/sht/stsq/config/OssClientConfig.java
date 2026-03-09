package com.sht.stsq.config;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云对象存储客户端
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 * 
 */
@Configuration
@ConfigurationProperties(prefix = "oss.client")
@Data
public class OssClientConfig {

    /**
     * 域名
     */
    private String host;

    /**
     * accessKey
     */
    private String accessKeyId;

    /**
     * secretKey
     */
    private String accessKeySecret;

    /**
     * 区域
     */
    private String region;

    /**
     * 桶名
     */
    private String bucket;

    @Bean
    public OSS ossClient() {
        // 初始化用户身份信息(accessKeyId, accessKeySecret)
        // 设置bucket的区域，OSS地域的简称请参照 https://help.aliyun.com/document_detail/140601.html
        String endpoint = String.format("https://%s", host);
        // 生成OSS客户端
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}