package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class ShopListFlowHandler(
    private val service: AccessibilityService, private val handler: Handler = Handler(Looper.getMainLooper()), private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "ShopListFlow1"
    }

    private var isStarted = false

    // --------------------------------------------------
    // ENTRY POINT
    // --------------------------------------------------

    fun start() {
        if (isStarted) return
        isStarted = true
        clickShoppingListIcon()
    }

    // --------------------------------------------------
    // STEP 1: Click shopping list icon
    // --------------------------------------------------

    private fun clickShoppingListIcon() {
        val root = service.rootInActiveWindow ?: run {
            Log.e(TAG, "❌ rootInActiveWindow is null")
            finishFlow()
            return
        }

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/shoppingListIcon"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e(TAG, "❌ shoppingListIcon not found")
            finishFlow()
            return
        }

        val icon = nodes.first()

        when {
            icon.isClickable -> {
                icon.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Clicked shoppingListIcon directly")
                postWriteItClick()
            }

            findClickableParent(icon) != null -> {
                findClickableParent(icon)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Clicked shoppingListIcon via parent")
                postWriteItClick()
            }

            else -> {
                Log.e(TAG, "❌ shoppingListIcon not clickable")
                finishFlow()
            }
        }
    }

    private fun postWriteItClick() {
        handler.postDelayed(
            { clickWriteItComposeSafe() }, 2000
        )
    }

    // --------------------------------------------------
    // STEP 2: Click "Write it"
    // --------------------------------------------------

    private fun clickWriteItComposeSafe() {
        val root = service.rootInActiveWindow ?: run {
            finishFlow()
            return
        }

        val targetNode = findWriteItNode(root)

        if (targetNode == null) {
            Log.e(TAG, "❌ 'Write it' not found")
            finishFlow()
            return
        }

        // Try clickable parent
        findClickableParent(targetNode)?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e(TAG, "✅ Clicked parent of 'Write it'")

            handler.postDelayed(
                {
                    service.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_BACK
                    )
                    finishFlow()
                }, 3000
            )
            return
        }

        // Fallback: gesture tap
        clickByBounds(targetNode)
        Log.e(TAG, "✅ Clicked 'Write it' via gesture")
        finishFlow()
    }

    // --------------------------------------------------
    // FINISH
    // --------------------------------------------------

    private fun finishFlow() {
        isStarted = false
        onFlowCompleted.invoke()
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private fun findWriteItNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()

        if (text?.contains("write it") == true || desc?.contains("write it") == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val found = findWriteItNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent
            parent = parent.parent
        }
        return null
    }

    private fun clickByBounds(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder().addStroke(
                GestureDescription.StrokeDescription(path, 0, 100)
            ).build()

        service.dispatchGesture(gesture, null, null)
    }
}