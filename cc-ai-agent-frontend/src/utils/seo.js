import { siteConfig } from '../config/site'

function upsertMeta(attr, key, content) {
  if (!content) return

  let el = document.head.querySelector(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function upsertLink(rel, href) {
  if (!href) return

  let el = document.head.querySelector(`link[rel="${rel}"]`)
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', rel)
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}

export function updatePageSeo({ title, description, keywords, path = '/' }) {
  const fullTitle = title === siteConfig.name ? title : `${title} | ${siteConfig.name}`
  const pageUrl = `${siteConfig.url.replace(/\/$/, '')}${path}`
  const pageDescription = description || siteConfig.description
  const pageKeywords = keywords || siteConfig.keywords

  document.title = fullTitle

  upsertMeta('name', 'description', pageDescription)
  upsertMeta('name', 'keywords', pageKeywords)
  upsertMeta('name', 'author', siteConfig.author)
  upsertMeta('name', 'robots', 'index, follow')

  upsertMeta('property', 'og:type', 'website')
  upsertMeta('property', 'og:site_name', siteConfig.name)
  upsertMeta('property', 'og:title', fullTitle)
  upsertMeta('property', 'og:description', pageDescription)
  upsertMeta('property', 'og:url', pageUrl)
  upsertMeta('property', 'og:locale', siteConfig.locale)

  upsertMeta('name', 'twitter:card', 'summary')
  upsertMeta('name', 'twitter:title', fullTitle)
  upsertMeta('name', 'twitter:description', pageDescription)

  upsertLink('canonical', pageUrl)
}
