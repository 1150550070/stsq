from fastapi import APIRouter, HTTPException
from app.schemas.question_dto import QuestionOptimizeReq
from app.services.ai_service import optimize_question_content

router = APIRouter(prefix="/api/ai", tags=["AI 辅助功能"])

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