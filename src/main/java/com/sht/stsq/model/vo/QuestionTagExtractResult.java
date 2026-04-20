package com.sht.stsq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 标签智能提取结果 VO（映射 Python 端返回的 data 字段）
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 */
@Data
public class QuestionTagExtractResult implements Serializable {

    /**
     * AI 提取的技术标签列表（3~6 个）
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
