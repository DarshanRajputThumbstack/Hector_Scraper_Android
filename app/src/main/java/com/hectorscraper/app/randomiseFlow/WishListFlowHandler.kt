package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class WishListFlowHandler(
    private val service: AccessibilityService,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "WishListFlow"
    }

    private var isWishlistClicked = false
    private var isStarted = false

    // --------------------------------------------------
    // ENTRY POINT
    // --------------------------------------------------

    fun start() {
        if (isStarted) return
        isStarted = true
        clickWishlistIconOnce()
    }

    // --------------------------------------------------
    // STEP 1: Click wishlist icon
    // --------------------------------------------------

    private fun clickWishlistIconOnce() {
        if (isWishlistClicked) {
            Log.d(TAG, "⏭ Wishlist already clicked, skipping")
            return
        }

        val root = service.rootInActiveWindow ?: run {
            finishFlow()
            return
        }

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/wishlist_bookmark_box"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e(TAG, "❌ wishlist_bookmark_box not found")
            finishFlow()
            return
        }

        val node = nodes.first()

        val clicked = if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            clickNodeOrParent(node)
        }

        if (clicked) {
            isWishlistClicked = true
            Log.e(TAG, "❤️ Wishlist clicked")

            handler.postDelayed(
                { goBackToHomeFromWishlist() },
                2500
            )
        } else {
            Log.e(TAG, "❌ Wishlist node not clickable")
            finishFlow()
        }
    }

    // --------------------------------------------------
    // STEP 2: Go back to home
    // --------------------------------------------------

    private fun goBackToHomeFromWishlist() {
        service.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK
        )
        Log.e(TAG, "🔙 Back from Wishlist")

        handler.postDelayed(
            { finishFlow() },
            1500
        )
    }

    // --------------------------------------------------
    // FINISH
    // --------------------------------------------------

    private fun finishFlow() {
        isWishlistClicked = false
        isStarted = false
        onFlowCompleted.invoke()
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

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
}