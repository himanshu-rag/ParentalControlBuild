package com.example.parentapp

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.parentapp.models.FileData
import com.example.parentapp.services.DashboardManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {

    // ── colour palette ────────────────────────────────────────────────────────
    private val BG         = Color.parseColor("#0F1117")
    private val CARD       = Color.parseColor("#1A1D27")
    private val ACCENT     = Color.parseColor("#6C63FF")
    private val ACCENT2    = Color.parseColor("#FF6584")
    private val TEXT_PRI   = Color.parseColor("#F0F0F0")
    private val TEXT_SEC   = Color.parseColor("#9E9EB0")
    private val GREEN      = Color.parseColor("#43E97B")
    private val ORANGE     = Color.parseColor("#FFA726")
    private val BLUE       = Color.parseColor("#42A5F5")
    private val PINK       = Color.parseColor("#EC407A")
    private val PURPLE     = Color.parseColor("#AB47BC")
    private val TEAL       = Color.parseColor("#26C6DA")

    // ── live views ───────────────────────────────────────────────────────────
    private lateinit var tvLocation    : TextView
    private lateinit var tvNotif       : TextView
    private lateinit var tvAppUsage    : TextView
    private lateinit var filesContainer: LinearLayout
    private lateinit var tvFilesStatus : TextView
    private lateinit var progressFiles : ProgressBar

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this).apply { setBackgroundColor(BG) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 80)
        }

        page.addView(buildHeader())
        page.addView(buildLiveSection())
        page.addView(buildFilesSection())

        root.addView(page)
        setContentView(root)

        startDashboard()
        loadFiles()
    }

    // ── HEADER ───────────────────────────────────────────────────────────────
    private fun buildHeader(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            setPadding(56, 72, 56, 48)
        }
        container.addView(TextView(this).apply {
            text = "👨‍👩‍👧 Parent Dashboard"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT_PRI)
        })
        container.addView(TextView(this).apply {
            text = "Monitoring child's device in real time"
            textSize = 13f
            setTextColor(TEXT_SEC)
            setPadding(0, 8, 0, 0)
        })

        // Divider
        container.addView(View(this).apply {
            setBackgroundColor(ACCENT)
            layoutParams = LinearLayout.LayoutParams(120, 4).also { it.topMargin = 24 }
        })
        return container
    }

    // ── LIVE SECTION (location / notif / usage) ───────────────────────────────
    private fun buildLiveSection(): View {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 0)
        }

        section.addView(sectionLabel("📡 Live Monitoring"))

        // Location card
        tvLocation = infoText("📍 Waiting for location...")
        section.addView(infoCard("Location", tvLocation, GREEN))

        // Latest notification card
        tvNotif = infoText("🔔 No notifications yet")
        section.addView(infoCard("Latest Notification", tvNotif, ACCENT))

        // App usage card
        tvAppUsage = infoText("📊 Loading usage stats...")
        section.addView(infoCard("App Usage Today", tvAppUsage, ORANGE))

        return section
    }

    // ── FILES SECTION ─────────────────────────────────────────────────────────
    private fun buildFilesSection(): View {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 0)
        }

        section.addView(sectionLabel("📁 Child's Files"))

        // Refresh button
        val btnRefresh = Button(this).apply {
            text = "🔄  Refresh File List"
            setTextColor(TEXT_PRI)
            setBackgroundColor(ACCENT)
            setPadding(24, 16, 24, 16)
            setOnClickListener { loadFiles() }
        }
        section.addView(btnRefresh)

        // Status / progress
        progressFiles = ProgressBar(this).apply { visibility = View.GONE }
        section.addView(progressFiles)

        tvFilesStatus = TextView(this).apply {
            text = "Loading files..."
            setTextColor(TEXT_SEC)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        section.addView(tvFilesStatus)

        // Category cards container
        filesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        section.addView(filesContainer)

        return section
    }

    // ── DASHBOARD (location, notif, usage) ────────────────────────────────────
    private fun startDashboard() {
        val dm = DashboardManager(
            onLocationUpdated = { loc ->
                runOnUiThread {
                    tvLocation.text = "📍 Lat: ${loc.latitude}\n    Lng: ${loc.longitude}"
                }
            },
            onNotificationReceived = { notif ->
                runOnUiThread {
                    tvNotif.text = "📦 ${notif.package_name}\n📌 ${notif.title}\n💬 ${notif.text}"
                }
            }
        )
        dm.startListening()

        scope.launch {
            val usageList = withContext(Dispatchers.IO) { dm.fetchAppUsage() }
            val top5 = usageList.sortedByDescending { it.time_spent_seconds }.take(5)
            tvAppUsage.text = if (top5.isEmpty()) "No data yet."
            else top5.joinToString("\n") { "▸ ${it.package_name}  (${it.time_spent_seconds}s)" }
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
                    SupabaseManager.client.postgrest["files"]
                        .select()
                        .decodeList<FileData>()
                }

                progressFiles.visibility = View.GONE

                if (files.isEmpty()) {
                    tvFilesStatus.text = "No files synced yet. Make sure the Child App has run and granted file access."
                    return@launch
                }

                tvFilesStatus.text = "✅ ${files.size} files found on child's device"

                // Group by category
                val grouped = files.groupBy { it.category }
                val categoryOrder = listOf("Images", "Videos", "Audio", "Documents", "Others")
                val categoryColors = mapOf(
                    "Images"    to PINK,
                    "Videos"    to BLUE,
                    "Audio"     to TEAL,
                    "Documents" to GREEN,
                    "Others"    to PURPLE
                )
                val categoryIcons = mapOf(
                    "Images"    to "🖼️",
                    "Videos"    to "🎬",
                    "Audio"     to "🎵",
                    "Documents" to "📄",
                    "Others"    to "📦"
                )

                for (cat in categoryOrder) {
                    val catFiles = grouped[cat] ?: continue
                    val color = categoryColors[cat] ?: ACCENT
                    val icon  = categoryIcons[cat] ?: "📁"
                    filesContainer.addView(buildCategoryCard(icon, cat, catFiles, color))
                }

            } catch (e: Exception) {
                progressFiles.visibility = View.GONE
                tvFilesStatus.text = "❌ Error loading files: ${e.message}"
            }
        }
    }

    // ── CATEGORY CARD ─────────────────────────────────────────────────────────
    private fun buildCategoryCard(
        icon: String,
        title: String,
        files: List<FileData>,
        color: Int
    ): View {
        val totalKb  = files.sumOf { it.file_size_kb }
        val totalStr = if (totalKb > 1024) "${totalKb / 1024} MB" else "$totalKb KB"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            val p = 4.dpToPx()
            setPadding(0, 0, 0, p)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 24 }
            layoutParams = lp
        }

        // Header row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(color)
            setPadding(40, 28, 40, 28)
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "$icon  $title"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "${files.size} files · $totalStr"
            textSize = 12f
            setTextColor(Color.WHITE)
            alpha = 0.85f
        })

        // Expandable file list (show up to 50 files)
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            visibility = View.GONE
        }

        files.take(50).forEachIndexed { idx, file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(40, 20, 40, 20)
                setBackgroundColor(if (idx % 2 == 0) CARD else Color.parseColor("#14161E"))
            }
            row.addView(TextView(this).apply {
                text = file.file_name
                setTextColor(TEXT_PRI)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = if (file.file_size_kb > 1024) "${file.file_size_kb / 1024}MB"
                       else "${file.file_size_kb}KB"
                setTextColor(TEXT_SEC)
                textSize = 12f
                gravity = Gravity.END
            })
            listContainer.addView(row)
        }

        if (files.size > 50) {
            listContainer.addView(TextView(this).apply {
                text = "  … and ${files.size - 50} more files"
                setTextColor(TEXT_SEC)
                textSize = 12f
                setPadding(40, 16, 40, 16)
            })
        }

        // Toggle expand on header tap
        var expanded = false
        header.setOnClickListener {
            expanded = !expanded
            listContainer.visibility = if (expanded) View.VISIBLE else View.GONE
        }

        card.addView(header)
        card.addView(listContainer)
        return card
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(null, Typeface.BOLD)
        setTextColor(TEXT_PRI)
        setPadding(0, 32, 0, 16)
    }

    private fun infoText(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(TEXT_PRI)
    }

    private fun infoCard(label: String, body: TextView, accentColor: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 16 }
            layoutParams = lp
        }
        // Left accent strip
        val strip = View(this).apply {
            setBackgroundColor(accentColor)
            layoutParams = LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }
        content.addView(TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.12f
        })
        content.addView(body.apply { setPadding(0, 8, 0, 0) })
        inner.addView(strip)
        inner.addView(content)
        card.addView(inner)
        return card
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
