from fastapi import APIRouter, HTTPException
from app.schemas.question_dto import QuestionOptimizeReq, TagExtractReq, AiChatReq, BankAnalyzeReq
from app.services.ai_service import optimize_question_content, extract_tags, ai_chat, analyze_bank

router = APIRouter(prefix="/api/ai", tags=["AI 辅助功能"])

#1. 题目智能润色与增强
@router.post("/optimize-question")
async def optimize_question(req: QuestionOptimizeReq):
    try:
        # 调用 Service 层的 AI 方法 (注意加上 await，因为它是异步的)
        ai_result = await optimize_question_content(
            title=req.title,
            content=req.content,
            tags=req.tags,      # 👈 传入从 DTO 接到的 tags
            answer=req.answer   # 👈 传入从 DTO 接到的 answer
        )

        # 将 Pydantic 对象转为字典返回给 Java
        return {
            "code": 200,
            "message": "success",
            "data": ai_result.model_dump() # model_dump() 等价于 Java 的 fastjson toJSON()
        }
    except Exception as e:
        # 捕获异常，抛出 HTTP 500 错误
        raise HTTPException(status_code=500, detail=f"AI 服务调用失败: {str(e)}")


#2. 题目标签智能提取
@router.post("/extract-tags")
async def extract_tags_endpoint(req: TagExtractReq):
    """
    根据题目标题和（可选）题干内容，智能提取 3~6 个核心技术标签。
    """
    try:
        ai_result = await extract_tags(
            title=req.title,
            content=req.content,
        )
        return {
            "code": 200,
            "message": "success",
            "data": ai_result.model_dump()  # {"tags": ["Redis", "缓存击穿", ...]}
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 标签提取失败: {str(e)}")



# 3.题目专属智能答疑
@router.post("/chat")
async def chat_endpoint(req: AiChatReq):
    """
    题目专属智能答疑
    """
    try:
        # Pydantic List 转 Python dict
        chat_history_dicts = [{"role": msg.role, "content": msg.content} for msg in req.chat_history]
        
        ai_result = await ai_chat(
            title=req.title,
            content=req.content,
            answer=req.answer,
            user_message=req.user_message,
            chat_history=chat_history_dicts
        )
        return {
            "code": 200,
            "message": "success",
            "data": ai_result.model_dump() # {"ai_reply": "..."}
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 答疑调用失败: {str(e)}")


#4. 题库健康度智能分析
@router.post("/analyze-bank")
async def analyze_bank_endpoint(req: BankAnalyzeReq):
    """
    题库健康度智能分析
    """
    try:
        ai_result = await analyze_bank(
            bank_name=req.bank_name,
            question_count=req.question_count,
            tags_data=req.tags_data
        )
        return {
            "code": 200,
            "message": "success",
            "data": ai_result.model_dump() # dict form of BankAnalyzeResult
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"题库健康度分析失败: {str(e)}")


