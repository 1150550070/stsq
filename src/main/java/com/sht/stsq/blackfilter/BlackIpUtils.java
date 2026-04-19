package com.sht.stsq.blackfilter;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class BlackIpUtils {

    private static BitMapBloomFilter bloomFilter;

    //判断ip在不在ip黑名单内
    public static boolean isBlackIp(String ip) {
        return bloomFilter.contains(ip);
    }

    //重建ip黑名单

    public static void rebuildBlackIp(String configInfo) {
        if (StrUtil.isBlank(configInfo)) {
            configInfo = "{}";
        }
        // 解析 yaml 文件
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(configInfo, Map.class);
        // 获取 ip 黑名单
        List<String> blackIpList = new ArrayList<>();
        Object ipObj = map.get("blackIpList");

        if (ipObj instanceof List) {
            // 情况 A：YAML 格式完全正确，成功解析为 List
            blackIpList = (List<String>) ipObj;
        } else if (ipObj instanceof String) {
            // 情况 B：因为 Tab 或格式问题被解析成了 String，咱们手动切割
            String ipStr = (String) ipObj;
            // 去除多余的字符（例如双引号、中括号、回车等）
            ipStr = ipStr.replace("[", "").replace("]", "").replace("\"", "").replace("\r", "");
            // 按逗号或换行符进行切割
            String[] ips = ipStr.split("[,\\n]+");
            for (String ip : ips) {
                if (StrUtil.isNotBlank(ip.trim())) {
                    blackIpList.add(ip.trim());
                }
            }
        }
        // 加锁防止并发
        synchronized (BlackIpUtils.class) {
            if (CollectionUtil.isNotEmpty(blackIpList)) {
                // 注意构造参数的设置
                BitMapBloomFilter bitMapBloomFilter = new BitMapBloomFilter(100);
                for (String ip : blackIpList) {
                    bitMapBloomFilter.add(ip.trim());
                }
                bloomFilter = bitMapBloomFilter;
            } else {
                bloomFilter = new BitMapBloomFilter(100);
            }
        }

    }
}
