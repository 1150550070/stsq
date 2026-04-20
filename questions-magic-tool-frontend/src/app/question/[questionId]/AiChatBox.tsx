"use client";

import React, { useState, useRef, useEffect } from "react";
import { FloatButton, Drawer, Input, Button, List, Avatar, message, Spin, Typography } from "antd";
import { MessageOutlined, RobotOutlined, UserOutlined } from "@ant-design/icons";
import { aiChatUsingPost } from "@/api/questionController";
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

export default function AiChatBox({ questionId }: Props) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<API.AiChatMessage[]>([]);
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
    
    // 更新本地历史
    const newChatHistory = [...messages, { role: "user", content: userMsg }];
    setMessages(newChatHistory);
    setLoading(true);

    try {
      const res = await aiChatUsingPost({
        questionId: questionId,
        userMessage: userMsg,
        chatHistory: messages, // 之前的记录
      });

      if (res.code === 0 && res.data?.aiReply) {
        setMessages([
          ...newChatHistory,
          { role: "assistant", content: res.data.aiReply }
        ]);
      } else {
        message.error("AI 响应异常: " + res.message);
      }
    } catch (e: any) {
      message.error("请求 AI 失败: " + e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <FloatButton
        icon={<MessageOutlined />}
        type="primary"
        style={{ right: 24, bottom: 24 }}
        onClick={() => setOpen(true)}
        tooltip="AI 专属智能答疑"
      />
      <Drawer
        title="✨ 题目专属智能答疑教练"
        placement="right"
        onClose={() => setOpen(false)}
        open={open}
        width={400}
        bodyStyle={{ display: "flex", flexDirection: "column", padding: 0 }}
      >
        {/* 聊天记录区 */}
        <div 
          ref={listRef}
          style={{ flex: 1, overflowY: "auto", padding: "16px", backgroundColor: "#f5f5f5" }}
        >
          {messages.length === 0 && (
            <div style={{ textAlign: "center", color: "#999", marginTop: "50px" }}>
              你好！我是你的 AI 面试教练。对这道题有什么不懂的，随时问我！
            </div>
          )}
          <List
            dataSource={messages}
            renderItem={(msg, index) => (
              <List.Item style={{ borderBottom: "none", padding: "8px 0" }}>
                <div style={{
                  display: "flex",
                  flexDirection: msg.role === "user" ? "row-reverse" : "row",
                  width: "100%",
                  alignItems: "flex-start"
                }}>
                  <Avatar 
                    icon={msg.role === "user" ? <UserOutlined /> : <RobotOutlined />} 
                    style={{ backgroundColor: msg.role === "user" ? "#1677ff" : "#52c41a", margin: "0 8px" }}
                  />
                  <div style={{
                    maxWidth: "75%",
                    backgroundColor: msg.role === "user" ? "#1677ff" : "#fff",
                    color: msg.role === "user" ? "#fff" : "#333",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    boxShadow: "0 1px 2px rgba(0,0,0,0.1)"
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
                 <Spin size="small" /> 正在思考中...
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
