import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

function sitemapPlugin(siteUrl) {
  const routes = ['/', '/car', '/manus']

  return {
    name: 'generate-sitemap',
    closeBundle() {
      const base = siteUrl.replace(/\/$/, '')
      const urls = routes
        .map(
          (path) => `  <url>
    <loc>${base}${path}</loc>
    <changefreq>weekly</changefreq>
    <priority>${path === '/' ? '1.0' : '0.8'}</priority>
  </url>`,
        )
        .join('\n')

      const xml = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>
`

      const distDir = resolve(__dirname, 'dist')
      writeFileSync(resolve(distDir, 'sitemap.xml'), xml, 'utf-8')

      const robots = `User-agent: *
Allow: /

Sitemap: ${base}/sitemap.xml
`
      writeFileSync(resolve(distDir, 'robots.txt'), robots, 'utf-8')
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const siteUrl = env.VITE_SITE_URL || 'https://example.com'

  return {
    plugins: [vue(), sitemapPlugin(siteUrl)],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8123',
          changeOrigin: true,
        },
      },
    },
  }
})
