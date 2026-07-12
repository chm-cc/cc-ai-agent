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
}
