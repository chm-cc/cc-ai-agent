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
  icpNumber: import.meta.env.VITE_ICP_NUMBER || '',
  locale: 'zh_CN',
}

export const routeSeo = {
  Home: {
    title: 'AI Agent 应用中心',
    description: siteConfig.description,
    keywords: siteConfig.keywords,
  },
  CarChat: {
    title: 'AI 选车大师',
    description:
      '专业 AI 选车顾问，根据预算、用途、偏好提供个性化购车建议，支持多轮对话记忆。',
    keywords: 'AI选车,购车建议,汽车推荐,智能选车',
  },
  ManusChat: {
    title: 'AI 超级智能体',
    description:
      '具备工具调用能力的 AI 超级智能体，可执行网页抓取、文件操作、命令执行等复杂任务。',
    keywords: 'AI智能体,工具调用,自动化任务,超级Agent',
  },
}
