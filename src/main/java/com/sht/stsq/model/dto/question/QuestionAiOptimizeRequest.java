package com.sht.stsq.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 题目润色请求
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 */
@Data
public class QuestionAiOptimizeRequest implements Serializable {

    /**
     * 题目 ID
     */
    private Long questionId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 题目分类（用于 AI 判断是否为算法题）
     */
    private String category;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 推荐答案（可选，有则传，提升润色质量）
     */
    private String answer;

    private static final long serialVersionUID = 1L;
}
