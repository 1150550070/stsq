package com.sht.stsq.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.sht.stsq.model.entity.QuestionChatRecord;
import org.springframework.scheduling.annotation.Async;

/**
* @author 1
* @description 针对表【question_chat_record(题目专属智能答疑记录表)】的数据库操作Service
* @createDate 2026-04-20 18:43:27
*/
public interface QuestionChatRecordService extends IService<QuestionChatRecord> {

    // 🌟 @Async 注解是核心，它让这个方法在独立的线程中执行，绝不阻塞主业务推流
    @Async
    void saveChatRecordAsync(Long questionId, Long userId, String userMessage, String aiReply);
}
