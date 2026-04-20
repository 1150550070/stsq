package com.sht.stsq.model.dto.question;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * AI 专属智能答疑请求
 */
@Data
public class QuestionAiChatRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 用户的新消息
     */
    private String userMessage;

    /**
     * 对话历史记录列表，每个 map 需要含 role 和 content
     */
    private List<Map<String, String>> chatHistory;

    private static final long serialVersionUID = 1L;
}
