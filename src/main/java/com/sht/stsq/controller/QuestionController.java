package com.sht.stsq.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sht.stsq.annotation.AuthCheck;
import com.sht.stsq.common.BaseResponse;
import com.sht.stsq.common.DeleteRequest;
import com.sht.stsq.common.ErrorCode;
import com.sht.stsq.common.ResultUtils;
import com.sht.stsq.constant.UserConstant;
import com.sht.stsq.exception.BusinessException;
import com.sht.stsq.exception.ThrowUtils;
import com.sht.stsq.model.dto.question.QuestionAddRequest;
import com.sht.stsq.model.dto.question.QuestionAiOptimizeRequest;
import com.sht.stsq.model.dto.question.QuestionEditRequest;
import com.sht.stsq.model.dto.question.QuestionQueryRequest;
import com.sht.stsq.model.dto.question.QuestionUpdateRequest;
import com.sht.stsq.model.vo.QuestionAiOptimizeResult;
import com.sht.stsq.model.dto.question.QuestionTagExtractRequest;
import com.sht.stsq.model.vo.QuestionTagExtractResult;
import com.sht.stsq.model.dto.questionbank.QuestionBankQueryRequest;
import com.sht.stsq.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.sht.stsq.model.entity.Question;
import com.sht.stsq.model.entity.QuestionBank;
import com.sht.stsq.model.entity.QuestionBankQuestion;
import com.sht.stsq.model.entity.User;
import com.sht.stsq.model.vo.QuestionBankVO;
import com.sht.stsq.model.vo.QuestionVO;
import com.sht.stsq.service.QuestionBankQuestionService;
import com.sht.stsq.service.QuestionBankService;
import com.sht.stsq.service.QuestionService;
import com.sht.stsq.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.draw.geom.CustomGeometry;
import org.apache.tomcat.Jar;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目接口
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;


    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);

        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, true);
        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        return ResultUtils.success(question.getId());
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);

        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //基于IP限流
        String remoteAddr = request.getRemoteAddr();
        Entry entry = null;
        try {
            entry = SphU.entry("listQuestionVOByPage", EntryType.IN, 1, remoteAddr);

            // 查询数据库
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            // 获取封装类
            return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
        }catch (Throwable ex) {
            if (!BlockException.isBlockException(ex)){
                Tracer.trace(ex);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
            }

            //降级操作
            if (ex instanceof BlockException) {
                return handleFallback(questionQueryRequest,request,ex);
            }
            //限流操作
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"访问过于频繁,请稍后再试");
        }finally {
            if (entry != null) {
                entry.exit(1, remoteAddr);
            }
        }

    }

    /**
     * listQuestionVOByPage 降级操作：直接返回本地数据
     */
    public BaseResponse<Page<QuestionVO>> handleFallback(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                             HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion

    @PostMapping("/search/page/vo")
    public BaseResponse<Page<QuestionVO>> searchQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.searchFromEs(questionQueryRequest);
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * AI 题目润色（仅管理员可用）
     * 将题目信息转发至 Python AI 边车，返回润色后的结构化结果。
     *
     * @param optimizeRequest AI 润色请求体
     * @return 润色结果 VO
     */
    @PostMapping("/ai-optimize")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<QuestionAiOptimizeResult> aiOptimizeQuestion(
            @RequestBody QuestionAiOptimizeRequest optimizeRequest) {

        ThrowUtils.throwIf(optimizeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(optimizeRequest.getTitle() == null || optimizeRequest.getTitle().isBlank(),
                ErrorCode.PARAMS_ERROR, "题目标题不能为空");

        // 构建发送给 Python 边车的请求体（字段名与 Python Pydantic schema 一致）
        JSONObject pyReqBody = new JSONObject();
        pyReqBody.set("question_id", optimizeRequest.getQuestionId() != null ? optimizeRequest.getQuestionId() : 0);
        pyReqBody.set("title", optimizeRequest.getTitle());
        pyReqBody.set("content", optimizeRequest.getContent() != null ? optimizeRequest.getContent() : "");
        pyReqBody.set("tags", optimizeRequest.getTags() != null ? optimizeRequest.getTags() : new ArrayList<>());
        pyReqBody.set("answer", optimizeRequest.getAnswer() != null ? optimizeRequest.getAnswer() : "");

        // 调用 Python 边车，超时 30s，防止大模型卡顿拖垮主线程池
        HttpResponse pyResponse;
        try {
            pyResponse = HttpRequest.post("http://127.0.0.1:8000/api/ai/optimize-question")
                    .header("Content-Type", "application/json")
                    .body(pyReqBody.toString())
                    .timeout(30_000)
                    .execute();
        } catch (Exception e) {
            log.error("AI 边车调用超时或网络异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务调用失败，请稍后重试");
        }

        if (!pyResponse.isOk()) {
            log.error("AI 边车返回异常状态码: {}, body: {}", pyResponse.getStatus(), pyResponse.body());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回异常：" + pyResponse.getStatus());
        }

        // 解析 Python 返回的 {code, message, data} 结构
        JSONObject pyResult = JSONUtil.parseObj(pyResponse.body());
        int pyCode = pyResult.getInt("code", -1);
        if (pyCode != 200) {
            String pyMsg = pyResult.getStr("message", "未知错误");
            log.error("AI 边车业务错误: {}", pyMsg);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 润色失败：" + pyMsg);
        }

        // 将 data 字段映射到 Java VO（snake_case -> camelCase 用 Hutool Bean 工具）
        JSONObject dataObj = pyResult.getJSONObject("data");
        QuestionAiOptimizeResult result = new QuestionAiOptimizeResult();
        result.setOptimizedTitle(dataObj.getStr("optimized_title"));
        result.setOptimizedContent(dataObj.getStr("optimized_content"));
        result.setOptimizedAnswer(dataObj.getStr("optimized_answer"));
        result.setComplexityAnalysis(dataObj.getStr("complexity_analysis"));
        result.setTips(dataObj.getStr("tips"));

        return ResultUtils.success(result);
    }

    /**
     * AI 标签智能提取（仅管理员可用）
     * 根据题目标题和内容，调用 Python 边车提取 3~6 个核心技术标签。
     *
     * @param extractRequest 标签提取请求体
     * @return 标签列表 VO
     */
    @PostMapping("/ai-extract-tags")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<QuestionTagExtractResult> aiExtractTags(
            @RequestBody QuestionTagExtractRequest extractRequest) {

        ThrowUtils.throwIf(extractRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(extractRequest.getTitle() == null || extractRequest.getTitle().isBlank(),
                ErrorCode.PARAMS_ERROR, "题目标题不能为空");

        // 构建发送给 Python 边车的请求体
        JSONObject pyReqBody = new JSONObject();
        pyReqBody.set("title", extractRequest.getTitle());
        pyReqBody.set("content", extractRequest.getContent() != null ? extractRequest.getContent() : "");

        // 调用 Python 边车，超时 30s
        HttpResponse pyResponse;
        try {
            pyResponse = HttpRequest.post("http://127.0.0.1:8000/api/ai/extract-tags")
                    .header("Content-Type", "application/json")
                    .body(pyReqBody.toString())
                    .timeout(30_000)
                    .execute();
        } catch (Exception e) {
            log.error("AI 边车调用超时或网络异常（标签提取）", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务调用失败，请稍后重试");
        }

        if (!pyResponse.isOk()) {
            log.error("AI 边车返回异常状态码（标签提取）: {}, body: {}", pyResponse.getStatus(), pyResponse.body());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回异常：" + pyResponse.getStatus());
        }

        // 解析 Python 返回的 {code, message, data: {tags: [...]}} 结构
        JSONObject pyResult = JSONUtil.parseObj(pyResponse.body());
        int pyCode = pyResult.getInt("code", -1);
        if (pyCode != 200) {
            String pyMsg = pyResult.getStr("message", "未知错误");
            log.error("AI 边车业务错误（标签提取）: {}", pyMsg);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 标签提取失败：" + pyMsg);
        }

        JSONObject dataObj = pyResult.getJSONObject("data");
        List<String> tags = dataObj.getJSONArray("tags").toList(String.class);

        QuestionTagExtractResult result = new QuestionTagExtractResult();
        result.setTags(tags);

        return ResultUtils.success(result);
    }

    /**
     * 第一阶段：题目专属智能答疑
     */
    @PostMapping("/ai-chat")
    public BaseResponse<com.sht.stsq.model.vo.QuestionAiChatResult> aiChat(
            @RequestBody com.sht.stsq.model.dto.question.QuestionAiChatRequest aiChatRequest) {
        
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        Long questionId = aiChatRequest.getQuestionId();
        ThrowUtils.throwIf(questionId == null || questionId <= 0, ErrorCode.PARAMS_ERROR, "题目 ID 不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(aiChatRequest.getUserMessage()), ErrorCode.PARAMS_ERROR, "提问内容不能为空");

        // 查询题目信息
        Question question = questionService.getById(questionId);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");

        // 构造传给 Python 的请求体
        JSONObject pyReqBody = new JSONObject();
        pyReqBody.set("question_id", questionId);
        pyReqBody.set("title", question.getTitle() != null ? question.getTitle() : "");
        pyReqBody.set("content", question.getContent() != null ? question.getContent() : "");
        pyReqBody.set("answer", question.getAnswer() != null ? question.getAnswer() : "");
        pyReqBody.set("user_message", aiChatRequest.getUserMessage());
        
        // 处理聊天记录
        pyReqBody.set("chat_history", aiChatRequest.getChatHistory() != null ? aiChatRequest.getChatHistory() : new java.util.ArrayList<>());

        HttpResponse pyResponse;
        try {
            pyResponse = HttpRequest.post("http://127.0.0.1:8000/api/ai/chat")
                    .header("Content-Type", "application/json")
                    .body(pyReqBody.toString())
                    .timeout(30_000)
                    .execute();
        } catch (Exception e) {
            log.error("AI 边车调用超时或网络异常（智能答疑）", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务调用失败，请稍后重试");
        }

        if (!pyResponse.isOk()) {
            log.error("AI 边车返回异常状态码（智能答疑）: {}, body: {}", pyResponse.getStatus(), pyResponse.body());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回异常：" + pyResponse.getStatus());
        }

        JSONObject pyResult = JSONUtil.parseObj(pyResponse.body());
        int pyCode = pyResult.getInt("code", -1);
        if (pyCode != 200) {
            String pyMsg = pyResult.getStr("message", "未知错误");
            log.error("AI 边车业务错误（智能答疑）: {}", pyMsg);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 提问失败：" + pyMsg);
        }

        JSONObject dataObj = pyResult.getJSONObject("data");
        com.sht.stsq.model.vo.QuestionAiChatResult result = new com.sht.stsq.model.vo.QuestionAiChatResult();
        result.setAiReply(dataObj.getStr("ai_reply"));

        return ResultUtils.success(result);
    }
}
