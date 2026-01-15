package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo

class HotDealFlowHandler(
    private val service: AccessibilityService,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "HotDealFlow"
    }

    private var isHotDealFlowStarted = false
    private var isFlowCompleted = false

    fun startFlow() {
        if (isFlowCompleted) return

        if (!isHotDealFlowStarted) {
            clickSeeAllDirect()
        }
    }

    private fun clickSeeAllDirect(): Boolean {
        val root = service.rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByText("See All")
        Log.e(TAG, "🔍 Found ${nodes.size} 'See All' nodes")

        nodes.forEach { node ->
            val clickable =
                if (node.isClickable) node else findClickableParent(node)

            clickable?.let {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Clicked See All (direct)")
                isHotDealFlowStarted = true

                handler.postDelayed({
                    randomScrollThenClick()
                }, 1500)

                return true
            }
        }
        return false
    }

    private fun randomScrollThenClick() {
        val metrics = service.resources.displayMetrics

        val startX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.75f
        val endY = metrics.heightPixels * 0.25f

        val totalScrolls = (1..5).random()
        var currentScroll = 0

        Log.e(TAG, "🎲 Will scroll $totalScrolls times")

        fun swipeOnce() {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }

            service.dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, (450..700).random().toLong()
                    )
                ).build(),
                null,
                null
            )
            currentScroll++
        }

        fun performNext() {
            if (currentScroll < totalScrolls) {
                swipeOnce()
                handler.postDelayed(
                    { performNext() },
                    (1200..2200).random().toLong()
                )
            } else {
                handler.postDelayed(
                    { clickRandomHotDealItem() },
                    (800..1500).random().toLong()
                )
            }
        }

        performNext()
    }

    private fun clickRandomHotDealItem(): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val productNodes = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            if (
                node.isClickable &&
                node.className == "android.view.ViewGroup" &&
                node.childCount > 2
            ) {
                productNodes.add(node)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)

        if (productNodes.isEmpty()) {
            Log.e(TAG, "❌ No product items found")
            return false
        }

        productNodes.random()
            .performAction(AccessibilityNodeInfo.ACTION_CLICK)

        Log.e(TAG, "✅ Clicked random Hot Deal item")

        handler.postDelayed({
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            handler.postDelayed({
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                Log.e(TAG, "✅ Hot Deal flow completed")
                onFlowCompleted.invoke()
            }, 1500)
        }, 2000)

        return true
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