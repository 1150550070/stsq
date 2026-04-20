from pydantic import BaseModel, Field
from typing import List, Optional

# 相当于 Java 里的 public class QuestionOptimizeReqDTO
class QuestionOptimizeReq(BaseModel):
    question_id: int = Field(..., description="题目ID")
    title: str = Field(..., min_length=1, description="题目名称")
    content: str = Field(..., description="题干内容")
    tags: List[str] = Field(default=[], description="题目标签列表") # 👈 改成了 List
    answer: Optional[str] = Field(None, description="原参考答案") # 👈 新增了 answer

# AI 标签提取请求 DTO
class TagExtractReq(BaseModel):
    title: str = Field(..., min_length=1, description="题目标题")
    content: str = Field(default="", description="题干内容（可为空，有内容时提取更准确）")

# AI 专属智能答疑历史消息
class ChatMessage(BaseModel):
    role: str = Field(..., description="角色：user, assistant, 或 system")
    content: str = Field(..., description="消息内容")

# AI 专属智能答疑请求 DTO
class AiChatReq(BaseModel):
    question_id: int = Field(..., description="题目ID")
    title: str = Field(..., description="题目名称")
    content: str = Field(..., description="题干内容")
    answer: str = Field(..., description="官方给定的参考答案")
    user_message: str = Field(..., description="当前用户发送的提问")
    chat_history: List[ChatMessage] = Field(default=[], description="之前的聊天记录")

# AI 题库健康度分析请求 DTO
class BankAnalyzeReq(BaseModel):
    bank_name: str = Field(..., description="题库名称")
    question_count: int = Field(..., description="该题库内的题目总数")
    tags_data: dict = Field(default={}, description="题库中的题目标签及对应的数量统计包，例如 {'Java': 50, 'JVM': 5}")

