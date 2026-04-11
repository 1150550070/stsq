"use client";
import React, { useState, useEffect, useMemo } from "react";
import { Card, Select, Typography, Space, Badge } from "antd";
import dayjs from "dayjs";
import dynamic from "next/dynamic";
import "./index.css";

const { Title, Text } = Typography;

// 【关键点】Next.js 中必须动态引入 Ant Design Charts 关闭 SSR
const Heatmap = dynamic(
  () => import("@ant-design/plots").then((mod) => mod.Heatmap),
  { ssr: false, loading: () => <div style={{ height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>图表加载中...</div> }
);

/**
 * 模拟生成全年的签到数据
 */
const generateMockData = (year: number) => {
  const startDate = dayjs(`${year}-01-01`);
  const endDate = dayjs(`${year}-12-31`);
  const today = dayjs();
  const data = [];

  let current = startDate;
  let weekColIndex = 0;

  while (current.isBefore(endDate) || current.isSame(endDate, "day")) {
    const dayOfWeek = current.day(); // 0(周日) - 6(周六)
    const isFuture = current.isAfter(today, "day");

    // 模拟状态：未来日期为空，过去的日期随机签到 (0=未签到, 1=已签到)
    const status = isFuture ? null : Math.random() > 0.4 ? 1 : 0;

    data.push({
      date: current.format("YYYY-MM-DD"),
      month: current.month(),
      dayOfWeek: dayOfWeek.toString(), // 转为字符串方便分类轴排序
      weekIndex: weekColIndex.toString(),
      status: status,
    });

    // 一周结束（周六），列索引 +1
    if (dayOfWeek === 6) {
      weekColIndex++;
    }
    current = current.add(1, "day");
  }
  return data;
};

/**
 * 用户年度签到日历热力图
 */
const CalendarChart: React.FC = () => {
  const [selectedYear, setSelectedYear] = useState<number>(2026);
  const [chartData, setChartData] = useState<any[]>([]);

  // 监听年份切换，重新生成/获取数据
  useEffect(() => {
    // 真实项目中这里应该替换为调用后端 API：
    // getUserSignInRecordUsingGet({ year: selectedYear }).then(...)
    const data = generateMockData(selectedYear);
    setChartData(data);
  }, [selectedYear]);

  // Ant Design Charts 热力图配置
  const config = useMemo(() => {
    return {
      data: chartData,
      autoFit: true,
      xField: "weekIndex",
      yField: "dayOfWeek",
      colorField: "status",
      // 自定义颜色映射
      color: (datum: any) => {
        if (datum.status === null) return "#fafafa"; // 未来日期格子的底色
        if (datum.status === 0) return "#ebedf0"; // 未签到的底色 (GitHub 浅灰色)
        return "#1677ff"; // 已签到的颜色 (Ant Design 主色蓝)
      },
      // 图表内边距
      padding: [20, 10, 20, 40],
      shape: "square",
      // 隐藏图例，我们自己写 HTML 图例更好看
      legend: false,
      xAxis: {
        line: null,
        tickLine: null,
        label: {
          formatter: (val: string) => {
            // 每月只在第一周显示一次月份标签
            const item = chartData.find((d) => d.weekIndex === val);
            if (item && item.date.endsWith("01")) {
              return `${item.month + 1}月`;
            }
            return "";
          },
          style: { fill: "#8c8c8c" },
        },
      },
      yAxis: {
        grid: null,
        line: null,
        tickLine: null,
        // 强制 Y 轴的顺序，让周日排在最上面
        values: ["6", "5", "4", "3", "2", "1", "0"],
        label: {
          formatter: (val: string) => {
            const days = ["日", "一", "二", "三", "四", "五", "六"];
            return days[parseInt(val)];
          },
          style: { fill: "#8c8c8c" },
        },
      },
      tooltip: {
        showMarkers: false,
        customContent: (title: string, dataItems: any[]) => {
          if (!dataItems || dataItems.length === 0) return null;
          const datum = dataItems[0].data;
          const statusText =
            datum.status === null
              ? "未来日期"
              : datum.status === 1
                ? "已签到"
                : "未签到";

          return (
            <div style={{ padding: "8px 12px" }}>
              <div style={{ marginBottom: 4, fontWeight: "bold", color: "#333" }}>
                {datum.date}
              </div>
              <div style={{ color: "#666" }}>状态: {statusText}</div>
            </div>
          );
        },
      },
      interactions: [{ type: "element-active" }],
    };
  }, [chartData]);

  // 统计数据
  const totalDays = chartData.filter((d) => d.status === 1).length;

  return (
    <Card
      title={<Title level={4} style={{ margin: 0 }}>年度刷题记录</Title>}
      extra={
        <Select
          value={selectedYear}
          onChange={(val) => setSelectedYear(val)}
          options={[
            { value: 2026, label: "2026 年" },
            { value: 2025, label: "2025 年" },
          ]}
        />
      }
      style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.04)' }}
    >
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary">当前年度已累计签到：</Text>
        <Text strong style={{ fontSize: 24, color: "#1677ff", margin: "0 8px" }}>
          {totalDays}
        </Text>
        <Text type="secondary">天</Text>
      </div>

      {/* 核心热力图区域 */}
      <div style={{ height: 200, width: "100%", overflowX: 'auto', overflowY: 'hidden' }}>
        <div style={{ minWidth: 800, height: "100%" }}>
          <Heatmap {...config} />
        </div>
      </div>

      {/* 底部自定义图例 */}
      <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end' }}>
        <Space size="middle">
          <Text type="secondary" style={{ fontSize: 12 }}>Less</Text>
          <Badge color="#ebedf0" text={<span style={{ fontSize: 12, color: '#8c8c8c'}}>未签到</span>} />
          <Badge color="#1677ff" text={<span style={{ fontSize: 12, color: '#8c8c8c'}}>已签到</span>} />
          <Text type="secondary" style={{ fontSize: 12 }}>More</Text>
        </Space>
      </div>
    </Card>
  );
};

export default CalendarChart;