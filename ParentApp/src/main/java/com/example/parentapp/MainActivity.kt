package com.example.parentapp

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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

    // ── Colour palette ────────────────────────────────────────────────────────
    private val BG       = Color.parseColor("#0F1117")
    private val CARD     = Color.parseColor("#1A1D27")
    private val ACCENT   = Color.parseColor("#6C63FF")
    private val TEXT_PRI = Color.parseColor("#F0F0F0")
    private val TEXT_SEC = Color.parseColor("#9E9EB0")
    private val GREEN    = Color.parseColor("#43E97B")
    private val ORANGE   = Color.parseColor("#FFA726")
    private val BLUE     = Color.parseColor("#42A5F5")
    private val PINK     = Color.parseColor("#EC407A")
    private val PURPLE   = Color.parseColor("#AB47BC")
    private val TEAL     = Color.parseColor("#26C6DA")
    private val RED      = Color.parseColor("#EF5350")

    // ── Live views ───────────────────────────────────────────────────────────
    private lateinit var tvLocation   : TextView
    private lateinit var tvNotif      : TextView
    private lateinit var tvAppUsage   : TextView
    private lateinit var filesContainer: LinearLayout
    private lateinit var tvFilesStatus : TextView
    private lateinit var progressFiles : ProgressBar

    // ── Tab views ─────────────────────────────────────────────────────────────
    private lateinit var tabDashboard : TextView
    private lateinit var tabFiles     : TextView
    private lateinit var panelDashboard: View
    private lateinit var panelFiles    : View

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this).apply { setBackgroundColor(BG) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 80)
        }

        page.addView(buildHeader())
        page.addView(buildTabBar())

        panelDashboard = buildDashboardPanel()
        panelFiles     = buildFilesPanel()

        page.addView(panelDashboard)
        page.addView(panelFiles)

        root.addView(page)
        setContentView(root)

        switchTab(isDashboard = true)
        startDashboard()
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private fun buildHeader(): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            setPadding(56, 72, 56, 40)
        }
        c.addView(TextView(this).apply {
            text = "👨‍👩‍👧  Parent Dashboard"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT_PRI)
        })
        c.addView(TextView(this).apply {
            text = "Real-time monitoring & file viewer"
            textSize = 13f
            setTextColor(TEXT_SEC)
            setPadding(0, 8, 0, 0)
        })
        c.addView(View(this).apply {
            setBackgroundColor(ACCENT)
            layoutParams = LinearLayout.LayoutParams(120, 4).also { it.topMargin = 20 }
        })
        return c
    }

    // ── TAB BAR ───────────────────────────────────────────────────────────────
    private fun buildTabBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(CARD)
        }
        tabDashboard = tabLabel("📡  Dashboard")
        tabFiles     = tabLabel("📁  Files")

        tabDashboard.setOnClickListener { switchTab(true) }
        tabFiles.setOnClickListener    { switchTab(false); loadFiles() }

        bar.addView(tabDashboard, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(tabFiles,     LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return bar
    }

    private fun tabLabel(label: String) = TextView(this).apply {
        text = label
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(0, 36, 0, 36)
        setTextColor(TEXT_SEC)
    }

    private fun switchTab(isDashboard: Boolean) {
        panelDashboard.visibility = if (isDashboard) View.VISIBLE else View.GONE
        panelFiles.visibility     = if (isDashboard) View.GONE else View.VISIBLE

        tabDashboard.setTextColor(if (isDashboard) ACCENT else TEXT_SEC)
        tabDashboard.setTypeface(null, if (isDashboard) Typeface.BOLD else Typeface.NORMAL)
        tabFiles.setTextColor(if (!isDashboard) ACCENT else TEXT_SEC)
        tabFiles.setTypeface(null, if (!isDashboard) Typeface.BOLD else Typeface.NORMAL)
    }

    // ── DASHBOARD PANEL ───────────────────────────────────────────────────────
    private fun buildDashboardPanel(): View {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 0)
        }
        p.addView(sectionLabel("📍 Live Location"))
        tvLocation = bodyText("Waiting for location...")
        p.addView(infoCard(tvLocation, GREEN))

        p.addView(sectionLabel("🔔 Latest Notification"))
        tvNotif = bodyText("No notifications yet")
        p.addView(infoCard(tvNotif, ACCENT))

        p.addView(sectionLabel("📊 App Usage Today"))
        tvAppUsage = bodyText("Loading...")
        p.addView(infoCard(tvAppUsage, ORANGE))
        return p
    }

    // ── FILES PANEL ───────────────────────────────────────────────────────────
    private fun buildFilesPanel(): View {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 0)
        }

        // Refresh button
        val btn = Button(this).apply {
            text = "🔄  Refresh Files"
            setTextColor(Color.WHITE)
            setBackgroundColor(ACCENT)
            setOnClickListener { loadFiles() }
        }
        p.addView(btn)

        progressFiles = ProgressBar(this).apply { visibility = View.GONE }
        p.addView(progressFiles)

        tvFilesStatus = TextView(this).apply {
            text = "Tap Refresh to load files"
            setTextColor(TEXT_SEC)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        p.addView(tvFilesStatus)

        filesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        p.addView(filesContainer)
        return p
    }

    // ── DASHBOARD LOGIC ───────────────────────────────────────────────────────
    private fun startDashboard() {
        val dm = DashboardManager(
            onLocationUpdated     = { loc   -> runOnUiThread { tvLocation.text = "Lat: ${loc.latitude}\nLng: ${loc.longitude}" } },
            onNotificationReceived = { notif -> runOnUiThread { tvNotif.text = "📦 ${notif.package_name}\n📌 ${notif.title}\n💬 ${notif.text}" } }
        )
        dm.startListening()
        scope.launch {
            val list = withContext(Dispatchers.IO) { dm.fetchAppUsage() }
            val top  = list.sortedByDescending { it.time_spent_seconds }.take(5)
            tvAppUsage.text = if (top.isEmpty()) "No data yet."
            else top.joinToString("\n") { "▸ ${it.package_name}  (${it.time_spent_seconds}s)" }
        }
    }

    // ── FILE LOADER ───────────────────────────────────────────────────────────
    private fun loadFiles() {
        filesContainer.removeAllViews()
        progressFiles.visibility = View.VISIBLE
        tvFilesStatus.text = "Fetching files..."

        scope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    SupabaseManager.client.from("files")
                        .select()
                        .decodeList<FileData>()
                }
                progressFiles.visibility = View.GONE

                if (files.isEmpty()) {
                    tvFilesStatus.text = "No files yet. Open the Child App and tap 'Scan & Sync'."
                    return@launch
                }

                tvFilesStatus.text = "✅ ${files.size} files on child's device"

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

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 24 }
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(color)
            setPadding(40, 28, 40, 28)
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "$icon  $title"
            textSize = 17f
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

        // File list (collapsible)
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        files.take(100).forEachIndexed { idx, file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
                setBackgroundColor(if (idx % 2 == 0) CARD else Color.parseColor("#14161E"))
            }

            // File name + size row
            val nameRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            nameRow.addView(TextView(this).apply {
                text = "📄 ${file.file_name}"
                setTextColor(TEXT_PRI)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            nameRow.addView(TextView(this).apply {
                text = if (file.file_size_kb > 1024) "${file.file_size_kb / 1024}MB" else "${file.file_size_kb}KB"
                setTextColor(TEXT_SEC)
                textSize = 12f
            })
            row.addView(nameRow)

            // Open / Download buttons (only if file was uploaded to storage)
            if (file.storage_url.isNotEmpty()) {
                val btnRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 12, 0, 0)
                }
                // Open in browser button
                btnRow.addView(buildSmallButton("🌐 Open", BLUE) {
                    openUrl(file.storage_url)
                })
                // Download button
                btnRow.addView(buildSmallButton("⬇️ Download", GREEN) {
                    downloadFile(file.file_name, file.storage_url, file.mime_type)
                })
                row.addView(btnRow)
            } else {
                row.addView(TextView(this).apply {
                    text = "  ⚠️ Not yet uploaded to storage"
                    setTextColor(ORANGE)
                    textSize = 11f
                    setPadding(0, 8, 0, 0)
                })
            }

            listContainer.addView(row)
        }

        if (files.size > 100) {
            listContainer.addView(TextView(this).apply {
                text = "  … and ${files.size - 100} more files"
                setTextColor(TEXT_SEC)
                textSize = 12f
                setPadding(40, 16, 40, 16)
            })
        }

        var expanded = false
        header.setOnClickListener {
            expanded = !expanded
            listContainer.visibility = if (expanded) View.VISIBLE else View.GONE
        }

        card.addView(header)
        card.addView(listContainer)
        return card
    }

    // ── OPEN / DOWNLOAD HELPERS ───────────────────────────────────────────────
    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun downloadFile(fileName: String, url: String, mimeType: String) {
        try {
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("Downloading from child's device")
                setMimeType(mimeType)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(req)
            Toast.makeText(this, "⬇️ Downloading $fileName...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── WIDGET HELPERS ────────────────────────────────────────────────────────
    private fun buildSmallButton(label: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 11f
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        setPadding(24, 8, 24, 8)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.marginEnd = 16 }
        setOnClickListener { onClick() }
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTypeface(null, Typeface.BOLD)
        setTextColor(TEXT_PRI)
        setPadding(0, 32, 0, 12)
    }

    private fun bodyText(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(TEXT_PRI)
    }

    private fun infoCard(body: TextView, accentColor: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(CARD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8 }
        }
        card.addView(View(this).apply {
            setBackgroundColor(accentColor)
            layoutParams = LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT)
        })
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            addView(body)
        })
        return card
    }
}
