package com.sht.stsq.esdao;

import com.sht.stsq.model.dto.question.QuestionEsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 题目 ES 操作
 */
public interface QuestionEsDao
        extends ElasticsearchRepository<QuestionEsDTO, Long> {

    /**
     * RAG 检索底层方法：根据关键字全文搜索相关题目
     * 采用 ES 的 multi_match 多字段查询，并赋予不同权重：
     * title^3: 标题命中权重最高 (x3)
     * tags^2: 标签命中权重次之 (x2)
     * content: 正文命中权重正常 (x1)
     * answer: 答案命中权重正常 (x1)
     *
     * @param keyword  用户的搜索关键字（通常是用户的提问）
     * @param pageable 分页对象，用于控制返回的条数
     * @return ES 检索到的分页结果
     */
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"tags^2\", \"content\", \"answer\"]}}]}}")
    Page<QuestionEsDTO> findByKeyword(String keyword, Pageable pageable);

    /**
     * RAG 检索便捷方法：获取 Top N 最相关的扩展题目
     * (提供给 Controller 层直接调用，完美兼容 questionEsDao.searchByKeyword(userMessage, 4))
     *
     * @param keyword 用户的提问
     * @param limit   需要限制返回的条数
     * @return 最相关的题目列表
     */
    default List<QuestionEsDTO> searchByKeyword(String keyword, int limit) {
        // 构建 PageRequest：第 0 页，每页 limit 条
        PageRequest pageRequest = PageRequest.of(0, limit);
        // 执行底层的 ES 查询并提取数据列表
        return findByKeyword(keyword, pageRequest).getContent();
    }


}

