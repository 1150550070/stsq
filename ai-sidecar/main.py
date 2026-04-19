from fastapi import FastAPI
import uvicorn
from app.routers import ai_router

app = FastAPI(title="刷题神器 - AI")

app.include_router(ai_router.router)

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port = 8000, reload=True)