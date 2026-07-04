<template>
  <main class="home">
    <header class="hero">
      <h1 class="hero-title">AI Agent 应用中心</h1>
      <p class="hero-desc">选择下方应用，开始智能对话体验</p>
    </header>

    <section class="app-grid" aria-label="应用列表">
      <router-link to="/car" class="app-card">
        <div class="card-icon car" aria-hidden="true">🚗</div>
        <h2 class="card-title">AI 选车大师</h2>
        <p class="card-desc">
          专业选车顾问，根据预算、用途、偏好提供个性化购车建议，支持多轮对话记忆。
        </p>
        <span class="card-action">进入应用 →</span>
      </router-link>

      <router-link to="/manus" class="app-card">
        <div class="card-icon manus" aria-hidden="true">🤖</div>
        <h2 class="card-title">AI 超级智能体</h2>
        <p class="card-desc">
          具备工具调用能力的超级智能体，可执行网页抓取、文件操作、命令执行等复杂任务。
        </p>
        <span class="card-action">进入应用 →</span>
      </router-link>
    </section>
  </main>
</template>

<style scoped>
.home {
  flex: 1;
  padding: 60px 24px 24px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

.hero {
  text-align: center;
  margin-bottom: 48px;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: -0.5px;
}

.hero-desc {
  margin-top: 12px;
  font-size: 16px;
  color: #888;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.app-card {
  display: flex;
  flex-direction: column;
  padding: 28px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(79, 110, 247, 0.15);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-bottom: 16px;
}

.card-icon.car {
  background: #eef1fe;
}

.card-icon.manus {
  background: #f0fdf4;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.card-desc {
  flex: 1;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
}

.card-action {
  font-size: 14px;
  font-weight: 500;
  color: #4f6ef7;
}
</style>
