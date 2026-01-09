package com.hectorscraper.app.randomiseFlow

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class UserAddressFlowHandler(
    private val service: AccessibilityService,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onFlowCompleted: () -> Unit
) {

    companion object {
        private const val TAG = "UserAddressFlow"
        private const val ADDRESS_SELECTOR_ID =
            "in.swiggy.android.instamart:id/address_selector_area"
    }

    private var isUserAddressRedirect = false

    fun userAddressPageFlow() {
        if (!isUserAddressRedirect) {
            val clicked = clickAddressSelector()
            Log.d(TAG, "Address selector clicked = $clicked")
        }
    }

    private fun clickAddressSelector(): Boolean {
        val root = service.rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByViewId(ADDRESS_SELECTOR_ID)

        nodes.forEach { node ->
            val clickableNode =
                if (node.isClickable) node else findClickableParent(node)

            clickableNode?.let {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✅ Address selector clicked")
                isUserAddressRedirect = true

                handler.postDelayed({
                    service.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_BACK
                    )
                    Log.e(TAG, "✅ User Address flow completed")
                    onFlowCompleted.invoke()
                }, 1500)

                return true
            }
        }
        return false
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