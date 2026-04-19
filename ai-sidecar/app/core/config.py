import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    API_KEY = os.getenv("LLM_API_KEY")
    BASE_URL = os.getenv("LLM_BASE_URL")
    MODEL_NAME = os.getenv("LLM_MODEL_NAME")

# 实例化一个单例供全局使用
settings = Settings()