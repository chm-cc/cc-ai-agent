const GA_ID = import.meta.env.VITE_GA_MEASUREMENT_ID
const BAIDU_ID = import.meta.env.VITE_BAIDU_ANALYTICS_ID
const MONITOR_API = import.meta.env.VITE_MONITOR_API

let initialized = false

function loadScript(src) {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.async = true
    script.src = src
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

function sendBeacon(payload) {
  if (!MONITOR_API) return

  const body = JSON.stringify({
    ...payload,
    timestamp: Date.now(),
    url: location.href,
    userAgent: navigator.userAgent,
  })

  if (navigator.sendBeacon) {
    navigator.sendBeacon(MONITOR_API, new Blob([body], { type: 'application/json' }))
    return
  }

  fetch(MONITOR_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    keepalive: true,
  }).catch(() => {})
}

function initGoogleAnalytics() {
  if (!GA_ID || window.gtag) return

  window.dataLayer = window.dataLayer || []
  window.gtag = function gtag() {
    window.dataLayer.push(arguments)
  }
  window.gtag('js', new Date())
  window.gtag('config', GA_ID, { send_page_view: false })

  loadScript(`https://www.googletagmanager.com/gtag/js?id=${GA_ID}`).catch(() => {})
}

function initBaiduAnalytics() {
  if (!BAIDU_ID) return

  window._hmt = window._hmt || []
  loadScript(`https://hm.baidu.com/hm.js?${BAIDU_ID}`).catch(() => {})
}

function observePerformance() {
  if (!('PerformanceObserver' in window)) return

  try {
    const lcpObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries()
      const last = entries[entries.length - 1]
      if (last) {
        sendBeacon({ type: 'web-vital', metric: 'LCP', value: Math.round(last.startTime) })
      }
    })
    lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true })
  } catch {
    // unsupported metric
  }

  window.addEventListener(
    'load',
    () => {
      const nav = performance.getEntriesByType('navigation')[0]
      if (nav) {
        sendBeacon({
          type: 'performance',
          metric: 'page-load',
          value: Math.round(nav.loadEventEnd - nav.startTime),
        })
      }
    },
    { once: true },
  )
}

function initErrorTracking() {
  window.addEventListener('error', (event) => {
    sendBeacon({
      type: 'error',
      message: event.message,
      source: event.filename,
      line: event.lineno,
      column: event.colno,
    })
  })

  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    sendBeacon({
      type: 'unhandled-rejection',
      message: reason?.message || String(reason),
    })
  })
}

export function initMonitor() {
  if (initialized || import.meta.env.DEV) return
  initialized = true

  initGoogleAnalytics()
  initBaiduAnalytics()
  observePerformance()
  initErrorTracking()
}

export function trackPageView(path, title) {
  if (import.meta.env.DEV) return

  if (GA_ID && window.gtag) {
    window.gtag('event', 'page_view', {
      page_path: path,
      page_title: title,
    })
  }

  if (BAIDU_ID && window._hmt) {
    window._hmt.push(['_trackPageview', path])
  }

  sendBeacon({ type: 'page-view', path, title })
}

export function trackEvent(name, params = {}) {
  if (import.meta.env.DEV) return

  if (GA_ID && window.gtag) {
    window.gtag('event', name, params)
  }

  if (BAIDU_ID && window._hmt) {
    window._hmt.push(['_trackEvent', 'app', name, JSON.stringify(params)])
  }

  sendBeacon({ type: 'event', name, params })
}
