"use client";

import React, { useState, useRef, useEffect } from "react";
import { FloatButton, Drawer, Input, Button, List, Avatar, message, Spin, Typography } from "antd";
import { MessageOutlined, RobotOutlined, UserOutlined } from "@ant-design/icons";
// 删除了旧的同步 API 调用：import { aiChatUsingPost } from "@/api/questionController";
import { Viewer } from "@bytemd/react";
import gfm from "@bytemd/plugin-gfm";
import highlight from "@bytemd/plugin-highlight";
import "bytemd/dist/index.css";
import "./index.css";

const plugins = [gfm(), highlight()];

interface Props {
  questionId: number;
}

const { Paragraph } = Typography;

// 🌟 这里配置你的 Java 后端真实地址（请根据你的实际端口修改）
const BACKEND_URL = "http://localhost:8101";

export default function AiChatBox({ questionId }: Props) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<any[]>([]); // 若有 API.AiChatMessage 也可以换回来
  const [inputValue, setInputValue] = useState("");
  const [loading, setLoading] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  // 自动滚动到最新消息
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages, loading]);

  const handleSend = async () => {
    if (!inputValue.trim()) return;

    const userMsg = inputValue.trim();
    setInputValue("");

    // 1. 更新本地历史，同时插入一条“空的 AI 占位消息”，准备接收流
    setMessages((prev) => [
      ...prev,
      { role: "user", content: userMsg },
      { role: "assistant", content: "" }
    ]);
    setLoading(true);

    try {
      // 2. 原生 fetch 发起 GET 流式请求
      // 🌟 关键修改：加上了 ${BACKEND_URL}
      const response = await fetch(
        `${BACKEND_URL}/api/question/ai-chat/stream?questionId=${questionId}&userMessage=${encodeURIComponent(userMsg)}`,
        {
          method: "GET",
          headers: {
            "Accept": "text/event-stream",
            // ⚠️ 如果你的项目有 JWT Token 拦截校验，请在这里加上：
            // "Authorization": `Bearer ${localStorage.getItem('token')}`
          },
          // 支持携带 Cookie Session，跨域时必带
          credentials: "include"
        }
      );

      if (!response.ok) {
        throw new Error(`网络请求异常: ${response.status}`);
      }

      // 3. 获取数据读取器
      const reader = response.body?.getReader();
      const decoder = new TextDecoder("utf-8");

      let aiFullReply = ""; // 完整拼接后的字符串
      let buffer = "";      // 防粘包/断包的字符串缓存区
      let done = false;

      // 4. 循环读取流
      while (!done && reader) {
        const { value, done: readerDone } = await reader.read();
        done = readerDone;

        if (value) {
          // 解码收到的字节流并存入缓存
          buffer += decoder.decode(value, { stream: true });

          // 核心解析：SSE 数据包严格以 \n\n 结尾
          let eventEndIndex = buffer.indexOf("\n\n");

          while (eventEndIndex !== -1) {
            // 截取出一个完整的 SSE 数据包
            const eventString = buffer.substring(0, eventEndIndex);
            // 移除已处理的部分
            buffer = buffer.substring(eventEndIndex + 2);

            // 👉 调试神器：把它打印在控制台，能直观看到后端发来的流数据
            console.log("收到流片段:", eventString);

            // 🌟 核心修复：兼容冒号后面有空格和无空格的两种情况
            if (eventString.startsWith("data:")) {
              let chunkText = eventString.substring(5); // 剥去 "data:"
              if (chunkText.startsWith(" ")) {
                chunkText = chunkText.substring(1); // 如果带有空格，再剥去空格
              }

              // 恢复后端的换行符保护
              chunkText = chunkText.replace(/\\n/g, "\n");

              // 累加真正的文字
              aiFullReply += chunkText;

              // 🌟 深度更新 React State，确保触发界面重绘
              setMessages((prev) => {
                const newArr = [...prev];
                const lastIndex = newArr.length - 1;
                // 使用解构来创建新对象，确保 React 监听到深层的内容变化
                newArr[lastIndex] = { ...newArr[lastIndex], content: aiFullReply };
                return newArr;
              });
            }

            // 继续寻找下一个换行符
            eventEndIndex = buffer.indexOf("\n\n");
          }
        }
      }
    } catch (e: any) {
      message.error("AI 导师开小差了: " + e.message);
    } finally {
      // 停止 loading 动画，释放输入框
      setLoading(false);
    }
  };

  return (
    <>
      <FloatButton
        icon={<RobotOutlined />}
        type="primary"
        style={{ right: 24, bottom: 24 }}
        onClick={() => setOpen(true)}
      />
      <Drawer
        title="AI 导师专属答疑"
        placement="right"
        width={500}
        onClose={() => setOpen(false)}
        open={open}
        styles={{ body: { padding: 0, display: "flex", flexDirection: "column" } }}
      >
        {/* 消息列表区 */}
        <div
          ref={listRef}
          style={{ flex: 1, overflowY: "auto", padding: "16px", backgroundColor: "#f5f5f5" }}
        >
          <List
            dataSource={messages}
            renderItem={(msg, index) => (
              <List.Item style={{ borderBottom: "none", padding: "8px 0" }}>
                <div style={{
                  display: "flex",
                  width: "100%",
                  justifyContent: msg.role === "user" ? "flex-end" : "flex-start",
                  alignItems: "flex-start"
                }}>
                  {msg.role === "assistant" && (
                    <Avatar icon={<RobotOutlined />} style={{ backgroundColor: "#1890ff", margin: "0 8px" }} />
                  )}
                  <div style={{
                    maxWidth: "80%",
                    backgroundColor: msg.role === "user" ? "#1890ff" : "#fff",
                    color: msg.role === "user" ? "#fff" : "#000",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    boxShadow: "0 1px 2px rgba(0,0,0,0.1)",
                    wordBreak: "break-word"
                  }}>
                    {msg.role === "user" ? (
                      <span>{msg.content}</span>
                    ) : (
                      <Viewer value={msg.content} plugins={plugins} />
                    )}
                  </div>
                </div>
              </List.Item>
            )}
          />
          {loading && (
            <div style={{ display: "flex", margin: "8px", alignItems: "flex-start" }}>
              <Avatar icon={<RobotOutlined />} style={{ backgroundColor: "#52c41a", margin: "0 8px" }} />
              <div style={{
                backgroundColor: "#fff",
                padding: "10px 14px",
                borderRadius: "8px",
                boxShadow: "0 1px 2px rgba(0,0,0,0.1)"
              }}>
                <Spin size="small" /> 正在沉浸式输出中...
              </div>
            </div>
          )}
        </div>

        {/* 输入区 */}
        <div style={{ padding: "16px", borderTop: "1px solid #f0f0f0", backgroundColor: "#fff" }}>
          <Input.Search
            placeholder="输入你的疑问..."
            allowClear
            enterButton="发送"
            size="large"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onSearch={handleSend}
            disabled={loading}
          />
          <Paragraph type="secondary" style={{ fontSize: 12, marginTop: 8, marginBottom: 0 }}>
            注：AI 的回答可能不完全准确，仅供参考。
          </Paragraph>
        </div>
      </Drawer>
    </>
  );
}