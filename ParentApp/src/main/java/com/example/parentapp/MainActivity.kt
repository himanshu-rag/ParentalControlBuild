package com.example.parentapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MainActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a FrameLayout as the root view to hold the WebView
        val layout = FrameLayout(this)
        layout.setBackgroundColor(Color.parseColor("#0A0C14")) // Match dark theme background

        // Initialize WebView
        webView = WebView(this)
        webView.setBackgroundColor(Color.parseColor("#0A0C14"))
        
        // Configure WebView settings to support modern React/Vite apps
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Prevent opening links in external browser
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }
        
        // Add support for alerts and other JS dialogs if needed
        webView.webChromeClient = WebChromeClient()

        // Load the live GitHub Pages dashboard
        webView.loadUrl("https://himanshu-rag.github.io/ParentalControlBuild/")

        layout.addView(webView)
        setContentView(layout)
    }

    // Handle the physical back button to go back in the web history instead of closing the app
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
