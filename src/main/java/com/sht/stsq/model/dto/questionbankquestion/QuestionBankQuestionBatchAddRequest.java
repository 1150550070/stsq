package com.sht.stsq.model.dto.questionbankquestion;


import lombok.Data;


import java.io.Serializable;

import java.util.List;

@Data
public class QuestionBankQuestionBatchAddRequest implements Serializable {


    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id
     */
    private List<Long> questionIds;


    private static final long serialVersionUID = 1L;
}
