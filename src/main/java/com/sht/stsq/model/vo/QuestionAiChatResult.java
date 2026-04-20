package com.sht.stsq.model.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 专属智能答疑响应视图
 */
@Data
public class QuestionAiChatResult implements Serializable {

    /**
     * AI 生成的回答内容
     */
    private String aiReply;

    private static final long serialVersionUID = 1L;
}
