from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
# 🌟 关键修改 1：引入 PydanticOutputParser
from langchain_core.output_parsers import PydanticOutputParser
from app.core.config import settings
from typing import Optional, List

# ────────────────────────────────────────────────
# 功能 1：题目智能润色与增强
# ────────────────────────────────────────────────
class QuestionOptimizeResult(BaseModel):
    optimized_title: str = Field(description="优化和润色后的题目名称，要专业简练")
    optimized_content: str = Field(description="优化后的正文内容，使用 Markdown 排版")
    optimized_answer: str = Field(description="优化、扩充、重新排版后的标准答案，必须使用 Markdown") # 👈 新增
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


# ────────────────────────────────────────────────
# 功能 2：标签智能提取
# ────────────────────────────────────────────────

class TagExtractResult(BaseModel):
    tags: List[str] = Field(
        description="从题目中提取的技术标签列表，3~6 个，每个标签尽量简短（2~8 个汉字或英文单词），"
                    "精准反映题目的核心技术考点，例如：['Redis', '缓存击穿', '分布式锁', '高并发']"
    )

tag_parser = PydanticOutputParser(pydantic_object=TagExtractResult)

async def extract_tags(title: str, content: str = "") -> TagExtractResult:
    """
    根据题目标题和内容，智能提取 3~6 个精准技术标签。
    """
    prompt = ChatPromptTemplate.from_messages([
        ("system", """你是一个资深的技术面试题库架构师，精通后端、前端、算法、系统设计等各类技术领域。
你的任务是：根据用户提供的面试题标题和内容，准确提取出该题目涉及的核心技术考点，并以标签列表的形式返回。

【标签提取规则】
1. 数量：提取 3~6 个标签，不能多也不能少。
2. 精准性：标签必须直接反映题目的技术考点，禁止使用"面试题"、"编程"、"技术"等无意义的通用词。
3. 粒度：优先使用具体的技术名词，例如使用"缓存击穿"而非"缓存问题"，使用"LRU算法"而非"算法"。
4. 格式：标签尽量简短（2~8个字/词），首字母大写（英文）或使用行业通用简称。
5. 语言：中英文混合，以题目内容为准，Java/Redis/MySQL 等专有名词保留英文。

{format_instructions}"""),
        ("human", "题目标题：{title}\n题目内容：{content}")
    ])

    chain = prompt | llm | tag_parser

    result = await chain.ainvoke({
        "title": title,
        "content": content if content else "（无内容，仅根据标题提取）",
        "format_instructions": tag_parser.get_format_instructions()
    })

    return result


# ────────────────────────────────────────────────
# 功能 3：题目专属智能答疑 (纯流式改造完毕)
# ────────────────────────────────────────────────
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from typing import List
# 删除了 Pydantic 和 OutputParser 相关的导入，因为不需要 JSON 了

async def ai_chat_stream(title: str, content: str, answer: str, user_message: str, chat_history: List[dict] = []):
    """
    根据题目上下文和历史对话，流式回答用户的提问，严防跑题。
    chat_history: [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
    """
    # 构建基础 System 提示词 (去掉了 format_instructions，明确要求输出 Markdown)
    system_prompt = f"""你是一个友好的面试辅导导师，专门解答用户在刷题过程中遇到的疑问。

【当前题目上下文】
标题：{title}
内容：{content}
官方解析：{answer}

【你的职责规则】
1. 你的回答必须严格基于上述【当前题目上下文】。
2. 对于此题目相关的问题，你需要热心、通俗易懂地解答，必要时可以补充代码示例或底层原理。
3. 如果用户的提问完全脱离了当前这道题（比如问“今天天气怎么样”、“怎么写个高并发电商系统”），请委婉地拒绝回答并提醒用户聚焦于当前题目。
4. 请直接使用 Markdown 排版回答，不要包含任何 JSON 结构。"""

    messages = [SystemMessage(content=system_prompt)]

    # 载入历史记录
    for msg in chat_history:
        role = msg.get("role", "")
        msg_content = msg.get("content", "")
        if role == "user":
            messages.append(HumanMessage(content=msg_content))
        elif role == "assistant":
            messages.append(AIMessage(content=msg_content))

    # 加入当前最新提问
    messages.append(HumanMessage(content=user_message))

    # 🌟 关键修改：使用 astream 获取流式输出，并包装为 SSE 格式
    async for chunk in llm.astream(messages):
        if chunk.content:
            # 必须严格遵守 SSE 协议格式：以 "data: " 开头，以 "\n\n" 结尾
            # 注意将文本中的换行符替换，防止破坏 SSE 结构
            safe_content = chunk.content.replace('\n', '\\n')
            yield f"data: {safe_content}\n\n"

    # ⚠️ 注意：最后调用模型 ainvoke 和 chat_parser.parse 的代码已经彻底删除！
    # 因为在上面的循环中，模型的数据已经全部流向前端了。

# ────────────────────────────────────────────────
# 功能 4：题库健康度智能分析
# ────────────────────────────────────────────────
class BankAnalyzeResult(BaseModel):
    health_score: int = Field(description="题库健康度打分，0-100的整数")
    current_distribution: dict = Field(description="当前题库的分布情况概述，如 {'基础': 60, '进阶': 30, '底层原理': 10}")
    suggested_topics: List[str] = Field(description="建议补充的考点或题目方向列表")


bank_analyze_parser = PydanticOutputParser(pydantic_object=BankAnalyzeResult)


async def analyze_bank(bank_name: str, question_count: int, tags_data: dict) -> BankAnalyzeResult:
    """
    根据题库名称、题目数量以及标签分布统计，让 AI 对该题库的健康度进行打分评估。
    """

    prompt_template = """你是一个高级教研总监。请根据下列题库统计数据，分析该题库的健康度和知识点覆盖情况，指出知识盲点，并建议补充哪些考点。

【题库概况】
题库名称：{bank_name}
总题目数量：{question_count}
考点标签分布（格式：'标签名': 次数）：
{tags_data}

结合常见大厂面试要求，请严格按照以下格式 JSON 输出你的分析。
{format_instructions}
"""

    chain = ChatPromptTemplate.from_template(prompt_template) | llm | bank_analyze_parser

    result = await chain.ainvoke({
        "bank_name": bank_name,
        "question_count": question_count,
        "tags_data": tags_data,
        "format_instructions": bank_analyze_parser.get_format_instructions()
    })
    return result

