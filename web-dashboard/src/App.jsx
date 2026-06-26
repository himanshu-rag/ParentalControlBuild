import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Shield, MapPin, Bell, BarChart2, FolderOpen,
  Image, Film, Music, FileText, Package,
  RefreshCw, Download, ExternalLink, ChevronDown,
  ChevronUp, Activity, Wifi, WifiOff, Search, X
} from 'lucide-react'
import { supabase } from './supabase'
import './App.css'

// ── Animation variants ────────────────────────────────────────────────────────
const fadeUp   = { hidden: { opacity: 0, y: 24 }, show: { opacity: 1, y: 0 } }
const fadeIn   = { hidden: { opacity: 0 },         show: { opacity: 1 } }
const stagger  = { show: { transition: { staggerChildren: 0.08 } } }
const scaleIn  = { hidden: { opacity: 0, scale: 0.92 }, show: { opacity: 1, scale: 1 } }

// ── Category config ───────────────────────────────────────────────────────────
const CATEGORIES = [
  { key: 'Images',    icon: Image,    color: '#f472b6', bg: '#2d1320' },
  { key: 'Videos',    icon: Film,     color: '#60a5fa', bg: '#0f1e2d' },
  { key: 'Audio',     icon: Music,    color: '#2dd4bf', bg: '#0f2420' },
  { key: 'Documents', icon: FileText, color: '#4ade80', bg: '#0f2018' },
  { key: 'Others',    icon: Package,  color: '#c084fc', bg: '#1e1230' },
]

// ── Helper: format bytes ──────────────────────────────────────────────────────
function fmtSize(kb) {
  if (kb > 1024 * 1024) return `${(kb / 1024 / 1024).toFixed(1)} GB`
  if (kb > 1024)        return `${(kb / 1024).toFixed(1)} MB`
  return `${kb} KB`
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
function StatCard({ icon: Icon, label, value, color, delay = 0 }) {
  return (
    <motion.div
      className="stat-card"
      variants={scaleIn}
      initial="hidden"
      animate="show"
      transition={{ delay, type: 'spring', stiffness: 200, damping: 20 }}
      whileHover={{ scale: 1.04, y: -4 }}
    >
      <div className="stat-icon" style={{ background: color + '22', color }}>
        <Icon size={20} />
      </div>
      <div className="stat-value" style={{ color }}>{value}</div>
      <div className="stat-label">{label}</div>
    </motion.div>
  )
}

// ── Info Card (dashboard) ─────────────────────────────────────────────────────
function InfoCard({ icon: Icon, title, children, color, delay = 0 }) {
  return (
    <motion.div
      className="info-card"
      variants={fadeUp}
      style={{ '--accent': color }}
      transition={{ delay }}
      whileHover={{ y: -2 }}
    >
      <div className="info-card-header">
        <div className="info-icon" style={{ color }}>
          <Icon size={16} />
        </div>
        <span style={{ color, fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
          {title}
        </span>
      </div>
      <div className="info-card-body">{children}</div>
    </motion.div>
  )
}

// ── File Row ──────────────────────────────────────────────────────────────────
function FileRow({ file, index }) {
  const hasUrl = file.storage_url && file.storage_url.length > 0

  return (
    <motion.div
      className="file-row"
      variants={fadeUp}
      style={{ background: index % 2 === 0 ? '#161929' : '#0f1120' }}
      whileHover={{ background: '#1e2240', x: 4 }}
      transition={{ duration: 0.15 }}
    >
      <div className="file-info">
        <span className="file-name">{file.file_name}</span>
        <span className="file-meta">
          {file.mime_type?.toUpperCase()} · {fmtSize(file.file_size_kb)}
        </span>
      </div>
      <div className="file-actions">
        {hasUrl ? (
          <>
            <motion.a
              href={file.storage_url}
              target="_blank"
              rel="noreferrer"
              className="file-btn open-btn"
              whileHover={{ scale: 1.08 }}
              whileTap={{ scale: 0.95 }}
            >
              <ExternalLink size={12} /> Open
            </motion.a>
            <motion.a
              href={file.storage_url}
              download={file.file_name}
              className="file-btn dl-btn"
              whileHover={{ scale: 1.08 }}
              whileTap={{ scale: 0.95 }}
            >
              <Download size={12} /> Save
            </motion.a>
          </>
        ) : (
          <span className="pending-badge">⏳ Pending</span>
        )}
      </div>
    </motion.div>
  )
}

// ── Category Panel ────────────────────────────────────────────────────────────
function CategoryPanel({ config, files, search }) {
  const [open, setOpen] = useState(false)
  const filtered = files.filter(f =>
    f.file_name.toLowerCase().includes(search.toLowerCase())
  )
  const totalKb  = files.reduce((a, f) => a + f.file_size_kb, 0)
  const uploaded = files.filter(f => f.storage_url).length
  const Icon = config.icon

  return (
    <motion.div
      className="category-card"
      variants={fadeUp}
      style={{ '--cat-color': config.color, '--cat-bg': config.bg }}
      layout
    >
      <motion.button
        className="category-header"
        onClick={() => setOpen(o => !o)}
        whileHover={{ opacity: 0.92 }}
        whileTap={{ scale: 0.99 }}
      >
        <div className="cat-icon-wrap">
          <Icon size={22} color={config.color} />
        </div>
        <div className="cat-info">
          <span className="cat-title">{config.key}</span>
          <span className="cat-meta">
            {files.length} files · {fmtSize(totalKb)} · {uploaded} uploaded
          </span>
        </div>
        <motion.div animate={{ rotate: open ? 180 : 0 }} transition={{ duration: 0.25 }}>
          <ChevronDown size={18} color={config.color} />
        </motion.div>
      </motion.button>

      <AnimatePresence>
        {open && (
          <motion.div
            className="file-list"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: 'easeInOut' }}
            style={{ overflow: 'hidden' }}
          >
            <motion.div variants={stagger} initial="hidden" animate="show">
              {filtered.slice(0, 100).map((file, i) => (
                <FileRow key={file.id ?? i} file={file} index={i} />
              ))}
              {filtered.length === 0 && (
                <div className="empty-cat">No files match your search</div>
              )}
              {filtered.length > 100 && (
                <div className="more-files">… and {filtered.length - 100} more files</div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}

// ── Main App ──────────────────────────────────────────────────────────────────
export default function App() {
  const [tab, setTab]         = useState('dashboard')
  const [files, setFiles]     = useState([])
  const [location, setLoc]    = useState(null)
  const [notif, setNotif]     = useState(null)
  const [usage, setUsage]     = useState([])
  const [loading, setLoading] = useState(false)
  const [search, setSearch]   = useState('')
  const [online, setOnline]   = useState(true)
  const [lastSync, setLastSync] = useState(null)

  // Load dashboard data
  useEffect(() => { fetchDashboard() }, [])

  async function fetchDashboard() {
    try {
      const [{ data: locs }, { data: notifs }, { data: usages }] = await Promise.all([
        supabase.from('locations').select('*').order('created_at', { ascending: false }).limit(1),
        supabase.from('notifications').select('*').order('created_at', { ascending: false }).limit(1),
        supabase.from('app_usage').select('*').order('time_spent_seconds', { ascending: false }).limit(10),
      ])
      if (locs?.length)   setLoc(locs[0])
      if (notifs?.length) setNotif(notifs[0])
      if (usages?.length) setUsage(usages)
      setOnline(true)
    } catch {
      setOnline(false)
    }
  }

  async function fetchFiles() {
    setLoading(true)
    try {
      const { data } = await supabase.from('files').select('*')
      setFiles(data ?? [])
      setLastSync(new Date())
      setOnline(true)
    } catch {
      setOnline(false)
    } finally {
      setLoading(false)
    }
  }

  const grouped = CATEGORIES.reduce((acc, c) => {
    acc[c.key] = files.filter(f => f.category === c.key)
    return acc
  }, {})

  const totalKb    = files.reduce((a, f) => a + f.file_size_kb, 0)
  const uploaded   = files.filter(f => f.storage_url).length

  return (
    <div className="app">
      {/* ── Sidebar ── */}
      <motion.aside
        className="sidebar"
        initial={{ x: -80, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        transition={{ type: 'spring', stiffness: 200, damping: 25 }}
      >
        <div className="logo">
          <Shield size={28} color="#7c6fff" />
          <span>ParentGuard</span>
        </div>

        <nav className="nav">
          {[
            { id: 'dashboard', icon: Activity,    label: 'Dashboard' },
            { id: 'files',     icon: FolderOpen,  label: 'Files' },
          ].map(item => (
            <motion.button
              key={item.id}
              className={`nav-btn ${tab === item.id ? 'active' : ''}`}
              onClick={() => { setTab(item.id); if (item.id === 'files' && files.length === 0) fetchFiles() }}
              whileHover={{ x: 4 }}
              whileTap={{ scale: 0.97 }}
            >
              <item.icon size={18} />
              <span>{item.label}</span>
              {tab === item.id && (
                <motion.div className="nav-indicator" layoutId="indicator" />
              )}
            </motion.button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className={`status-pill ${online ? 'online' : 'offline'}`}>
            {online ? <Wifi size={12} /> : <WifiOff size={12} />}
            {online ? 'Connected' : 'Offline'}
          </div>
          {lastSync && (
            <div className="last-sync">
              Synced {lastSync.toLocaleTimeString()}
            </div>
          )}
        </div>
      </motion.aside>

      {/* ── Main Content ── */}
      <main className="main">
        <AnimatePresence mode="wait">

          {/* ── DASHBOARD TAB ── */}
          {tab === 'dashboard' && (
            <motion.div
              key="dashboard"
              className="page"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.25 }}
            >
              <div className="page-header">
                <div>
                  <h1>Dashboard</h1>
                  <p className="page-sub">Live monitoring of child's device</p>
                </div>
                <motion.button
                  className="refresh-btn"
                  onClick={fetchDashboard}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  <RefreshCw size={16} /> Refresh
                </motion.button>
              </div>

              <motion.div className="stats-row" variants={stagger} initial="hidden" animate="show">
                <StatCard icon={MapPin}    label="Location"      value={location ? '✓ Active' : '—'} color="#2dd4bf" delay={0.0} />
                <StatCard icon={Bell}      label="Notifications" value={notif    ? '✓ Active' : '—'} color="#7c6fff" delay={0.1} />
                <StatCard icon={BarChart2} label="Apps Tracked"  value={usage.length}                 color="#fb923c" delay={0.2} />
              </motion.div>

              <motion.div
                className="cards-grid"
                variants={stagger}
                initial="hidden"
                animate="show"
              >
                <InfoCard icon={MapPin} title="Last Known Location" color="#2dd4bf" delay={0.1}>
                  {location ? (
                    <div className="location-info">
                      <div className="coord">
                        <span className="coord-label">Latitude</span>
                        <span className="coord-val">{location.latitude?.toFixed(6)}</span>
                      </div>
                      <div className="coord">
                        <span className="coord-label">Longitude</span>
                        <span className="coord-val">{location.longitude?.toFixed(6)}</span>
                      </div>
                      <a
                        href={`https://maps.google.com/?q=${location.latitude},${location.longitude}`}
                        target="_blank"
                        rel="noreferrer"
                        className="map-link"
                      >
                        <ExternalLink size={13} /> Open in Google Maps
                      </a>
                    </div>
                  ) : (
                    <span className="no-data">Waiting for location data…</span>
                  )}
                </InfoCard>

                <InfoCard icon={Bell} title="Latest Notification" color="#7c6fff" delay={0.15}>
                  {notif ? (
                    <div className="notif-info">
                      <div className="notif-app">{notif.package_name}</div>
                      <div className="notif-title">{notif.title}</div>
                      <div className="notif-text">{notif.text}</div>
                    </div>
                  ) : (
                    <span className="no-data">No notifications intercepted yet…</span>
                  )}
                </InfoCard>
              </motion.div>

              <div className="section-title">📊 App Usage Today</div>
              <motion.div className="usage-list" variants={stagger} initial="hidden" animate="show">
                {usage.length === 0 && (
                  <div className="no-data">No usage data yet…</div>
                )}
                {usage.map((u, i) => (
                  <motion.div key={i} className="usage-row" variants={fadeUp} whileHover={{ x: 6 }}>
                    <div className="usage-rank">#{i + 1}</div>
                    <div className="usage-pkg">{u.package_name}</div>
                    <div className="usage-bar-wrap">
                      <motion.div
                        className="usage-bar"
                        initial={{ width: 0 }}
                        animate={{ width: `${Math.min(100, (u.time_spent_seconds / (usage[0]?.time_spent_seconds || 1)) * 100)}%` }}
                        transition={{ delay: i * 0.05 + 0.3, duration: 0.6, ease: 'easeOut' }}
                      />
                    </div>
                    <div className="usage-time">{u.time_spent_seconds}s</div>
                  </motion.div>
                ))}
              </motion.div>
            </motion.div>
          )}

          {/* ── FILES TAB ── */}
          {tab === 'files' && (
            <motion.div
              key="files"
              className="page"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.25 }}
            >
              <div className="page-header">
                <div>
                  <h1>Child's Files</h1>
                  <p className="page-sub">
                    {files.length > 0
                      ? `${files.length} files · ${fmtSize(totalKb)} · ${uploaded} available to open`
                      : 'Browse files from child\'s device'}
                  </p>
                </div>
                <motion.button
                  className="refresh-btn"
                  onClick={fetchFiles}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  disabled={loading}
                >
                  <motion.div animate={{ rotate: loading ? 360 : 0 }} transition={{ repeat: loading ? Infinity : 0, duration: 1, ease: 'linear' }}>
                    <RefreshCw size={16} />
                  </motion.div>
                  {loading ? 'Loading…' : 'Refresh'}
                </motion.button>
              </div>

              {/* Category mini stats */}
              {files.length > 0 && (
                <motion.div className="cat-stats" variants={stagger} initial="hidden" animate="show">
                  {CATEGORIES.map((cat, i) => {
                    const count = grouped[cat.key]?.length ?? 0
                    return (
                      <motion.div
                        key={cat.key}
                        className="cat-stat"
                        style={{ '--c': cat.color, '--cbg': cat.bg }}
                        variants={scaleIn}
                        transition={{ delay: i * 0.07 }}
                        whileHover={{ scale: 1.05, y: -3 }}
                      >
                        <cat.icon size={18} color={cat.color} />
                        <span className="cat-stat-count">{count}</span>
                        <span className="cat-stat-label">{cat.key}</span>
                      </motion.div>
                    )
                  })}
                </motion.div>
              )}

              {/* Search */}
              {files.length > 0 && (
                <motion.div className="search-wrap" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }}>
                  <Search size={16} color="var(--text-sec)" />
                  <input
                    className="search-input"
                    placeholder="Search files by name…"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                  />
                  {search && (
                    <motion.button className="clear-btn" onClick={() => setSearch('')} whileHover={{ scale: 1.1 }}>
                      <X size={14} />
                    </motion.button>
                  )}
                </motion.div>
              )}

              {/* Loading spinner */}
              {loading && (
                <motion.div className="spinner-wrap" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                  <motion.div
                    className="spinner"
                    animate={{ rotate: 360 }}
                    transition={{ repeat: Infinity, duration: 0.9, ease: 'linear' }}
                  />
                  <span>Fetching files from child's device…</span>
                </motion.div>
              )}

              {/* Empty state */}
              {!loading && files.length === 0 && (
                <motion.div className="empty-state" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
                  <FolderOpen size={56} color="var(--text-dim)" />
                  <h3>No Files Yet</h3>
                  <p>Open the Child App on the child's phone and tap<br /><strong>"🔄 Scan & Sync Files"</strong></p>
                  <motion.button className="refresh-btn big" onClick={fetchFiles} whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                    <RefreshCw size={16} /> Try Refresh
                  </motion.button>
                </motion.div>
              )}

              {/* Category panels */}
              {!loading && files.length > 0 && (
                <motion.div variants={stagger} initial="hidden" animate="show">
                  {CATEGORIES.map(cat => {
                    const catFiles = grouped[cat.key] ?? []
                    if (catFiles.length === 0) return null
                    return (
                      <CategoryPanel key={cat.key} config={cat} files={catFiles} search={search} />
                    )
                  })}
                </motion.div>
              )}
            </motion.div>
          )}

        </AnimatePresence>
      </main>
    </div>
  )
}
