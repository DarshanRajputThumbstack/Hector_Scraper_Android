package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class CategoryKeywordFlowHandler(
    private val service: AccessibilityService,
    private val categoryKeywords: List<String>,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "CategoryKeywordFlow"
    }

    private var isClickedOnKeyword = false

    fun start() {
        if (!isClickedOnKeyword) {
            clickRandomKeywordOnScreen()
        }
    }

    // --------------------------------------------------
    // STEP 1: Click random keyword
    // --------------------------------------------------

    private fun clickRandomKeywordOnScreen(): Boolean {
        val root = service.rootInActiveWindow ?: return false

        val keyword = categoryKeywords.random()
        Log.e(TAG, "🎯 Selected keyword: $keyword")

        fun traverse(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty() &&
                text.equals(keyword, ignoreCase = true)
            ) {
                val clickable = findClickableParent(node) ?: node
                if (clickable.isClickable) {
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    isClickedOnKeyword = true

                    Log.e(TAG, "✅ Clicked keyword: $keyword")

                    handler.postDelayed({
                        findSeeAllOrFallback()
                    }, 2000)
                    return true
                }
            }

            for (i in 0 until node.childCount) {
                if (traverse(node.getChild(i))) return true
            }
            return false
        }

        val clicked = traverse(root)
        if (!clicked) {
            Log.e(TAG, "❌ Keyword not found: $keyword")
        }
        return clicked
    }

    // --------------------------------------------------
    // STEP 2: Find See All or fallback
    // --------------------------------------------------

    private fun findSeeAllOrFallback() {
        var attempts = 0
        val maxAttempts = 10

        fun tryNext() {
            val currentRoot = service.rootInActiveWindow ?: return

            // 1️⃣ Try See All
            if (clickSeeAll(currentRoot)) {
                Log.e(TAG, "✅ See All clicked")
                return
            }

            // 2️⃣ Scroll & retry
            val scrollNode = findScrollableNode(currentRoot)
            if (scrollNode != null && attempts < maxAttempts) {
                attempts++
                Log.e(TAG, "⬇️ Scrolling attempt $attempts")
                scrollNode.performAction(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                )
                handler.postDelayed({ tryNext() }, 1200)
                return
            }

            // 3️⃣ Fallback
            Log.e(TAG, "⚠️ Using product fallback")
            clickProductViaIncrement(currentRoot)
        }

        tryNext()
    }

    // --------------------------------------------------
    // STEP 3: See All
    // --------------------------------------------------

    private fun clickSeeAll(root: AccessibilityNodeInfo): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText("See All")

        for (node in nodes) {
            val clickable = findClickableParent(node)
            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "👉 Clicked See All")

                handler.postDelayed(
                    { onFlowCompleted.invoke() },
                    2000
                )
                return true
            }
        }
        return false
    }

    // --------------------------------------------------
    // STEP 4: Fallback product click
    // --------------------------------------------------

    private fun clickProductViaIncrement(root: AccessibilityNodeInfo): Boolean {
        val incrementNodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/increment_button"
        )

        if (incrementNodes.isNullOrEmpty()) {
            Log.e(TAG, "❌ increment_button not found")
            return false
        }

        val increment = incrementNodes.random()
        var parent = increment.parent
        var depth = 0

        while (parent != null && depth < 6) {
            if (parent.viewIdResourceName ==
                "in.swiggy.android.instamart:id/image_layout"
            ) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Fallback product clicked")

                handler.postDelayed(
                    { onFlowCompleted.invoke() },
                    1500
                )
                return true
            }
            parent = parent.parent
            depth++
        }

        Log.e(TAG, "❌ image_layout not found")
        return false
    }

    // --------------------------------------------------

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val found = findScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }
}