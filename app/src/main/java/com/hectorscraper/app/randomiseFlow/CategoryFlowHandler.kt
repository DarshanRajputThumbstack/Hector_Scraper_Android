package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class CategoryFlowHandler(
    private val service: AccessibilityService,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    private var isCategoryRedirected = false
    private var isFlowCompleted = false

    fun startFlow() {
        if (isFlowCompleted) return

        val root = service.rootInActiveWindow ?: return
        if (!isCategoryRedirected) {
            clickCategoriesButton(root)
        }
    }

    private fun clickCategoriesButton(root: AccessibilityNodeInfo) {

        val containers = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/item_container"
        )

        if (containers.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No item_container found")
            return
        }

        for (container in containers) {

            val categoryText =
                container.findAccessibilityNodeInfosByText("Categories")

            if (!categoryText.isNullOrEmpty()) {

                Log.e("A11Y", "✅ Categories clicked")

                isCategoryRedirected = true
                container.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                handler.postDelayed({
                    service.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_BACK
                    )
                    markCompleted()
                }, 1500)

                return
            }
        }

        Log.e("A11Y", "❌ Categories container not found")
    }

    private fun markCompleted() {
        isFlowCompleted = true
        Log.e("A11Y", "✅ Category flow completed")
        onFlowCompleted.invoke()
    }
}