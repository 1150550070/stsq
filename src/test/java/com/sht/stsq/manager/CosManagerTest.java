package com.sht.stsq.manager;

import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Oss 操作测试
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 * 
 */
@SpringBootTest
class OssManagerTest {

    @Resource
    private OssManager ossManager;

    @Test
    void putObject() {
        ossManager.putObject("test", "test.json");
    }
}