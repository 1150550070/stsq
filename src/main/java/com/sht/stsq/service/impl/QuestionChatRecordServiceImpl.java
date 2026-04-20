package com.sht.stsq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sht.stsq.model.entity.QuestionChatRecord;
import com.sht.stsq.service.QuestionChatRecordService;
import com.sht.stsq.mapper.QuestionChatRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
* @author 1
* @description 针对表【question_chat_record(题目专属智能答疑记录表)】的数据库操作Service实现
* @createDate 2026-04-20 18:43:27
*/
@Service
@Slf4j
public class QuestionChatRecordServiceImpl extends ServiceImpl<QuestionChatRecordMapper, QuestionChatRecord>
    implements QuestionChatRecordService{

    // 🌟 @Async 注解是核心，它让这个方法在独立的线程中执行，绝不阻塞主业务推流
    @Async
    @Override
    public void saveChatRecordAsync(Long questionId, Long userId, String userMessage, String aiReply) {
        try {
            // 安全校验
            if (StrUtil.isBlank(userMessage) || StrUtil.isBlank(aiReply)) {
                log.warn("异步存库跳过：存在空消息。提问长度={}, AI回复长度={}",
                        userMessage != null ? userMessage.length() : 0,
                        aiReply != null ? aiReply.length() : 0);
                return;
            }

            log.info("开始异步写入对话记录到 MySQL (题目ID: {}, 用户ID: {})...", questionId, userId);

            // 1. 构造用户的提问记录
            QuestionChatRecord userRecord = new QuestionChatRecord();
            userRecord.setQuestionId(questionId);
            userRecord.setUserId(userId);
            userRecord.setRole("user");
            userRecord.setContent(userMessage);

            // 2. 构造 AI 的回复记录
            QuestionChatRecord aiRecord = new QuestionChatRecord();
            aiRecord.setQuestionId(questionId);
            aiRecord.setUserId(userId);
            aiRecord.setRole("assistant");
            aiRecord.setContent(aiReply);

            // 3. 批量插入数据库
            boolean success = this.saveBatch(Arrays.asList(userRecord, aiRecord));
            if (success) {
                log.info("✅ 异步对话记录存库成功！");
            } else {
                log.error("❌ 异步对话记录存库失败：MyBatis-Plus saveBatch 返回 false");
            }

        } catch (Exception e) {
            // 🌟 拦截一切数据库字段映射错误、主键冲突等异常并打印到控制台
            log.error("❌ 异步对话存库过程中发生严重异常：", e);
        }
    }
}




