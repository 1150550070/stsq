import { updateQuestionUsingPost, aiOptimizeQuestionUsingPost, aiExtractTagsUsingPost } from "@/api/questionController";
import { ProColumns, ProTable } from "@ant-design/pro-components";
import { Alert, Button, Descriptions, message, Modal, Spin, Tag, Space, Tooltip } from "antd";
import React, { useRef, useState } from "react";
import type { FormInstance } from "antd";

interface Props {
  oldData?: API.Question;
  visible: boolean;
  columns: ProColumns<API.Question>[];
  onSubmit: (values: API.QuestionAddRequest) => void;
  onCancel: () => void;
}

const handleUpdate = async (fields: API.QuestionUpdateRequest) => {
  const hide = message.loading("正在更新");
  try {
    await updateQuestionUsingPost(fields);
    hide();
    message.success("更新成功");
    return true;
  } catch (error: any) {
    hide();
    message.error("更新失败，" + error.message);
    return false;
  }
};

const UpdateModal: React.FC<Props> = (props) => {
  const { oldData, visible, columns, onSubmit, onCancel } = props;

  // AI 润色状态
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<API.QuestionAiOptimizeResult | null>(null);
  const [aiPreviewVisible, setAiPreviewVisible] = useState(false);

  // AI 标签提取状态
  const [tagLoading, setTagLoading] = useState(false);
  const [suggestedTags, setSuggestedTags] = useState<string[]>([]);
  const [tagPreviewVisible, setTagPreviewVisible] = useState(false);

  const formRef = useRef<FormInstance>();

  if (!oldData?.id) {
    return <></>;
  }

  const initValues = { ...oldData };
  if (oldData.tags) {
    initValues.tags = JSON.parse(oldData.tags) || [];
  }

  /** AI 润色（优先读取表单当前内容） */
  const handleAiOptimize = async () => {
    const values = formRef.current?.getFieldsValue();
    const title = values?.title || oldData.title;
    if (!title) {
      message.warning("题目标题为空，无法进行 AI 润色");
      return;
    }
    setAiLoading(true);
    try {
      const res = await aiOptimizeQuestionUsingPost({
        questionId: oldData.id,
        title,
        content: values?.content || oldData.content || "",
        answer: values?.answer || oldData.answer || "",
        tags: values?.tags || JSON.parse(oldData.tags || "[]"),
        category: "通用编程",
      });
      if (res?.code === 0 && res.data) {
        setAiResult(res.data);
        setAiPreviewVisible(true);
      } else {
        message.error("AI 润色失败：" + (res?.message || "未知错误"));
      }
    } catch {
      message.error("AI 服务调用失败，请检查边车服务是否启动");
    } finally {
      setAiLoading(false);
    }
  };

  /** AI 提标签（优先读取表单当前内容） */
  const handleExtractTags = async () => {
    const values = formRef.current?.getFieldsValue();
    const title = values?.title || oldData.title;
    if (!title) {
      message.warning("题目标题为空，无法进行 AI 标签提取");
      return;
    }
    setTagLoading(true);
    try {
      const res = await aiExtractTagsUsingPost({
        title,
        content: values?.content || oldData.content || "",
      });
      if (res?.code === 0 && res.data?.tags && res.data.tags.length > 0) {
        setSuggestedTags(res.data.tags);
        setTagPreviewVisible(true);
      } else {
        message.error("AI 标签提取失败：" + (res?.message || "未知错误"));
      }
    } catch {
      message.error("AI 服务调用失败，请检查边车服务是否启动");
    } finally {
      setTagLoading(false);
    }
  };

  /** 一键填入润色结果 */
  const handleApplyAiResult = () => {
    if (!aiResult) return;
    formRef.current?.setFieldsValue({
      title: aiResult.optimizedTitle,
      content: aiResult.optimizedContent,
      answer: aiResult.optimizedAnswer,
    });
    setAiPreviewVisible(false);
    message.success("AI 润色结果已填入表单，请确认后提交");
  };

  /** 追加标签 */
  const handleApplyTags = () => {
    const currentTags: string[] = formRef.current?.getFieldValue("tags") || [];
    const merged = Array.from(new Set([...currentTags, ...suggestedTags]));
    formRef.current?.setFieldsValue({ tags: merged });
    setTagPreviewVisible(false);
    message.success(`已追加 ${suggestedTags.length} 个标签到表单`);
  };

  /** 替换标签 */
  const handleReplaceTags = () => {
    formRef.current?.setFieldsValue({ tags: suggestedTags });
    setTagPreviewVisible(false);
    message.success("标签已替换为 AI 提取结果");
  };

  const isAnyLoading = aiLoading || tagLoading;

  return (
    <>
      <Modal
        destroyOnClose
        title={
          <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
            <span>更新题目</span>
            <Tooltip title="AI 对标题、内容、答案进行润色增强">
              <Button
                size="small"
                type="primary"
                loading={aiLoading}
                disabled={tagLoading}
                onClick={handleAiOptimize}
                style={{
                  background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                  border: "none",
                  borderRadius: 6,
                  fontSize: 12,
                }}
              >
                ✨ AI 润色
              </Button>
            </Tooltip>
            <Tooltip title="AI 根据标题和内容自动提取技术标签">
              <Button
                size="small"
                loading={tagLoading}
                disabled={aiLoading}
                onClick={handleExtractTags}
                style={{
                  background: "linear-gradient(135deg, #11998e 0%, #38ef7d 100%)",
                  border: "none",
                  borderRadius: 6,
                  fontSize: 12,
                  color: "#fff",
                }}
              >
                🏷️ AI 提标签
              </Button>
            </Tooltip>
          </div>
        }
        open={visible}
        footer={null}
        onCancel={() => {
          setAiResult(null);
          setSuggestedTags([]);
          onCancel?.();
        }}
        width={800}
      >
        <Spin spinning={isAnyLoading} tip={tagLoading ? "AI 正在提取标签..." : "AI 正在润色题目，请稍候（约 10-30s）..."}>
          <ProTable
            type="form"
            columns={columns}
            formRef={formRef as any}
            form={{ initialValues: initValues }}
            onSubmit={async (values: API.QuestionAddRequest) => {
              const success = await handleUpdate({ ...values, id: oldData?.id });
              if (success) {
                onSubmit?.(values);
              }
            }}
          />
        </Spin>
      </Modal>

      {/* AI 润色结果预览 Modal */}
      <Modal
        title={
          <span>
            ✨ AI 润色结果预览
            <span style={{ fontSize: 12, color: "#888", marginLeft: 8 }}>确认后将自动填入表单</span>
          </span>
        }
        open={aiPreviewVisible}
        onCancel={() => setAiPreviewVisible(false)}
        width={900}
        footer={[
          <Button key="cancel" onClick={() => setAiPreviewVisible(false)}>取消</Button>,
          <Button
            key="apply"
            type="primary"
            style={{ background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)", border: "none" }}
            onClick={handleApplyAiResult}
          >
            一键填入表单
          </Button>,
        ]}
      >
        {aiResult && (
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="优化后标题">
                <strong style={{ color: "#1677ff" }}>{aiResult.optimizedTitle}</strong>
              </Descriptions.Item>
            </Descriptions>
            <div>
              <div style={{ fontWeight: 600, marginBottom: 6, color: "#555" }}>优化后题目内容</div>
              <div style={{ background: "#f8f9fa", border: "1px solid #e8e8e8", borderRadius: 6, padding: "10px 14px", maxHeight: 160, overflowY: "auto", whiteSpace: "pre-wrap", fontSize: 13 }}>
                {aiResult.optimizedContent}
              </div>
            </div>
            <div>
              <div style={{ fontWeight: 600, marginBottom: 6, color: "#555" }}>优化后答案</div>
              <div style={{ background: "#f6ffed", border: "1px solid #b7eb8f", borderRadius: 6, padding: "10px 14px", maxHeight: 220, overflowY: "auto", whiteSpace: "pre-wrap", fontSize: 13 }}>
                {aiResult.optimizedAnswer}
              </div>
            </div>
            {aiResult.complexityAnalysis && (
              <Alert message={`⏱️ 复杂度分析：${aiResult.complexityAnalysis}`} type="info" showIcon />
            )}
            {aiResult.tips && (
              <Alert message="💡 面试追问提示" description={aiResult.tips} type="warning" showIcon />
            )}
          </div>
        )}
      </Modal>

      {/* AI 标签提取结果预览 Modal */}
      <Modal
        title={
          <span>
            🏷️ AI 标签提取结果
            <span style={{ fontSize: 12, color: "#888", marginLeft: 8 }}>选择填入方式</span>
          </span>
        }
        open={tagPreviewVisible}
        onCancel={() => setTagPreviewVisible(false)}
        width={520}
        footer={[
          <Button key="cancel" onClick={() => setTagPreviewVisible(false)}>取消</Button>,
          <Button key="append" onClick={handleApplyTags}>
            追加到现有标签
          </Button>,
          <Button
            key="replace"
            type="primary"
            style={{ background: "linear-gradient(135deg, #11998e 0%, #38ef7d 100%)", border: "none" }}
            onClick={handleReplaceTags}
          >
            替换全部标签
          </Button>,
        ]}
      >
        <div style={{ padding: "12px 0" }}>
          <div style={{ marginBottom: 12, color: "#555", fontSize: 13 }}>
            AI 提取到以下 <strong>{suggestedTags.length}</strong> 个技术标签：
          </div>
          <Space size={[8, 12]} wrap>
            {suggestedTags.map((tag) => (
              <Tag
                key={tag}
                color="blue"
                style={{ fontSize: 14, padding: "4px 12px", borderRadius: 20, cursor: "default" }}
              >
                {tag}
              </Tag>
            ))}
          </Space>
        </div>
      </Modal>
    </>
  );
};
export default UpdateModal;