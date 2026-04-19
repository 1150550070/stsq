from pydantic import BaseModel, Field
from typing import List, Optional

# 相当于 Java 里的 public class QuestionOptimizeReqDTO
class QuestionOptimizeReq(BaseModel):
    question_id: int = Field(..., description="题目ID")
    title: str = Field(..., min_length=1, description="题目名称")
    content: str = Field(..., description="题干内容")
    tags: List[str] = Field(default=[], description="题目标签列表") # 👈 改成了 List
    answer: Optional[str] = Field(None, description="原参考答案") # 👈 新增了 answer