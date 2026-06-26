package com.example.parentapp

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.parentapp.models.FileData
import com.example.parentapp.services.DashboardManager
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {

    // ── Palette ───────────────────────────────────────────────────────────────
    private val BG        = Color.parseColor("#0A0C14")
    private val SURFACE   = Color.parseColor("#131520")
    private val CARD      = Color.parseColor("#1C1F2E")
    private val ACCENT    = Color.parseColor("#7C6FFF")
    private val ACCENT2   = Color.parseColor("#FF6B8A")
    private val TEXT_PRI  = Color.parseColor("#EEEEF5")
    private val TEXT_SEC  = Color.parseColor("#8B8FA8")
    private val TEXT_DIM  = Color.parseColor("#555870")
    private val GREEN     = Color.parseColor("#2ECC71")
    private val ORANGE    = Color.parseColor("#F39C12")
    private val BLUE      = Color.parseColor("#3498DB")
    private val PINK      = Color.parseColor("#E91E8C")
    private val PURPLE    = Color.parseColor("#9B59B6")
    private val TEAL      = Color.parseColor("#1ABC9C")
    private val RED       = Color.parseColor("#E74C3C")

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var tvLocation    : TextView
    private lateinit var tvNotif       : TextView
    private lateinit var tvAppUsage    : TextView
    private lateinit var filesContainer: LinearLayout
    private lateinit var tvFilesStatus : TextView
    private lateinit var progressFiles : ProgressBar
    private lateinit var tabDashboard  : TextView
    private lateinit var tabFiles      : TextView
    private lateinit var panelDashboard: View
    private lateinit var panelFiles    : ScrollView
    private lateinit var rootScroll    : ScrollView

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootScroll = ScrollView(this).apply { setBackgroundColor(BG) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        page.addView(buildHeader())
        page.addView(buildTabBar())

        panelDashboard = buildDashboardPanel()
        panelFiles     = buildFilesPanel()

        page.addView(panelDashboard)
        page.addView(panelFiles)

        rootScroll.addView(page)
        setContentView(rootScroll)

        switchTab(true)
        startDashboard()
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private fun buildHeader(): View {
        val grad = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#1A1340"), Color.parseColor("#0F1724"))
        )
        val c = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            background   = grad
            setPadding(56, 80, 56, 48)
        }
        // Row with icon + title
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(this).apply {
            text      = "👨‍👩‍👧"
            textSize  = 36f
            setPadding(0, 0, 24, 0)
        }
        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleCol.addView(TextView(this).apply {
            text     = "ParentGuard"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT_PRI)
        })
        titleCol.addView(TextView(this).apply {
            text     = "Child monitoring dashboard"
            textSize = 12f
            setTextColor(TEXT_SEC)
            setPadding(0, 4, 0, 0)
        })
        row.addView(badge)
        row.addView(titleCol)
        c.addView(row)

        // Status pill
        val pill = TextView(this).apply {
            text      = "● LIVE"
            textSize  = 11f
            setTextColor(GREEN)
            setTypeface(null, Typeface.BOLD)
            background = roundRect(Color.parseColor("#1A2E1A"), 20)
            setPadding(24, 8, 24, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 20 }
        }
        c.addView(pill)
        return c
    }

    // ── TAB BAR ───────────────────────────────────────────────────────────────
    private fun buildTabBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(SURFACE)
        }
        tabDashboard = buildTab("📡  Dashboard")
        tabFiles     = buildTab("📁  Files")
        tabDashboard.setOnClickListener { switchTab(true) }
        tabFiles.setOnClickListener    { switchTab(false); loadFiles() }
        bar.addView(tabDashboard, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(tabFiles,     LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return bar
    }

    private fun buildTab(label: String) = TextView(this).apply {
        text     = label
        textSize = 13f
        gravity  = Gravity.CENTER
        setPadding(0, 36, 0, 36)
        setTextColor(TEXT_DIM)
    }

    private fun switchTab(isDashboard: Boolean) {
        panelDashboard.visibility = if (isDashboard) View.VISIBLE else View.GONE
        panelFiles.visibility     = if (isDashboard) View.GONE else View.VISIBLE
        tabDashboard.setTextColor(if (isDashboard) ACCENT else TEXT_DIM)
        tabDashboard.setTypeface(null, if (isDashboard) Typeface.BOLD else Typeface.NORMAL)
        tabFiles.setTextColor(if (!isDashboard) ACCENT else TEXT_DIM)
        tabFiles.setTypeface(null, if (!isDashboard) Typeface.BOLD else Typeface.NORMAL)

        // Bottom border indicator
        tabDashboard.setBackgroundColor(if (isDashboard) Color.parseColor("#0F1724") else Color.TRANSPARENT)
        tabFiles.setBackgroundColor(if (!isDashboard) Color.parseColor("#0F1724") else Color.TRANSPARENT)
    }

    // ── DASHBOARD PANEL ───────────────────────────────────────────────────────
    private fun buildDashboardPanel(): View {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 48)
        }

        p.addView(sectionLabel("📍 Location"))
        tvLocation = bodyText("Waiting for location data...")
        p.addView(glassCard(tvLocation, TEAL, "Child's current position"))

        p.addView(sectionLabel("🔔 Notifications"))
        tvNotif = bodyText("No notifications intercepted yet")
        p.addView(glassCard(tvNotif, ACCENT, "Latest notification from child"))

        p.addView(sectionLabel("📊 App Usage"))
        tvAppUsage = bodyText("Loading usage data...")
        p.addView(glassCard(tvAppUsage, ORANGE, "Top apps used today"))

        return p
    }

    // ── FILES PANEL ───────────────────────────────────────────────────────────
    private fun buildFilesPanel(): ScrollView {
        val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 80)
        }

        // Summary stats row
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 24 }
        }
        statsRow.addView(miniStat("Files", "—", ACCENT))
        statsRow.addView(miniStat("Size", "—", TEAL))
        statsRow.addView(miniStat("Status", "Tap ↻", GREEN))

        p.addView(statsRow)

        // Refresh button
        val btn = Button(this).apply {
            text       = "↻  REFRESH FILES"
            textSize   = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundRect(ACCENT, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
            ).also { it.bottomMargin = 16 }
            setOnClickListener { loadFiles() }
        }
        p.addView(btn)

        progressFiles = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        p.addView(progressFiles)

        tvFilesStatus = TextView(this).apply {
            text     = "Tap Refresh to load files from child's device"
            setTextColor(TEXT_SEC)
            textSize = 13f
            gravity  = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }
        p.addView(tvFilesStatus)

        filesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        p.addView(filesContainer)
        scroll.addView(p)
        return scroll
    }

    // ── MINI STAT CARD ────────────────────────────────────────────────────────
    private fun miniStat(label: String, value: String, color: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = roundRect(CARD, 12)
            setPadding(24, 20, 24, 20)
            gravity     = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginEnd = 12 }
        }
        card.addView(TextView(this).apply {
            text     = value
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
            gravity  = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text     = label
            textSize = 11f
            setTextColor(TEXT_SEC)
            gravity  = Gravity.CENTER
        })
        return card
    }

    // ── DASHBOARD LOGIC ───────────────────────────────────────────────────────
    private fun startDashboard() {
        val dm = DashboardManager(
            onLocationUpdated      = { loc   -> runOnUiThread { tvLocation.text  = "📍 Lat: ${loc.latitude}\n    Lng: ${loc.longitude}" } },
            onNotificationReceived = { notif -> runOnUiThread { tvNotif.text     = "📦 ${notif.package_name}\n📌 ${notif.title}\n💬 ${notif.text}" } }
        )
        dm.startListening()
        scope.launch {
            val list = withContext(Dispatchers.IO) { dm.fetchAppUsage() }
            val top  = list.sortedByDescending { it.time_spent_seconds }.take(5)
            tvAppUsage.text = if (top.isEmpty()) "No data yet." else
                top.joinToString("\n") { "▸ ${it.package_name}  →  ${it.time_spent_seconds}s" }
        }
    }

    // ── FILE LOADER ───────────────────────────────────────────────────────────
    private fun loadFiles() {
        filesContainer.removeAllViews()
        progressFiles.visibility = View.VISIBLE
        tvFilesStatus.text = "Fetching files from child's device..."

        scope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    SupabaseManager.client.from("files").select().decodeList<FileData>()
                }
                progressFiles.visibility = View.GONE

                if (files.isEmpty()) {
                    tvFilesStatus.text = "📭 No files yet.\nOpen Child App → tap 'Scan & Sync Files'."
                    return@launch
                }

                val totalKb  = files.sumOf { it.file_size_kb }
                val totalStr = if (totalKb > 1024) "${totalKb / 1024} MB" else "$totalKb KB"
                tvFilesStatus.text = "✅ ${files.size} files · $totalStr"

                // Update mini stats
                val statsRow = (panelFiles.getChildAt(0) as LinearLayout).getChildAt(0) as LinearLayout
                (statsRow.getChildAt(0) as LinearLayout).let { card ->
                    (card.getChildAt(0) as TextView).text = "${files.size}"
                }
                (statsRow.getChildAt(1) as LinearLayout).let { card ->
                    (card.getChildAt(0) as TextView).text = totalStr
                }
                (statsRow.getChildAt(2) as LinearLayout).let { card ->
                    (card.getChildAt(0) as TextView).text = "✅"
                }

                val grouped = files.groupBy { it.category }
                val order   = listOf("Images", "Videos", "Audio", "Documents", "Others")
                val colors  = mapOf("Images" to PINK, "Videos" to BLUE, "Audio" to TEAL, "Documents" to GREEN, "Others" to PURPLE)
                val icons   = mapOf("Images" to "🖼️", "Videos" to "🎬", "Audio" to "🎵", "Documents" to "📄", "Others" to "📦")

                for (cat in order) {
                    val catFiles = grouped[cat] ?: continue
                    filesContainer.addView(buildCategoryCard(icons[cat]!!, cat, catFiles, colors[cat]!!))
                }

            } catch (e: Exception) {
                progressFiles.visibility = View.GONE
                tvFilesStatus.text = "❌ Error: ${e.message}"
            }
        }
    }

    // ── CATEGORY CARD ─────────────────────────────────────────────────────────
    private fun buildCategoryCard(icon: String, title: String, files: List<FileData>, color: Int): View {
        val totalKb  = files.sumOf { it.file_size_kb }
        val totalStr = if (totalKb > 1024) "${totalKb / 1024} MB" else "$totalKb KB"
        val uploaded = files.count { it.storage_url.isNotEmpty() }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = roundRect(CARD, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 20 }
        }

        // ── Header ──────────────────────────────────────────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background  = roundRectTop(color, 16)
            setPadding(40, 32, 40, 32)
            gravity     = Gravity.CENTER_VERTICAL
        }

        // Icon circle
        val iconCircle = TextView(this).apply {
            text       = icon
            textSize   = 22f
            gravity    = Gravity.CENTER
            background = roundRect(Color.parseColor("#33FFFFFF"), 32)
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = 20 }
        }

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        infoCol.addView(TextView(this).apply {
            text     = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        infoCol.addView(TextView(this).apply {
            text     = "${files.size} files · $totalStr · $uploaded uploaded"
            textSize = 11f
            setTextColor(Color.parseColor("#CCFFFFFF"))
            setPadding(0, 4, 0, 0)
        })

        val chevron = TextView(this).apply {
            text     = "▼"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha    = 0.7f
        }

        header.addView(iconCircle)
        header.addView(infoCol)
        header.addView(chevron)

        // ── File list ────────────────────────────────────────────
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility  = View.GONE
        }

        files.take(100).forEachIndexed { idx, file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 20, 32, 20)
                setBackgroundColor(if (idx % 2 == 0) CARD else Color.parseColor("#161824"))
            }

            // Top row: icon + name + size
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(this).apply {
                text     = fileIcon(file.mime_type)
                textSize = 20f
                setPadding(0, 0, 16, 0)
            })
            val nameCol = LinearLayout(this).apply {
                orientation  = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            nameCol.addView(TextView(this).apply {
                text     = file.file_name
                setTextColor(TEXT_PRI)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            nameCol.addView(TextView(this).apply {
                text     = file.mime_type.uppercase() + " · " +
                           (if (file.file_size_kb > 1024) "${file.file_size_kb / 1024} MB" else "${file.file_size_kb} KB")
                setTextColor(TEXT_SEC)
                textSize = 11f
            })
            topRow.addView(nameCol)
            row.addView(topRow)

            // Action buttons
            if (file.storage_url.isNotEmpty()) {
                val btnRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 14, 0, 0)
                }
                btnRow.addView(actionBtn("🌐 Open", BLUE)   { openUrl(file.storage_url) })
                btnRow.addView(actionBtn("⬇ Download", GREEN) { downloadFile(file.file_name, file.storage_url, file.mime_type) })
                row.addView(btnRow)
            } else {
                row.addView(TextView(this).apply {
                    text     = "⏳ File pending upload from child device"
                    setTextColor(ORANGE)
                    textSize = 11f
                    setPadding(0, 10, 0, 0)
                })
            }

            listContainer.addView(row)

            // Divider
            if (idx < files.size - 1) {
                listContainer.addView(View(this).apply {
                    setBackgroundColor(Color.parseColor("#20FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                })
            }
        }

        if (files.size > 100) {
            listContainer.addView(TextView(this).apply {
                text     = "  … and ${files.size - 100} more files"
                setTextColor(TEXT_SEC)
                textSize = 12f
                setPadding(40, 20, 40, 20)
                gravity  = Gravity.CENTER
            })
        }

        // Toggle expand
        var expanded = false
        header.setOnClickListener {
            expanded = !expanded
            chevron.text     = if (expanded) "▲" else "▼"
            listContainer.visibility = if (expanded) View.VISIBLE else View.GONE
        }

        card.addView(header)
        card.addView(listContainer)
        return card
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private fun fileIcon(mime: String) = when {
        mime.startsWith("image")       -> "🖼️"
        mime.startsWith("video")       -> "🎬"
        mime.startsWith("audio")       -> "🎵"
        mime == "application/pdf"      -> "📑"
        mime.contains("word")          -> "📝"
        mime.contains("excel") || mime.contains("sheet") -> "📊"
        mime == "text/plain"           -> "📄"
        else                           -> "📦"
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun downloadFile(name: String, url: String, mimeType: String) {
        try {
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(name)
                setDescription("Downloading from child's device")
                setMimeType(mimeType)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(this, "⬇ Downloading $name…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun actionBtn(label: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text     = label
        textSize = 11f
        setTextColor(Color.WHITE)
        background   = roundRect(color, 8)
        setPadding(28, 12, 28, 12)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.marginEnd = 12 }
        setOnClickListener { onClick() }
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize  = 13f
        setTypeface(null, Typeface.BOLD)
        setTextColor(TEXT_SEC)
        letterSpacing = 0.1f
        setPadding(0, 36, 0, 12)
    }

    private fun bodyText(text: String) = TextView(this).apply {
        this.text = text
        textSize  = 14f
        setTextColor(TEXT_PRI)
    }

    private fun glassCard(body: TextView, accentColor: Int, subtitle: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = roundRect(CARD, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        // Top accent bar
        card.addView(View(this).apply {
            setBackgroundColor(accentColor)
            background = roundRectTop(accentColor, 14)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 6)
        })
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }
        inner.addView(TextView(this).apply {
            text      = subtitle.uppercase()
            textSize  = 10f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.15f
        })
        inner.addView(body.apply { setPadding(0, 8, 0, 0) })
        card.addView(inner)
        return card
    }

    private fun roundRect(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.dpToPx().toFloat()
        }
    }

    private fun roundRectTop(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            val r = radius.dpToPx().toFloat()
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()
}
