package com.sht.stsq.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sht.stsq.annotation.AuthCheck;
import com.sht.stsq.common.BaseResponse;
import com.sht.stsq.common.DeleteRequest;
import com.sht.stsq.common.ErrorCode;
import com.sht.stsq.common.ResultUtils;
import com.sht.stsq.constant.UserConstant;
import com.sht.stsq.exception.BusinessException;
import com.sht.stsq.exception.ThrowUtils;
import com.sht.stsq.model.dto.question.QuestionQueryRequest;
import com.sht.stsq.model.dto.questionbank.QuestionBankAddRequest;
import com.sht.stsq.model.dto.questionbank.QuestionBankEditRequest;
import com.sht.stsq.model.dto.questionbank.QuestionBankQueryRequest;
import com.sht.stsq.model.dto.questionbank.QuestionBankUpdateRequest;
import com.sht.stsq.model.entity.Question;
import com.sht.stsq.model.entity.QuestionBank;
import com.sht.stsq.model.entity.User;
import com.sht.stsq.model.vo.QuestionBankVO;
import com.sht.stsq.model.vo.QuestionVO;
import com.sht.stsq.service.QuestionBankService;
import com.sht.stsq.service.QuestionService;
import com.sht.stsq.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题库接口
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;

    // region 增删改查

    /**
     * 创建题库
     *
     * @param questionBankAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        //将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankAddRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    /**
     * 删除题库
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionBankUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest) {
        if (questionBankUpdateRequest == null || questionBankUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankUpdateRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        // 判断是否存在
        long id = questionBankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库（封装类）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionBankQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionBankQueryRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);

        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);

        // 是否要关联查询题库下的题目列表
        boolean needQueryQuestionList = questionBankQueryRequest.isNeedQueryQuestionList();
        if (needQueryQuestionList) {
            QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
            questionQueryRequest.setQuestionBankId(id);
            // 可以按需支持更多的题目搜索参数，比如分页
            questionQueryRequest.setPageSize(questionBankQueryRequest.getPageSize());
            questionQueryRequest.setCurrent(questionBankQueryRequest.getCurrent());
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            Page<QuestionVO> questionVOPage = questionService.getQuestionVOPage(questionPage, request);
            questionBankVO.setQuestionPage(questionVOPage);
        }
        // 获取封装类
        return ResultUtils.success(questionBankVO);
    }

    /**
     * 分页获取题库列表（仅管理员可用）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        return ResultUtils.success(questionBankPage);
    }

    /**
     * 分页获取题库列表（封装类）
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    @SentinelResource(value = "listQuestionBankVOByPage",
            blockHandler = "handleBlockException",
            fallback = "handleFallback")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                       HttpServletRequest request) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * listQuestionBankVOByPage 降级操作：直接返回本地数据
     */
    public BaseResponse<Page<QuestionBankVO>> handleFallback(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                             HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }

    /**
     * listQuestionBankVOByPage 流控操作
     * 限流：提示“系统压力过大，请耐心等待”
     * 熔断：执行降级操作
     */
    public BaseResponse<Page<QuestionBankVO>> handleBlockException(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                   HttpServletRequest request, BlockException ex) {
        // 降级操作
        if (ex instanceof DegradeException) {
            return handleFallback(questionBankQueryRequest, request, ex);
        }
        // 限流操作
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统压力过大，请耐心等待");
    }



    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionBankQueryRequest.setUserId(loginUser.getId());
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * 编辑题库（给用户使用）
     *
     * @param questionBankEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest, HttpServletRequest request) {
        if (questionBankEditRequest == null || questionBankEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankEditRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionBankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 第一阶段：题库健康度智能分析（宏观管理）
     */
    @PostMapping("/ai-analyze")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<com.sht.stsq.model.vo.QuestionBankAiAnalyzeResult> aiAnalyze(
            @RequestBody com.sht.stsq.model.dto.questionbank.QuestionBankAiAnalyzeRequest analyzeRequest) {
        
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        Long bankId = analyzeRequest.getBankId();
        ThrowUtils.throwIf(bankId == null || bankId <= 0, ErrorCode.PARAMS_ERROR, "题库 ID 不合法");

        QuestionBank questionBank = questionBankService.getById(bankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");

        // 查询该题库下的所有题目
        QuestionQueryRequest queryRequest = new QuestionQueryRequest();
        queryRequest.setQuestionBankId(bankId);
        queryRequest.setPageSize(10000); // 拉取全部题目用于统计
        Page<Question> questionPage = questionService.listQuestionByPage(queryRequest);
        java.util.List<Question> questions = questionPage.getRecords();

        int questionCount = questions.size();
        
        // 统计 tags 数据
        java.util.Map<String, Integer> tagsData = new java.util.HashMap<>();
        for (Question q : questions) {
            String tagsStr = q.getTags();
            if (cn.hutool.core.util.StrUtil.isNotBlank(tagsStr)) {
                try {
                    java.util.List<String> tags = cn.hutool.json.JSONUtil.toList(tagsStr, String.class);
                    for (String tag : tags) {
                        tagsData.put(tag, tagsData.getOrDefault(tag, 0) + 1);
                    }
                } catch (Exception e) {
                    log.error("解析题目 tags 异常, questionId: {}", q.getId(), e);
                }
            }
        }

        // 调用 Python 边车
        cn.hutool.json.JSONObject pyReqBody = new cn.hutool.json.JSONObject();
        pyReqBody.set("bank_name", questionBank.getTitle());
        pyReqBody.set("question_count", questionCount);
        pyReqBody.set("tags_data", tagsData);

        cn.hutool.http.HttpResponse pyResponse;
        try {
            pyResponse = cn.hutool.http.HttpRequest.post("http://127.0.0.1:8000/api/ai/analyze-bank")
                    .header("Content-Type", "application/json")
                    .body(pyReqBody.toString())
                    .timeout(30_000)
                    .execute();
        } catch (Exception e) {
            log.error("AI 边车调用超时或网络异常（题库健康度分析）", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务调用失败，请稍后重试");
        }

        if (!pyResponse.isOk()) {
            log.error("AI 边车返回异常状态码（健康度分析）: {}, body: {}", pyResponse.getStatus(), pyResponse.body());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务返回异常：" + pyResponse.getStatus());
        }

        cn.hutool.json.JSONObject pyResult = cn.hutool.json.JSONUtil.parseObj(pyResponse.body());
        int pyCode = pyResult.getInt("code", -1);
        if (pyCode != 200) {
            String pyMsg = pyResult.getStr("message", "未知错误");
            log.error("AI 边车业务错误（健康度分析）: {}", pyMsg);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题库健康度分析失败：" + pyMsg);
        }

        cn.hutool.json.JSONObject dataObj = pyResult.getJSONObject("data");
        com.sht.stsq.model.vo.QuestionBankAiAnalyzeResult result = new com.sht.stsq.model.vo.QuestionBankAiAnalyzeResult();
        result.setHealthScore(dataObj.getInt("health_score"));
        
        // 解析 current_distribution
        cn.hutool.json.JSONObject distObj = dataObj.getJSONObject("current_distribution");
        java.util.Map<String, Integer> distMap = new java.util.HashMap<>();
        if (distObj != null) {
            for (String key : distObj.keySet()) {
                distMap.put(key, distObj.getInt(key));
            }
        }
        result.setCurrentDistribution(distMap);
        
        // 解析 suggested_topics
        cn.hutool.json.JSONArray topicsArr = dataObj.getJSONArray("suggested_topics");
        java.util.List<String> topics = new java.util.ArrayList<>();
        if (topicsArr != null) {
            topics = topicsArr.toList(String.class);
        }
        result.setSuggestedTopics(topics);

        return ResultUtils.success(result);
    }

    // endregion
}

