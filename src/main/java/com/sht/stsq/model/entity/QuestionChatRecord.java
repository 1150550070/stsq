package com.sht.stsq.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 题目专属智能答疑记录表
 * @TableName question_chat_record
 */
@TableName(value ="question_chat_record")
@Data
public class QuestionChatRecord {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目 id
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 用户 id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色：user(用户) / assistant(AI)
     */
    private String role;

    /**
     * 对话内容
     */
    private String content;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;


}