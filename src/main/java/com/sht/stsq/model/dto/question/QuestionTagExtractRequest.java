package com.sht.stsq.model.dto.question;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 标签智能提取请求
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 */
@Data
public class QuestionTagExtractRequest implements Serializable {

    /**
     * 题目标题（必填）
     */
    private String title;

    /**
     * 题干内容（可选，有内容时提取更准确）
     */
    private String content;

    private static final long serialVersionUID = 1L;
}
