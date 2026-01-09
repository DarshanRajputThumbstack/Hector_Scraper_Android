package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class HotDealSearchFlowHandler(
    private val service: AccessibilityService,
    private val productKeywords: List<String>,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "HotDealSearchFlow"
    }

    private var isHotDealFlowStarted = false

    fun start() {
        if (!isHotDealFlowStarted) {
            val clicked = clickSeeAllDirectSearch()
            Log.e(TAG, "hotDealProductSearchFlow: $clicked")
        }
    }

    // --------------------------------------------------
    // STEP 1: Click See All
    // --------------------------------------------------

    private fun clickSeeAllDirectSearch(): Boolean {
        val root = service.rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByText("See All")
        Log.e(TAG, "🔍 Found ${nodes.size} 'See All' nodes")

        nodes.forEach { node ->
            val clickable =
                if (node.isClickable) node else findClickableParent(node)

            clickable?.let {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Clicked See All (search)")
                isHotDealFlowStarted = true

                handler.postDelayed(
                    { randomScrollThenGoTopAndSearch() },
                    1500
                )
                return true
            }
        }
        return false
    }

    // --------------------------------------------------
    // STEP 2: Random scroll down, then go back up
    // --------------------------------------------------

    private fun randomScrollThenGoTopAndSearch() {
        val metrics = service.resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val downStartY = metrics.heightPixels * 0.75f
        val downEndY = metrics.heightPixels * 0.25f

        val totalScrolls = (1..5).random()
        var currentScroll = 0

        Log.e(TAG, "🎲 Random scroll count = $totalScrolls")

        fun swipeDown() {
            val path = Path().apply {
                moveTo(centerX, downStartY)
                lineTo(centerX, downEndY)
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

        fun performScrollDown() {
            if (currentScroll < totalScrolls) {
                swipeDown()
                handler.postDelayed(
                    { performScrollDown() },
                    (1200..2200).random().toLong()
                )
            } else {
                handler.postDelayed(
                    { scrollUpSameTimes(totalScrolls) },
                    (1000..1600).random().toLong()
                )
            }
        }

        performScrollDown()
    }

    private fun scrollUpSameTimes(times: Int) {
        val metrics = service.resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val upStartY = metrics.heightPixels * 0.25f
        val upEndY = metrics.heightPixels * 0.75f

        var count = 0

        fun swipeUp() {
            val path = Path().apply {
                moveTo(centerX, upStartY)
                lineTo(centerX, upEndY)
            }

            service.dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, (500..750).random().toLong()
                    )
                ).build(),
                null,
                null
            )
            count++
        }

        fun performScrollUp() {
            if (count < times) {
                swipeUp()
                handler.postDelayed(
                    { performScrollUp() },
                    (1200..2000).random().toLong()
                )
            } else {
                handler.postDelayed(
                    { clickSearchIcon() },
                    (800..1400).random().toLong()
                )
            }
        }

        performScrollUp()
    }

    // --------------------------------------------------
    // STEP 3: Search flow
    // --------------------------------------------------

    private fun clickSearchIcon(): Boolean {
        val root = service.rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/iv_search_image"
        )

        nodes.forEach { node ->
            val clickable = findClickableParent(node)
            clickable?.let {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "🔍 Clicked search icon")

                handler.postDelayed(
                    { typeRandomly() },
                    1000
                )
                return true
            }
        }
        return false
    }

    private fun typeRandomly() {
        val root = service.rootInActiveWindow ?: return

        val edit = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        val keyword = productKeywords.random()

        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                keyword
            )
        }
        edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        Log.e(TAG, "⌨️ Typed keyword: $keyword")

        handler.postDelayed(
            { clickOnSearchResult() },
            (2000..3500).random().toLong()
        )
    }

    private fun clickOnSearchResult() {
        val root = service.rootInActiveWindow ?: return

        val rv = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/search_results"
        ).firstOrNull() ?: return

        if (rv.childCount == 0) return

        rv.getChild(0)
            .performAction(AccessibilityNodeInfo.ACTION_CLICK)

        handler.postDelayed(
            { clickRandomProductAndBack() },
            3000
        )
    }

    // --------------------------------------------------
    // STEP 4: Click product & exit
    // --------------------------------------------------

    private fun clickRandomProductAndBack() {
        val root = service.rootInActiveWindow ?: return

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/open_item_v3"
        )

        if (nodes.isNullOrEmpty()) return

        val product = nodes.random()
        val clickable = findClickableParent(product) ?: product

        if (clickable.isClickable) {
            clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            handler.postDelayed(
                { pressBackMultipleTimes(5) },
                (2500..3500).random().toLong()
            )
        }
    }

    private fun pressBackMultipleTimes(times: Int) {
        var count = 0

        fun pressNext() {
            if (count < times) {
                service.performGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_BACK
                )
                count++
                handler.postDelayed(
                    { pressNext() },
                    (800..1400).random().toLong()
                )
            } else {
                Log.e(TAG, "✅ Hot Deal Search flow completed")
                onFlowCompleted.invoke()
            }
        }
        pressNext()
    }

    // --------------------------------------------------

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