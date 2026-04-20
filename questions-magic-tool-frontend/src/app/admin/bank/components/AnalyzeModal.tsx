import { Button, Modal, Spin, message, Typography, Divider, List, Tag } from "antd";
import React, { useEffect, useState } from "react";
import { aiAnalyzeBankUsingPost } from "@/api/questionBankController";

const { Title, Paragraph, Text } = Typography;

interface Props {
  oldData?: API.QuestionBank;
  visible: boolean;
  onCancel: () => void;
}

/**
 * 题库健康度智能分析弹窗
 */
const AnalyzeModal: React.FC<Props> = (props) => {
  const { oldData, visible, onCancel } = props;
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<API.QuestionBankAiAnalyzeResult | null>(null);

  useEffect(() => {
    if (visible && oldData?.id) {
      handleAnalyze(oldData.id);
    } else {
      setResult(null);
    }
  }, [visible, oldData]);

  const handleAnalyze = async (bankId: number) => {
    setLoading(true);
    try {
      const res = await aiAnalyzeBankUsingPost({
        bankId,
      });
      if (res.code === 0 && res.data) {
        setResult(res.data);
      } else {
        message.error("分析失败: " + res.message);
      }
    } catch (e: any) {
      message.error("请求失败: " + e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      destroyOnClose
      title={`📈 题库健康度分析 - ${oldData?.title || ""}`}
      open={visible}
      footer={[
        <Button key="close" onClick={onCancel}>
          关闭
        </Button>,
      ]}
      onCancel={onCancel}
      width={600}
    >
      {loading ? (
        <div style={{ textAlign: "center", padding: "40px 0" }}>
          <Spin size="large" />
          <div style={{ marginTop: 16, color: "#666" }}>正在进行深度检视，这可能需要几十秒时间...</div>
        </div>
      ) : result ? (
        <div>
          <div style={{ textAlign: "center", marginBottom: 24 }}>
            <Title level={1} style={{ margin: 0, color: result.healthScore && result.healthScore >= 80 ? "#52c41a" : "#faad14" }}>
              {result.healthScore} 分
            </Title>
            <Text type="secondary">综合健康得分</Text>
          </div>

          <Divider orientation="left">考点能力分布</Divider>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
            {Object.entries(result.currentDistribution || {}).map(([key, value]) => (
              <Tag color="geekblue" key={key}>
                {key}: {value} 题
              </Tag>
            ))}
          </div>

          <Divider orientation="left">AI 优化建议</Divider>
          <List
            size="small"
            dataSource={result.suggestedTopics || []}
            renderItem={(item) => (
              <List.Item>
                 <Text>💡 {item}</Text>
              </List.Item>
            )}
            locale={{ emptyText: "题库非常健康，没有多余建议！" }}
          />
        </div>
      ) : (
        <div style={{ textAlign: "center", padding: "40px 0", color: "#999" }}>
          获取数据失败或没有数据
        </div>
      )}
    </Modal>
  );
};

export default AnalyzeModal;
