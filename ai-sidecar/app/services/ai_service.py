from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
# 🌟 关键修改 1：引入 PydanticOutputParser
from langchain_core.output_parsers import PydanticOutputParser
from app.core.config import settings
from typing import Optional, List

class QuestionOptimizeResult(BaseModel):
    optimized_title: str = Field(description="优化和润色后的题目名称，要专业简练")
    optimized_content: str = Field(description="优化后的正文内容，使用 Markdown 排版")
    optimized_answer: str = Field(description="优化、扩充、重新排版后的标准答案，必须使用 Markdown") # 👈 新增
    # 🌟 关键修改：改为 Optional (可选)，并明确告知什么时候填，什么时候不填
    complexity_analysis: Optional[str] = Field(
        default=None,
        description="【仅针对涉及代码实现的算法题填写】时间与空间复杂度分析。如果是纯概念、理论、系统设计题，请务必返回 null。"
    )
    tips: str = Field(description="给面试官的追问提示，或求职者的易错点")

llm = ChatOpenAI(
    api_key=settings.API_KEY,
    base_url=settings.BASE_URL,
    model=settings.MODEL_NAME,
    temperature=0.3
)

# 🌟 关键修改 2：实例化解析器
parser = PydanticOutputParser(pydantic_object=QuestionOptimizeResult)

async def optimize_question_content(title: str, content: str, tags: List[str], answer: str = None):

    prompt = ChatPromptTemplate.from_messages([
        ("system", """你是一个资深的 BAT 级别大厂面试官和题库架构师。
        你的任务是对录入的草稿面试题进行专业润色和维度增强。
        
        【处理规则】
        1. 优化排版：请修正错别字，使用严谨的技术术语，所有输出（尤其是答案）请使用结构清晰的 Markdown 格式。
        2. 答案增强：在原答案基础上进行扩充，补充核心原理或底层实现。
        3. 题型判断与复杂度分析：
           - 如果题目是【算法题】或明确要求写代码的【手撕题】（如：两数之和、手写单例模式）：必须输出准确的时间复杂度和空间复杂度分析。
           - 如果题目是【纯概念题】或【理论知识题】（如：解释 JS 变量提升、什么是 Redis 雪崩）：请直接跳过复杂度分析，对应的 JSON 字段必须填 null。
        4. 面试指南：提炼出该题的核心易错点，或提供 1-2 个拔高的连环追问。
        
        \n{format_instructions}"""),
        # 2. 清理 Human 提示词，保持和入参一致
        ("human", "题目标签：{tags}\n草稿标题：{title}\n草稿正文：{content}\n草稿答案：{answer}")
    ])

    chain = prompt | llm | parser

    # 3. 更新触发请求时的参数绑定
    result = await chain.ainvoke({
        "tags": ", ".join(tags) if tags else "无", # 把列表转成逗号分隔的字符串给 AI 看
        "title": title,
        "content": content,
        "answer": answer if answer else "无参考答案，请根据题干自行生成", # 处理答案为空的情况
        "format_instructions": parser.get_format_instructions()
    })

    return result