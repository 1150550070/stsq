package com.sht.stsq.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 题库 AI 健康度分析结果
 */
@Data
public class QuestionBankAiAnalyzeResult implements Serializable {

    private Integer healthScore;
    
    private Map<String, Integer> currentDistribution;
    
    private List<String> suggestedTopics;

    private static final long serialVersionUID = 1L;
}
