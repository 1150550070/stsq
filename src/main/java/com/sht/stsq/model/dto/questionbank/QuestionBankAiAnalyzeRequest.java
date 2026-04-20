package com.sht.stsq.model.dto.questionbank;

import lombok.Data;
import java.io.Serializable;

/**
 * 题库 AI 健康度分析请求
 */
@Data
public class QuestionBankAiAnalyzeRequest implements Serializable {

    /**
     * 题库 id
     */
    private Long bankId;

    private static final long serialVersionUID = 1L;
}
