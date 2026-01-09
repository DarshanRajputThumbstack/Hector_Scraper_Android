package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class UserProfileFlow(
    private val service: AccessibilityService,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    private var isUserProfileRedirect = false
    private var isFlowCompleted = false

    fun startFlow() {
        if (isFlowCompleted) return

        if (!isUserProfileRedirect) {
            clickUserAccountIcon()
        }
    }

    private fun clickUserAccountIcon(): Boolean {
        val root = service.rootInActiveWindow ?: return false

        fun traverse(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            if (
                node.className == "android.widget.ImageView" &&
                node.isClickable &&
                node.contentDescription
                    ?.toString()
                    ?.equals("Show account", ignoreCase = true) == true
            ) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                isUserProfileRedirect = true

                Log.e("A11Y", "✅ User profile opened")

                handler.postDelayed({
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    markCompleted()
                }, 1500)

                return true
            }

            for (i in 0 until node.childCount) {
                if (traverse(node.getChild(i))) return true
            }
            return false
        }

        return traverse(root)
    }

    private fun markCompleted() {
        isFlowCompleted = true
        Log.e("A11Y", "✅ UserProfile flow completed")
        onFlowCompleted.invoke()
    }
}