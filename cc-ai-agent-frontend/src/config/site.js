export const siteConfig = {
  name: import.meta.env.VITE_SITE_NAME || 'AI Agent 应用中心',
  description:
    import.meta.env.VITE_SITE_DESCRIPTION ||
    'AI Agent 应用中心，提供 AI 选车大师、AI 超级智能体等智能对话应用，支持多轮对话与工具调用。',
  keywords:
    import.meta.env.VITE_SITE_KEYWORDS ||
    'AI Agent,人工智能,智能对话,选车助手,超级智能体,大模型',
  url: import.meta.env.VITE_SITE_URL || 'https://example.com',
  author: import.meta.env.VITE_SITE_AUTHOR || 'CC AI Agent Team',
  copyrightHolder: import.meta.env.VITE_COPYRIGHT_HOLDER || 'CC AI Agent Team',
  wechat: import.meta.env.VITE_WECHAT || 'min_1282857357',
  icpNumber: import.meta.env.VITE_ICP_NUMBER || '',
  locale: 'zh_CN',
}

export const routeSeo = {
  Home: {
    title: 'AI Agent 应用中心',
    description: siteConfig.description,
    keywords: siteConfig.keywords,
  },
  Dashboard: {
    title: '用量统计 - AI Agent 应用中心',
    description: 'AI Agent 用量统计看板，查看会话数、消息数、每日趋势及各 Agent 用量分布。',
    keywords: '统计,用量,看板,数据分析',
  },
  AgentManage: {
    title: 'Agent 管理 - AI Agent 应用中心',
    description: '管理 AI Agent，支持创建、编辑、启停及配置 System Prompt 与工具集。',
    keywords: 'Agent管理,配置,System Prompt,工具配置',
  },
}
