package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class CategoryRandomProductFlowHandler(
    private val service: AccessibilityService,
    private val categories: List<String>,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "CategoryFlow3"
        private const val MAX_SCROLL_ATTEMPTS = 10
    }

    private var targetCategory: String? = null
    private var scrollAttempts = 0
    private var isStarted = false

    // --------------------------------------------------
    // ENTRY POINT
    // --------------------------------------------------

    fun start() {
        if (isStarted) return
        val root = service.rootInActiveWindow ?: return
        isStarted = true
        clickCategoriesButton(root)
    }

    // --------------------------------------------------
    // STEP 1: Click Categories button
    // --------------------------------------------------

    private fun clickCategoriesButton(root: AccessibilityNodeInfo) {
        val containers = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/item_container"
        )

        if (containers.isNullOrEmpty()) {
            Log.e(TAG, "❌ No item_container found")
            return
        }

        for (container in containers) {
            val children = container.findAccessibilityNodeInfosByText("Categories")
            if (children.isNotEmpty()) {
                container.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Categories clicked")

                handler.postDelayed(
                    { startRandomCategoryFlow() },
                    2500
                )
                return
            }
        }

        Log.e(TAG, "❌ Categories button not found")
    }

    // --------------------------------------------------
    // STEP 2: Pick random category
    // --------------------------------------------------

    private fun startRandomCategoryFlow() {
        if (targetCategory != null) return

        targetCategory = categories.random()
        scrollAttempts = 0

        Log.d(TAG, "🎯 Selected category: $targetCategory")

        findAndClickCategory()
    }

    // --------------------------------------------------
    // STEP 3: Find & click category (with scroll)
    // --------------------------------------------------

    private fun findAndClickCategory() {
        val category = targetCategory ?: return

        val nodes = findNodesByExactText(category)

        if (nodes.isNotEmpty()) {
            val node = nodes.random()
            val clicked = clickNodeOrParent(node)

            Log.d(TAG, "✅ Category clicked: $clicked")

            targetCategory = null
            scrollAttempts = 0

            handler.postDelayed(
                { clickRandomProduct() },
                3000
            )
            return
        }

        val scrollable = findScrollableNode(service.rootInActiveWindow)

        if (scrollable != null && scrollAttempts < MAX_SCROLL_ATTEMPTS) {
            scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollAttempts++

            Log.d(TAG, "⬇️ Scrolling for '$category' ($scrollAttempts)")

            handler.postDelayed(
                { findAndClickCategory() },
                1200
            )
        } else {
            Log.e(TAG, "❌ Category not found: $category")
            targetCategory = null
            scrollAttempts = 0
            finishFlow()
        }
    }

    // --------------------------------------------------
    // STEP 4: Click random product
    // --------------------------------------------------

    private fun clickRandomProduct() {
        val products = findProductNodesById()

        if (products.isEmpty()) {
            Log.e(TAG, "❌ No products found")
            finishFlow()
            return
        }

        val product = products.random()
        val clicked = clickNodeOrParent(product)

        Log.d(TAG, "🛒 Product clicked: $clicked")

        handler.postDelayed(
            { goBackToHome() },
            3000
        )
    }

    // --------------------------------------------------
    // STEP 5: Go back to home
    // --------------------------------------------------

    private fun goBackToHome() {
        var backCount = 0
        val maxBacks = 3

        fun backStep() {
            if (backCount >= maxBacks) {
                Log.e(TAG, "🏠 Returned to home")
                finishFlow()
                return
            }

            service.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK
            )
            backCount++

            Log.e(TAG, "🔙 Back pressed ($backCount)")

            handler.postDelayed(
                { backStep() },
                1200
            )
        }

        backStep()
    }

    // --------------------------------------------------
    // FINISH
    // --------------------------------------------------

    private fun finishFlow() {
        isStarted = false
        targetCategory = null
        scrollAttempts = 0
        onFlowCompleted.invoke()
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private fun findNodesByExactText(text: String): List<AccessibilityNodeInfo> {
        val root = service.rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByText(text)
            .filter { it.text?.toString() == text }
    }

    private fun findProductNodesById(): List<AccessibilityNodeInfo> {
        val root = service.rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/open_item_v3"
        ) ?: emptyList()
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
        }
        return false
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val found = findScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }
}