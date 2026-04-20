package com.sht.stsq.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 题目润色结果 VO（映射 Python 端返回的 data 字段）
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 */
@Data
public class QuestionAiOptimizeResult implements Serializable {

    /**
     * AI 优化后的标题
     */
    private String optimizedTitle;

    /**
     * AI 优化后的题目正文（Markdown 格式）
     */
    private String optimizedContent;

    /**
     * AI 优化后的标准答案（Markdown 格式）
     */
    private String optimizedAnswer;

    /**
     * 时间/空间复杂度分析（仅算法题有值，概念题为 null）
     */
    private String complexityAnalysis;

    /**
     * 面试追问 / 易错点提示
     */
    private String tips;

    private static final long serialVersionUID = 1L;
}
