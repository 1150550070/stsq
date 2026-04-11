package com.sht.stsq.mapper;

import com.sht.stsq.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author 1
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2025-11-19 18:18:38
* @Entity com.sht.stsq.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 查询题目列表（包括已被删除的数据）
     */
    @Select("select * from question where updateTime >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);
}





