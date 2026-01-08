package com.hectorscraper.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.hectorscraper.app.utils.ExcelManager
import com.hectorscraper.app.utils.HectorScraper
import com.hectorscraper.app.utils.HectorScraper.Companion.killInstamartApp
import kotlin.random.Random

class MyAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_START_AUTOMATION = "ACTION_START_AUTOMATION"
        private const val TAG = "InstamartFlow"

        private const val INSTAMART_PACKAGE = "in.swiggy.android.instamart"
//        private const val INSTAMART_PACKAGE = "in.swiggy.android.instamart"

        var instance: MyAccessibilityService? = null
    }

    private var randomFlowCounter = 0

    private var viewCartRetryCount = 0
    private val MAX_VIEW_CART_RETRY = 5
    private var flowStartTime: Long = 0L
    private var flowElapsedTimeMs: Long = 0L   // ✅ STORED VALUE
    private var shouldStart = false
    private var waitForAppToLoad = false
    private var searchClicked = false

    //    private var isAddressStored = false
    private var searchTextTyped = false
    private var isUserProfileRedirect = false
    private var isCategoryRedirected = false
    private var isUserAddressRedirect = false
    private var isRandomFunCalled = false
    private var isHotDealFlow = false
    private var isRandomFlowDone = false
    private var isClickedOnKeyword = false
    private var isShopListFlow1 = false
    private var isWishlistClicked = false
    private var isShopListFlow2 = false
    private var isShopListFlow3 = false
    private var isCategoryFlow3 = false
    private var isWishListFlow = false
    private var typingScheduled = false
    private var appClosed = false

    private var lastQuantity = -1
    private var sameCountHits = 0
    private val MAX_SAME_COUNT = 2

    var city = ""
    var pincode = ""
    var darkStoreName: String? = ""
    var productName: String? = ""
    var productWeight: String? = ""
    var productMRP: String? = "0"
    var productSellingPrice: String? = "0"
    var productDiscountPercentage: String? = "0"
    var productInventory: String? = "0"
    var productBrand: String? = ""
    var darkStoreId = ""
    var darkStoreLocality = ""
    var darkStoreAddress = ""
    var darkStorePlusCode = ""
    var merchantId = ""
    var productId = ""
    var productUrl = ""
    var level1Category = ""
    var level2Category = ""
    var level3Category = ""
    var etaIdentifier = ""
    private var appDrawerScrollCount = 0
    private val MAX_APP_DRAWER_SCROLL = 6
    val categoryKeywords = listOf(
        "Winter", "Fresh", "Bhakti", "Gourmet", "Maxxsaver", "Pharmacy", "Electronics", "Home", "Beauty", "Kids", "Grocery", "Fashion"
    )

    private val shoppingItems = listOf(
        "Milk", "Bread", "Eggs", "Butter", "Sugar", "Salt", "Tea", "Coffee", "Rice", "Wheat flour", "Cooking oil", "Biscuits", "Chips", "Soap", "Shampoo"
    )

    val productKeywords = listOf(
        "oreo", "milk", "bread", "banana", "apple", "sugar", "rice", "oil", "chocolate", "chips", "biscuit", "coffee", "tea", "soap", "shampoo"
    )
    val categories = listOf(
        "Fresh Vegetables",
        "Fresh Fruits",
        "Atta, Rice and Dal",
        "Dairy, Bread and Eggs",
        "Masalas",
        "Oils and Ghee",
        "Cereals and Breakfast",
        "Meat and Seafood",
        "Cold Drinks and Juices",
        "Ice Creams and Frozen Desserts",
        "Chips and Namkeens",
        "Chocolates",
        "Biscuits and Cakes",
        "Tea, Coffee and Milk drinks",
        "Sauces and Spreads",
        "Sweet Corner",
        "Noodles, Pasta, Vermicelli",
        "Frozen Food",
        "Dry Fruits and Seeds Mix",
        "Paan Corner",
        "Bath and Body",
        "Hair Care",
        "Skincare",
        "Makeup",
        "Feminine Hygiene",
        "Sexual Wellness",
        "Health and Pharma",
        "Body Care",
        "Home and Kitchen",
        "Puja Store",
        "Cleaners and Repellents",
        "Toys and Stationary",
        "Electronics and Appliances",
        "Fashion",
        "Pet Supplies",
        "Sports and Fitness"
    )

    private val MAX_SCROLL_ATTEMPTS = 5
    private var scrollAttempts = 0
    private var targetCategory: String? = null

    private val handler = Handler(Looper.getMainLooper())

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e(TAG, "onReceive1: ${intent}")
            Log.e(TAG, "onReceive2 : ${intent?.action}")
            if (intent?.action == ACTION_START_AUTOMATION) {

                shouldStart = true
                waitForAppToLoad = true
                searchClicked = false
                searchTextTyped = false
                typingScheduled = false
                appClosed = false

                isCategoryRedirected = false
                isUserProfileRedirect = false
                isUserAddressRedirect = false
                isHotDealFlow = false
                isClickedOnKeyword = false
                isShopListFlow1 = false
                isShopListFlow2 = false
                isShopListFlow3 = false
                isCategoryFlow3 = false
                isWishListFlow = false
                isRandomFunCalled = false
                isRandomFlowDone = false

                handler.postDelayed({
                    waitForAppToLoad = false
                }, 4500)

                launchInstamart()

                Log.e(TAG, "🚀 Automation Triggered from Manifest Receiver")
            }
        }
    }

    // Register internal receiver for forwarded broadcasts
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            applicationContext.registerReceiver(
//                serviceReceiver, IntentFilter(ACTION_START_AUTOMATION), RECEIVER_NOT_EXPORTED
//            )
//        } else {
//            applicationContext.registerReceiver(serviceReceiver, IntentFilter(ACTION_START_AUTOMATION))
//        }
//        Log.e(TAG, "Service Connected & Internal Receiver Registered")

        val appCtx = applicationContext

        val filter = IntentFilter(ACTION_START_AUTOMATION)

        appCtx.registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED)

        Log.e(TAG, "📡 Receiver registered in AccessibilityService")
    }

    override fun onInterrupt() {
        Log.d(TAG, "⚠️ Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        unregisterReceiver(serviceReceiver)
        super.onDestroy()
        instance = null
        Log.e(TAG, "Service Destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!shouldStart || !pkg.contains(INSTAMART_PACKAGE)) return

        val type = event.eventType
        if (type !in listOf(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_FOCUSED
            )
        ) return

        val root = rootInActiveWindow ?: return


        handler.postDelayed({
            clickCloseOverlay()
            clickToBtnProcessed()
//            clickCloseButton()
//            if (!isWishListFlow) {
//                dumpTree(rootInActiveWindow)
//
//            }
        }, 3000)

        if (!searchClicked) {
            handler.postDelayed({
                if (!isRandomFunCalled) {
                    callRandomFlowRandomly(root)
                }
                if (isRandomFlowDone) {
                    handler.postDelayed({
                        if (!HectorScraper.isAddressStored) {
                            startFlowTimer()
                            clearAllValues()
                            extractAddressDetails()
                        }
                        clickSearchBar(root)
                    }, 2000)
                }
            }, 4000)
            return
        }

        if (searchClicked && !searchTextTyped) {
            if (!typingScheduled) {
                typingScheduled = true
                handler.postDelayed({
                    searchTextTyped = true
                    typeChocolate()
                }, 1000)
            }
            return
        }
    }

    private fun clickToBtnProcessed() {
        handler.postDelayed({ clickProceedButton() }, 1500)
    }

    fun clickProceedButton(): Boolean {
        val root = rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/btn_proceed"
        )

        if (nodes.isNullOrEmpty()) {
//            Log.e("A11Y", "❌ Proceed button not found")
            return false
        }

        for (node in nodes) {
            val clicked = clickNodeOrParent(node)
            if (clicked) {
                Log.e("A11Y", "✅ Proceed button clicked")
                return true
            }
        }

        Log.e("A11Y", "❌ Proceed button found but not clickable")
        return false
    }

    fun startFlowTimer() {
        flowStartTime = System.currentTimeMillis()
        flowElapsedTimeMs = 0L
        Log.e("A11Y_TIMER", "⏱ Timer started")
    }

    fun stopFlowTimer() {
        if (flowStartTime == 0L) {
            Log.e("A11Y_TIMER", "❌ Timer not started")
            return
        }

        val endTime = System.currentTimeMillis()
        flowElapsedTimeMs = endTime - flowStartTime   // ✅ STORE HERE

        Log.e(
            "A11Y_TIMER", "⏹ Timer stopped | Elapsed = $flowElapsedTimeMs ms (${flowElapsedTimeMs / 1000.0}s)"
        )

        flowStartTime = 0L
    }

    private fun callWishListFlow() {
        isWishListFlow = true
        clickWishlistIconOnce()
    }

    fun clickWishlistIconOnce() {
        if (isWishlistClicked) {
            Log.d("A11Y", "⏭ Wishlist already clicked, skipping")
            return
        }

        val root = rootInActiveWindow ?: return

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/wishlist_bookmark_box"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ wishlist_bookmark_box not found")
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
            Log.e("A11Y", "❤️ Wishlist clicked (by ID)")

            handler.postDelayed({
                goBackToHomeFromWishlist()
            }, 2500)
        } else {
            Log.e("A11Y", "❌ Wishlist node found but not clickable")
        }
    }

    private fun goBackToHomeFromWishlist() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        Log.e("A11Y", "🔙 Back from Wishlist")

        handler.postDelayed({
            isWishlistClicked = false
            isRandomFlowDone = true// reset for future runs
        }, 1500)
    }

    private fun callCategoryFlow3(root: AccessibilityNodeInfo) {
        clickCategoriesButtonFlow3(root)
        isCategoryFlow3 = true
    }

    fun clickCategoriesButtonFlow3(root: AccessibilityNodeInfo) {
        val containers = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/item_container"
        )

        if (containers.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No item_container found")
            return
        }

        for (container in containers) {
            val children = container.findAccessibilityNodeInfosByText("Categories")
            if (children.isNotEmpty()) {
                container.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Categories clicked")

                handler.postDelayed({
                    startRandomCategoryFlow()
                }, 2500)

                return
            }
        }

        Log.e("A11Y", "❌ Categories button not found")
    }

    private fun startRandomCategoryFlow() {
        if (targetCategory != null) {
            Log.d("A11Y", "⚠️ Category flow already running")
            return
        }

        targetCategory = categories.random()
        scrollAttempts = 0

        Log.d("A11Y", "🎯 Selected category: $targetCategory")

        findAndClickCategory()
    }

    private fun findAndClickCategory() {
        val category = targetCategory
        if (category.isNullOrEmpty()) {
            Log.e("A11Y", "⚠️ targetCategory is null, stopping flow")
            return
        }

        val nodes = findNodesByExactText(category)

        if (nodes.isNotEmpty()) {
            val node = nodes.random()
            val clicked = clickNodeOrParent(node)

            Log.d("A11Y", "✅ Category clicked: $clicked")

            // Stop category flow
            targetCategory = null
            scrollAttempts = 0

            handler.postDelayed({
                clickRandomProduct()
            }, 3000)

            return
        }

        val root = rootInActiveWindow
        val scrollable = findScrollableNode(root)

        if (scrollable != null && scrollAttempts < MAX_SCROLL_ATTEMPTS) {
            scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollAttempts++

            Log.d("A11Y", "⬇️ Scrolling for category '$category' ($scrollAttempts)")

            handler.postDelayed({
                findAndClickCategory()
            }, 1200)
        } else {
            Log.e("A11Y", "❌ Category not found after scrolling: $category")
            targetCategory = null
            scrollAttempts = 0
        }
    }

    fun clickRandomProduct() {
        val products = findProductNodesById()

        if (products.isEmpty()) {
            Log.e("A11Y", "❌ No products found")
            return
        }

        val product = products.random()
        val clicked = product.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        Log.d("A11Y", "🛒 Random product clicked: $clicked")

        handler.postDelayed({
            goBackToHome()
        }, 3000)
    }

    private fun goBackToHome() {
        var backCount = 0
        val maxBacks = 3

        fun backStep() {
            if (backCount >= maxBacks) {
                Log.e("A11Y", "🏠 Reached home (assumed)")
                isRandomFlowDone = true
                return
            }

            performGlobalAction(GLOBAL_ACTION_BACK)
            backCount++

            Log.e("A11Y", "🔙 Back pressed ($backCount)")

            handler.postDelayed({
                backStep()
            }, 1200)
        }

        backStep()
        if (backCount == maxBacks) {
            isRandomFlowDone = true
        }
    }

//    fun clickRandomProductFlow3() {
//        val products = findProductNodesById()
//
//        if (products.isEmpty()) {
//            Log.e("A11Y", "❌ No open_item_v3 products found")
//            return
//        }
//
//        val product = products.random()
//
//        val clicked = if (product.isClickable) {
//            product.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        } else {
//            clickNodeOrParent(product)
//        }
//
//        Log.d("A11Y", "🛒 Random product clicked: $clicked")
//    }

    fun findNodesByExactText(text: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.filter { it.text?.toString() == text }
    }

    fun findProductNodesById(): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()

        return root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/open_item_v3"
        ) ?: emptyList()
    }

    fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
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


    private fun callShopListFlow2() {
        clickShoppingListIconFlow2()
    }

    private fun clickShoppingListIconFlow2(): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is null")
            return false
        }

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/shoppingListIcon"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ shoppingListIcon not found")
            return false
        }

        val icon = nodes[0]

        // Try direct click
        if (icon.isClickable) {
            icon.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked shoppingListIcon directly")
            return true
        }

        // Try clickable parent
        val clickableParent = findClickableParent(icon)
        if (clickableParent != null) {
            clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked shoppingListIcon via parent")
            handler.postDelayed({ clickWriteItComposeSafeFlow2() }, 2000)
            return true
        }

        Log.e("A11Y", "❌ shoppingListIcon not clickable")
        return false
    }

    fun clickWriteItComposeSafeFlow2(): Boolean {
        val root = rootInActiveWindow ?: return false

        fun traverse(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null

            val text = node.text?.toString()?.lowercase()
            val desc = node.contentDescription?.toString()?.lowercase()

            if (text?.contains("write it") == true || desc?.contains("write it") == true) {
                return node
            }

            for (i in 0 until node.childCount) {
                val found = traverse(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val targetNode = traverse(root)

        if (targetNode == null) {
            Log.e("A11Y", "❌ 'Write it' not found even by traversal")
            return false
        }

        // Try clickable parent
        findClickableParent(targetNode)?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked parent of 'Write it'")
            handler.postDelayed({ clickAddYourItemsAndType() }, 3000)
            return true
        }

        // Fallback: tap by bounds
        clickByBounds(targetNode)
        Log.e("A11Y", "✅ Clicked 'Write it' via gesture tap")
        return true
    }

    fun clickAddYourItemsAndType(): Boolean {
        val root = rootInActiveWindow ?: return false

        fun findNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null

            val text = node.text?.toString()?.lowercase()
            val desc = node.contentDescription?.toString()?.lowercase()

            if (text?.contains("add your items") == true || desc?.contains("add your items") == true) {
                return node
            }

            for (i in 0 until node.childCount) {
                val found = findNode(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val target = findNode(root)

        if (target == null) {
            Log.e("A11Y", "❌ 'Add your items' not found")
            return false
        }

        // Try normal click
        findClickableParent(target)?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked 'Add your items'")
            handler.postDelayed({ typeRandomShoppingItem() }, 800)
            return true
        }

        // Fallback: gesture tap (Compose)
        clickByBounds(target)
        Log.e("A11Y", "✅ Clicked 'Add your items' via gesture")
        handler.postDelayed({ typeRandomShoppingItem() }, 800)
        return true
    }

//    private fun typeRandomShoppingItem() {
//        val root = rootInActiveWindow ?: return
//
//        val input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
//        if (input == null) {
//            Log.e("A11Y", "❌ Input field not focused")
//            return
//        }
//
//        val randomItem = shoppingItems.random()
//
//        // 👇 Add newline to trigger Enter
//        val args = Bundle().apply {
//            putCharSequence(
//                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
//                "$randomItem\n"
//            )
//        }
//
//        val success = input.performAction(
//            AccessibilityNodeInfo.ACTION_SET_TEXT,
//            args
//        )
//
//        Log.e("A11Y", "✍️ Typed item & pressed Enter: $randomItem | success=$success")
//
//        // 🔁 Fallback if Compose ignores newline
//        if (!success) {
//            pressEnter()
//        }
//    }

    private fun typeRandomShoppingItem() {
        val root = rootInActiveWindow ?: return
        val input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (input == null) {
            Log.e("A11Y", "❌ Input field not focused")
            return
        }

        val item = shoppingItems.random()

        // 1️⃣ Copy to clipboard
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText("item", item)
        )

        // 2️⃣ Ensure focus
        input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        // 3️⃣ Paste
        val pasted = input.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        Log.e("A11Y", "📋 Pasted item: $item | success=$pasted")

        // 4️⃣ Press enter after paste
        handler.postDelayed({
//            submitViaNewline(input)
            pressEnter1()
//            pressKeyboardDoneWithoutKeyEvent(input)
        }, 500)
    }

    fun pressEnter1() {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }


    private fun pressKeyboardDoneWithoutKeyEvent(input: AccessibilityNodeInfo) {
        val currentText = input.text?.toString() ?: return

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "$currentText\n"
            )
        }

        val success = input.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT, args
        )

        Log.e("A11Y", "✔ Submit via newline | success=$success")
    }

    private fun submitViaNewline(input: AccessibilityNodeInfo) {
        val text = input.text?.toString() ?: return

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "$text\n"
            )
        }

        input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        Log.e("A11Y", "⏎ Submitted item")
    }

    fun pressEnter() {
        val root = rootInActiveWindow ?: return
        val input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (input == null) {
            Log.e("A11Y", "❌ No focused input to press Enter")
            return
        }

        // ✅ Method 1: Try newline (works on Compose)
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input.text.toString() + "\n"
            )
        }

        val success = input.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT, args
        )

        Log.e("A11Y", "⏎ Enter via newline: $success")

        // 🔁 Fallback if newline doesn't submit
        if (!success) {
            pressEnterViaNewLine(root)
        }
    }

    private fun pressEnterViaNewLine(input: AccessibilityNodeInfo) {
        val text = input.text?.toString() ?: ""

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "$text\n"
            )
        }

        input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        Log.e("A11Y", "⏎ Enter via newline")
    }

    private fun callShopListFlow1() {
        clickShoppingListIcon()
        isShopListFlow1 = true
    }

    private fun clickShoppingListIcon(): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is null")
            return false
        }

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/shoppingListIcon"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ shoppingListIcon not found")
            return false
        }

        val icon = nodes[0]

        // Try direct click
        if (icon.isClickable) {
            icon.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked shoppingListIcon directly")
            return true
        }

        // Try clickable parent
        val clickableParent = findClickableParent(icon)
        if (clickableParent != null) {
            clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked shoppingListIcon via parent")
            handler.postDelayed({ clickWriteItComposeSafe() }, 2000)
            return true
        }

        Log.e("A11Y", "❌ shoppingListIcon not clickable")
        return false
    }

    fun clickWriteItComposeSafe(): Boolean {
        val root = rootInActiveWindow ?: return false

        fun traverse(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null

            val text = node.text?.toString()?.lowercase()
            val desc = node.contentDescription?.toString()?.lowercase()

            if (text?.contains("write it") == true || desc?.contains("write it") == true) {
                return node
            }

            for (i in 0 until node.childCount) {
                val found = traverse(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val targetNode = traverse(root)

        if (targetNode == null) {
            Log.e("A11Y", "❌ 'Write it' not found even by traversal")
            return false
        }

        // Try clickable parent
        findClickableParent(targetNode)?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked parent of 'Write it'")
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK)
                isRandomFlowDone = true
            }, 3000)
            return true
        }

        // Fallback: tap by bounds
        clickByBounds(targetNode)
        Log.e("A11Y", "✅ Clicked 'Write it' via gesture tap")
        return true
    }

    fun clickByBounds(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 100)).build()

        dispatchGesture(gesture, null, null)
    }

    fun callCategoryFlow2(): Boolean {
        val root = rootInActiveWindow ?: return false

        val keyword = categoryKeywords.random()
        Log.e("A11Y_KEYWORD", "🎯 Selected keyword: $keyword")

        fun traverse(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            val text = node.text?.toString()?.trim()

            if (!text.isNullOrEmpty() && text.equals(keyword, ignoreCase = true)) {
                val clickable = findClickableParent(node) ?: node
                if (clickable.isClickable) {
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    isClickedOnKeyword = true
                    handler.postDelayed({ findSeeAllOrFallbackFlow2(root) }, 2000)
                    Log.e("A11Y_KEYWORD", "✅ Clicked keyword: $keyword")
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
            Log.e("A11Y_KEYWORD", "❌ Keyword not found on screen: $keyword")
        }

        return clicked
    }

    fun findSeeAllOrFallbackFlow2(root: AccessibilityNodeInfo?) {
        if (root == null) return

        var attempts = 0
        val maxAttempts = 10

        fun tryNext() {
            val currentRoot = rootInActiveWindow ?: return

            // 1️⃣ Try clicking "See All"
            if (clickSeeAllFlow2(currentRoot)) {
                Log.e("A11Y_FLOW", "✅ See All clicked")
                return
            }

            // 2️⃣ Try scrolling
            val scrollNode = findScrollableNode(currentRoot)
            if (scrollNode != null && attempts < maxAttempts) {
                attempts++
                Log.e("A11Y_FLOW", "⬇️ Scrolling down attempt $attempts")
                scrollNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

                handler.postDelayed({ tryNext() }, 1200)
                return
            }

            // 3️⃣ Fallback: Click product via increment button
            Log.e("A11Y_FLOW", "⚠️ See All not found, using fallback")
            clickProductViaIncrementFlow2(currentRoot)
        }

        tryNext()
    }

    fun clickSeeAllFlow2(root: AccessibilityNodeInfo): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText("See All")

        for (node in nodes) {
            val clickable = findClickableParent(node)
            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "👉 Clicked See All")
                handler.postDelayed({ randomScrollThenClick() }, 2000)
                return true
            }
        }
        return false
    }

    fun clickProductViaIncrementFlow2(root: AccessibilityNodeInfo): Boolean {
        val incrementNodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/increment_button"
        )

        if (incrementNodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ increment_button not found")
            return false
        }

        val increment = incrementNodes.random()
        var parent = increment.parent
        var depth = 0

        // climb up until product layout
        while (parent != null && depth < 6) {
            if (parent.viewIdResourceName == "in.swiggy.android.instamart:id/image_layout") {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Clicked product via image_layout fallback")
                isRandomFlowDone = true
                return true
            }
            parent = parent.parent
            depth++
        }

        Log.e("A11Y", "❌ image_layout not found above increment_button")
        return false
    }

//    fun clickAnyKeywordOnScreen(): Boolean {
//        val shuffled = categoryKeywords.shuffled()
//
//        for (keyword in shuffled) {
//            if (clickKeyword(keyword)) return true
//        }
//        return false
//    }
//
//    fun clickKeyword(keyword: String): Boolean {
//        val root = rootInActiveWindow ?: return false
//        return clickRandomKeywordOnScreen()
//    }

    fun clickCloseOverlay(): Boolean {
        val root = rootInActiveWindow ?: return false

        fun traverse(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            if (node.className == "android.widget.Button" && node.contentDescription?.toString()?.equals("Close overlay", ignoreCase = true) == true) {
                val clickable = findClickableParent(node) ?: node
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                Log.e("A11Y", "❌ Clicked Close overlay")
                return true
            }

            for (i in 0 until node.childCount) {
                if (traverse(node.getChild(i))) return true
            }
            return false
        }

        return traverse(root)
    }

    fun findNodeByContentDesc(
        node: AccessibilityNodeInfo?, desc: String
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        Log.d("A11Y", "Node: class=${node.className}, desc=${node.contentDescription}, clickable=${node.isClickable}")

        if (node.contentDescription?.toString()?.equals(desc, ignoreCase = true) == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findNodeByContentDesc(node.getChild(i), desc)
            if (result != null) return result
        }

        return null
    }


    fun clickCloseButton() {
        val root = rootInActiveWindow
        if (root == null) {
            Log.e("A11Y", "Root window is null")
            return
        }

        val node = findNodeByContentDesc(root, "close")

        if (node == null) {
            Log.e("A11Y", "Close button NOT found")
            return
        }

        Log.d("A11Y", "Found close node: class=${node.className}, clickable=${node.isClickable}")

        if (node.isClickable) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d("A11Y", "Clicked close directly: $clicked")
            return
        }

        // Try clickable parent
        var parent = node.parent
        while (parent != null) {
            Log.d(
                "A11Y", "Checking parent: class=${parent.className}, clickable=${parent.isClickable}"
            )

            if (parent.isClickable) {
                val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("A11Y", "Clicked parent: $clicked")
                return
            }
            parent = parent.parent
        }

        Log.e("A11Y", "No clickable parent found for close button")
    }


    fun clickRandomKeywordOnScreen(): Boolean {
        val root = rootInActiveWindow ?: return false

        val keyword = categoryKeywords.random()
        Log.e("A11Y_KEYWORD", "🎯 Selected keyword: $keyword")

        fun traverse(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            val text = node.text?.toString()?.trim()

            if (!text.isNullOrEmpty() && text.equals(keyword, ignoreCase = true)) {
                val clickable = findClickableParent(node) ?: node
                if (clickable.isClickable) {
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    isClickedOnKeyword = true
                    handler.postDelayed({ findSeeAllOrFallback(root) }, 2000)
                    Log.e("A11Y_KEYWORD", "✅ Clicked keyword: $keyword")
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
            Log.e("A11Y_KEYWORD", "❌ Keyword not found on screen: $keyword")
        }

        return clicked
    }

    fun findSeeAllOrFallback(root: AccessibilityNodeInfo?) {
        if (root == null) return

        var attempts = 0
        val maxAttempts = 10

        fun tryNext() {
            val currentRoot = rootInActiveWindow ?: return

            // 1️⃣ Try clicking "See All"
            if (clickSeeAll(currentRoot)) {
                Log.e("A11Y_FLOW", "✅ See All clicked")
                return
            }

            // 2️⃣ Try scrolling
            val scrollNode = findScrollableNode(currentRoot)
            if (scrollNode != null && attempts < maxAttempts) {
                attempts++
                Log.e("A11Y_FLOW", "⬇️ Scrolling down attempt $attempts")
                scrollNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

                handler.postDelayed({ tryNext() }, 1200)
                return
            }

            // 3️⃣ Fallback: Click product via increment button
            Log.e("A11Y_FLOW", "⚠️ See All not found, using fallback")
            clickProductViaIncrement(currentRoot)
        }

        tryNext()
    }

    fun clickSeeAll(root: AccessibilityNodeInfo): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText("See All")

        for (node in nodes) {
            val clickable = findClickableParent(node)
            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "👉 Clicked See All")
                handler.postDelayed({ randomScrollThenClick() }, 2000)
                return true
            }
        }
        return false
    }

    fun clickProductViaIncrement(root: AccessibilityNodeInfo): Boolean {
        val incrementNodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/increment_button"
        )

        if (incrementNodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ increment_button not found")
            return false
        }

        val increment = incrementNodes.random()
        var parent = increment.parent
        var depth = 0

        // climb up until product layout
        while (parent != null && depth < 6) {
            if (parent.viewIdResourceName == "in.swiggy.android.instamart:id/image_layout") {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Clicked product via image_layout fallback")
                return true
            }
            parent = parent.parent
            depth++
        }

        Log.e("A11Y", "❌ image_layout not found above increment_button")
        return false
    }

    private fun hotDealProductSearchFlow(root: AccessibilityNodeInfo) {
        val xyz = clickSeeAllDirectSearch()
        Log.e(TAG, "hotDealProductSearchFlow: $xyz")
    }

    fun clickSeeAllDirectSearch(): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is NULL")
            return false
        }

        val nodes = root.findAccessibilityNodeInfosByText("See All")
        Log.e("A11Y", "🔍 Found ${nodes.size} 'See All' nodes")

        nodes.forEach { node ->
            val clickable = if (node.isClickable) node else findClickableParent(node)

            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Clicked See All (direct)")
                isHotDealFlow = true
                handler.postDelayed({ randomScrollThenGoTopAndSearch() }, 1500)
                return true
            }
        }

        Log.e("A11Y", "❌ See All not clicked")
        return false
    }

    fun randomScrollThenGoTopAndSearch() {
        val metrics = resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val downStartY = metrics.heightPixels * 0.75f
        val downEndY = metrics.heightPixels * 0.25f

        val totalScrolls = (1..5).random()
        var currentScroll = 0

        Log.e("A11Y", "🎲 Random scroll count = $totalScrolls")

        fun swipeDown() {
            val path = Path().apply {
                moveTo(centerX, downStartY)
                lineTo(centerX, downEndY)
            }

            dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, (450..700).random().toLong()
                    )
                ).build(), null, null
            )

            currentScroll++
            Log.e("A11Y", "⬇️ Scroll down $currentScroll")
        }

        fun performScrollDown() {
            if (currentScroll < totalScrolls) {
                swipeDown()
                handler.postDelayed(
                    { performScrollDown() }, (1200..2200).random().toLong()
                )
            } else {
                handler.postDelayed({
                    scrollUpSameTimes(totalScrolls)
                }, (1000..1600).random().toLong())
            }
        }

        performScrollDown()
    }

    fun scrollUpSameTimes(times: Int) {
        val metrics = resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val upStartY = metrics.heightPixels * 0.25f
        val upEndY = metrics.heightPixels * 0.75f

        var count = 0

        fun swipeUp() {
            val path = Path().apply {
                moveTo(centerX, upStartY)
                lineTo(centerX, upEndY)
            }

            dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, (500..750).random().toLong()
                    )
                ).build(), null, null
            )

            count++
            Log.e("A11Y", "⬆️ Scroll up $count")
        }

        fun performScrollUp() {
            if (count < times) {
                swipeUp()
                handler.postDelayed(
                    { performScrollUp() }, (1200..2000).random().toLong()
                )
            } else {
                handler.postDelayed({
                    clickSearchIcon()
                }, (800..1400).random().toLong())
            }
        }

        performScrollUp()
    }

    fun clickSearchIcon(): Boolean {
        val root = rootInActiveWindow ?: return false

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/iv_search_image"
        )

        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                val clickable = findClickableParent(node)
                if (clickable != null) {
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.e("A11Y", "🔍 Clicked search icon")
                    handler.postDelayed({ typeRandomly() }, 1000)
                    return true
                }
            }
        }

        Log.e("A11Y", "❌ Search icon not found")
        return false
    }

    private fun typeRandomly() {
        val root = rootInActiveWindow ?: return

        val edit = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (edit == null) {
            Log.e(TAG, "❌ Could not find search EditText")
            return
        }

        // Pick random keyword
        val randomKeyword = productKeywords.random()

        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, randomKeyword
            )
        }

        edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        Log.e(TAG, "⌨️ Typed random keyword: $randomKeyword")

        handler.postDelayed({
            clickOnSearchHotDealResult()
        }, (2000..3500).random().toLong())
    }

    fun clickOnSearchHotDealResult() {
        val root = rootInActiveWindow ?: return
        val rv = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/search_results"
        ).firstOrNull()
        if (rv == null) {
            Log.e("A11Y", "❌ RecyclerView not found")
            return
        }
        if (rv.childCount == 0) {
            Log.e("A11Y", "❌ No suggestions")
            return
        }
        val item = rv.getChild(0)
        val rect = Rect()
        item.getBoundsInScreen(rect)
        Log.e("A11Y", "✔ Clicking suggestion @ $rect")
        item.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        handler.postDelayed({
            clickRandomProductAndBack()
        }, 3000)
    }

    fun clickRandomProductAndBack() {
        val root = rootInActiveWindow ?: return

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/open_item_v3"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No product items found")
            return
        }

        // Pick random product
        val product = nodes.random()

        val rect = Rect()
        product.getBoundsInScreen(rect)
        Log.e("A11Y", "🟢 Clicking product at bounds: $rect")

        val clickable = findClickableParent(product) ?: product

        if (clickable.isClickable) {
            clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked random product")

            handler.postDelayed({
                pressBackMultipleTimes(5)
            }, (2500..3500).random().toLong())
        } else {
            Log.e("A11Y", "❌ Product not clickable")
        }
    }

    fun pressBackMultipleTimes(times: Int) {
        var count = 0

        fun pressNext() {
            if (count < times) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                count++
                Log.e("A11Y", "🔙 Back pressed $count")

                handler.postDelayed(
                    { pressNext() }, (800..1400).random().toLong()
                )
            }
        }
        pressNext()
        if (count == times) {
            isRandomFlowDone = true
        }
    }

    private fun hotDealProductFlow(root1: AccessibilityNodeInfo) {
        val xyz = clickSeeAllDirect()
        Log.e(TAG, "hotDealProductFlow: $xyz")
    }

    fun clickSeeAllDirect(): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is NULL")
            return false
        }

        val nodes = root.findAccessibilityNodeInfosByText("See All")
        Log.e("A11Y", "🔍 Found ${nodes.size} 'See All' nodes")

        nodes.forEach { node ->
            val clickable = if (node.isClickable) node else findClickableParent(node)

            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Clicked See All (direct)")
                isHotDealFlow = true
                handler.postDelayed({ randomScrollThenClick() }, 1500)
                return true
            }
        }

        Log.e("A11Y", "❌ See All not clicked")
        return false
    }

    fun randomScrollThenClick() {
        val metrics = resources.displayMetrics

        val startX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.75f
        val endY = metrics.heightPixels * 0.25f

        val totalScrolls = (1..5).random()
        var currentScroll = 0

        Log.e("A11Y", "🎲 Will scroll $totalScrolls times")

        fun swipeOnce() {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }

            dispatchGesture(
                GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, (450..700).random().toLong()
                    )
                ).build(), null, null
            )

            currentScroll++
            Log.e("A11Y", "⬇️ Gesture scroll $currentScroll")
        }

        fun performNext() {
            if (currentScroll < totalScrolls) {
                swipeOnce()

                handler.postDelayed(
                    { performNext() }, (1200..2200).random().toLong()
                )
            } else {
                handler.postDelayed({
                    clickRandomHotDealItem()
                    Log.e("A11Y", "✅ Click after random scrolls")
                }, (800..1500).random().toLong())
            }
        }

        performNext()
    }

    fun clickRandomHotDealItem(): Boolean {
        val root = rootInActiveWindow ?: return false

        val productNodes = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            // Collect clickable nodes that look like product cards
            if (node.isClickable && node.className == "android.view.ViewGroup" && node.childCount > 2 // product cards usually have image + text + price
            ) {
                productNodes.add(node)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)

        if (productNodes.isEmpty()) {
            Log.e("A11Y", "❌ No product items found")
            return false
        }

        // Pick random product
        val randomNode = productNodes.random()

        randomNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.e("A11Y", "✅ Clicked random Hot Deal item")

        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK)
                isRandomFlowDone = true
            }, 1500)
        }, 2000)

        return true
    }

//    fun callRandomFlowRandomly(root: AccessibilityNodeInfo) {
//        val shouldCall = Random.nextInt(0, 2) == 0  // 33% chance
//
//        if (!shouldCall) {
//            isRandomFunCalled = true
//            isRandomFlowDone = true
//            Log.e("A11Y_FLOW", "⏭ Skipped calling any flow")
//            return
//        }
//
//        val flows = listOf(
//            "UserProfile" to { userProfileFlow() },
//            "CategoryPage" to { categoryPageFlow(root) },
//            "UserAddress" to { userAddressPageFlow() },
//            "hotDealFlow1" to { hotDealProductFlow(root) },
//            "hotDealFlow2" to { hotDealProductSearchFlow(root) },
//            "CategoryFlow1" to { clickRandomKeywordOnScreen() })
//            "CategoryFlow2" to { callCategoryFlow2() })
//            "CategoryFlow3" to { callCategoryFlow3() })
//            "ShopListFlow1" to { callShopListFlow1()() })
//            "WishList" to { callWishListFlow() })
//
//        val selected = flows.random()
//        Log.e("A11Y_FLOW", "🎯 Randomly selected flow: ${selected.first}")
//        isRandomFunCalled = true
//        selected.second.invoke()
//    }

    fun callRandomFlowRandomly(root: AccessibilityNodeInfo) {
        randomFlowCounter++

//        val shouldCall = randomFlowCounter >= Random.nextInt(2, 5)
//        if (!shouldCall) {
//            Log.e("A11Y_FLOW", "⏭ Waiting to trigger random flow")
//            isRandomFunCalled = true
//            isRandomFlowDone = true
//            return
//        }

        val chance = Random.nextInt(100)
        if (chance > 50) {   // ~50% probability
            Log.d("A11Y_FLOW", "⏭ Skipped random flow (chance=$chance)")
            isRandomFunCalled = true
            isRandomFlowDone = true
            return
        }


        randomFlowCounter = 0 // reset

        val flows = listOf(
            "UserProfile" to { userProfileFlow() },
            "CategoryPage" to { categoryPageFlow(root) },
            "UserAddress" to { userAddressPageFlow() },
            "hotDealFlow1" to { hotDealProductFlow(root) },
            "hotDealFlow2" to { hotDealProductSearchFlow(root) },
            "CategoryFlow1" to { clickRandomKeywordOnScreen() },
            "CategoryFlow2" to { callCategoryFlow2() },
            "CategoryFlow3" to { callCategoryFlow3(root) },
            "ShopListFlow1" to { callShopListFlow1() },
            "WishList" to { callWishListFlow() })

        val selected = flows.random()
//        isRandomFlowDone = true
        Log.e("A11Y_FLOW", "🎯 Triggered flow after cooldown: ${selected.first}")
        selected.second.invoke()
    }

    fun dumpTree(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return

        Log.e(
            "A11Y_DUMP",
            "${" ".repeat(depth * 2)}- ${node.className} | text=${node.text} | desc=${node.contentDescription} | id=${node.viewIdResourceName} | clickable=${node.isClickable}"
        )

        for (i in 0 until node.childCount) {
            dumpTree(node.getChild(i), depth + 1)
        }
    }

    private fun userAddressPageFlow() {
        if (!isUserAddressRedirect) {
            val userAddress = clickAddressSelector()
            Log.e(TAG, "onAccessibilityEvent: $userAddress")
        }
    }

    private fun categoryPageFlow(root: AccessibilityNodeInfo) {
        if (!isCategoryRedirected) {
            clickCategoriesButton(root)
        }
    }

    private fun userProfileFlow() {
        if (!isUserProfileRedirect) {
            val userProfile = clickUserAccountIcon()
            Log.e(TAG, "onAccessibilityEvent: $userProfile")
        }
    }

    fun clickAddressSelector(): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is NULL")
            return false
        }

        val id = "in.swiggy.android.instamart:id/address_selector_area"
        val nodes = root.findAccessibilityNodeInfosByViewId(id)

        Log.d("A11Y", "🔍 Found ${nodes.size} nodes for address_selector_area")

        nodes.forEach { node ->
            val clickableNode = if (node.isClickable) node else findAddressParent(node)

            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Address selector clicked")
                isUserAddressRedirect = true
                handler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    isRandomFlowDone = true
                }, 1500)
                return true
            }
        }

        Log.e("A11Y", "❌ Address selector NOT clicked")
        return false
    }

    fun findAddressParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        var depth = 0

        while (current != null && depth < 6) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    fun clickUserAccountIcon(): Boolean {
        val root = rootInActiveWindow ?: return false

        fun traverse(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false

            if (node.className == "android.widget.ImageView" && node.isClickable && node.contentDescription?.toString()
                    ?.equals("Show account", ignoreCase = true) == true
            ) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                isUserProfileRedirect = true
                Log.e("A11Y", "✅ User account icon clicked")
                handler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    isRandomFlowDone = true
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

    fun clearAllValues() {
        city = ""
        pincode = ""
        darkStoreName = ""
        productName = ""
        productWeight = ""
        productMRP = "0"
        productSellingPrice = "0"
        productDiscountPercentage = "0"
        productInventory = "0"
        productBrand = ""
        darkStoreId = ""
        darkStoreLocality = ""
        darkStorePlusCode = ""
        merchantId = ""
        productId = ""
        productUrl = ""
        level1Category = ""
        level2Category = ""
        level3Category = ""
        etaIdentifier = ""
    }

    fun clickCategoriesButton(root1: AccessibilityNodeInfo) {

        // 1️⃣ Get all item_container nodes
        val containers = root1.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/item_container"
        )

        if (containers.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No item_container found")
            return
        }

        // 2️⃣ Loop through all containers to find one with text "Categories"
        for (container in containers) {

            // Search inside container for TextView "Categories"
            val children = container.findAccessibilityNodeInfosByText("Categories")

            if (!children.isNullOrEmpty()) {
                Log.e("A11Y", "✅ Found container for Categories: $container")
                isCategoryRedirected = true
                container.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                handler.postDelayed({
//                    clickFirstCategoryAndGetHeader(root1)
//                }, 3000)
                handler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    isRandomFlowDone = true
                }, 1500)
                return
            }
        }

        Log.e("A11Y", "❌ No container had 'Categories' text")
    }

    fun clickFirstCategoryAndGetHeader(root: AccessibilityNodeInfo) {

        // 1️⃣ Find all category blocks (food_listing → header + recycler)
        val categoryBlocks = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/food_listing"
        )

        if (categoryBlocks.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No food_listing blocks found")
            return
        }

        // 2️⃣ Work on the first block only
        val firstBlock = categoryBlocks[0]

        // ----------- Extract Header Text -----------
        val headers = firstBlock.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/include_card_widget_header"
        )

        var headerTitle = ""

        if (!headers.isNullOrEmpty()) {
            val headerNode = headers[0]

            // Extract first TextView inside header
            val textNodes = headerNode.findAccessibilityNodeInfosByText("")
            for (n in textNodes) {
                if (!n.text.isNullOrBlank()) {
                    headerTitle = n.text.toString()
                    break
                }
            }

            Log.e("A11Y", "📌 Header Title: $headerTitle")
        } else {
            Log.e("A11Y", "❌ Header not found")
        }


        // ----------- Click First Item of Recycler -----------
        val recyclerItems = firstBlock.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/include_card_widget_recycler"
        )

        if (recyclerItems.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No include_card_widget_recycler found")
            return
        }

        val recycler = recyclerItems[0]

        // Get first child inside recycler (index 0)
        var firstItem: AccessibilityNodeInfo? = null

        for (i in 0 until recycler.childCount) {
            val child = recycler.getChild(i)
            if (child != null && child.isVisibleToUser) {
                firstItem = child
                break
            }
        }

        if (firstItem == null) {
            Log.e("A11Y", "❌ No visible item found inside recycler")
            return
        }

        Log.e("A11Y", "🟦 Clicking first item: $firstItem")

        firstItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Done!
    }

//    fun clickCategoriesButton(): Boolean {
//        val root = rootInActiveWindow ?: return false
//
//        // Get all nodes with text "Categories"
//        val nodes = root.findAccessibilityNodeInfosByText("Categories")
//        if (!nodes.isNullOrEmpty()) {
//            val textNode = nodes[0]
//            Log.e(TAG, "Found Categories Text Node: $textNode")
//
//            // Walk upward until we find clickable parent
//            var parent = textNode
//            while (parent != null) {
//                if (parent.isClickable) {
//                    Log.e(TAG, "Clicking parent node: $parent")
//                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                }
//                parent = parent.parent
//            }
//
//            Log.e(TAG, "Found text but no clickable parent")
//            return false
//        }
//
//        Log.e(TAG, "❌ Categories bottom nav not found")
//        return false
//    }

    fun extractAddressDetails() {
        val desc = getAddressDesc() ?: return
        parseAddress(desc)
    }

    fun getAddressDesc(): String? {
        val root = rootInActiveWindow ?: return null

        val id = "in.swiggy.android.instamart:id/address_selector_area"
        val nodes = root.findAccessibilityNodeInfosByViewId(id)

        if (nodes.isNullOrEmpty()) {
            Log.e(TAG, "❌ address_selector_area not found")
            return null
        }

        val node = nodes.first()
        val desc = node.contentDescription?.toString()?.trim()

        Log.e(TAG, "📌 Address Description: $desc")

        return desc
    }

    fun parseAddress(desc: String) {
        val clean = desc.replace("Selected address is", "", ignoreCase = true).replace("Delivering in", "", ignoreCase = true).trim()
        Log.e(TAG, "📍 Clean Address: $clean")
        val parts = clean.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        Log.e(TAG, "🔹 Parts: $parts")

        // Detect 6-digit pincode
        val pinRegex = Regex("\\b\\d{6}\\b")
        val pinMatch = pinRegex.find(clean)
        if (pinMatch != null) {
            pincode = pinMatch.value
            Log.e(TAG, "🏷 Pincode: $pincode")
        } else {
            Log.e(TAG, "⚠ No pincode found")
        }

        val indianStatesAndUTs = arrayListOf(
            // States
            "Andhra Pradesh",
            "Arunachal Pradesh",
            "Assam",
            "Bihar",
            "Chhattisgarh",
            "Goa",
            "Gujarat",
            "Haryana",
            "Himachal Pradesh",
            "Jharkhand",
            "Karnataka",
            "Kerala",
            "Madhya Pradesh",
            "Maharashtra",
            "Manipur",
            "Meghalaya",
            "Mizoram",
            "Nagaland",
            "Odisha",
            "Punjab",
            "Rajasthan",
            "Sikkim",
            "Tamil Nadu",
            "Telangana",
            "Tripura",
            "Uttar Pradesh",
            "Uttarakhand",
            "West Bengal",

            // Union Territories
            "Andaman and Nicobar Islands",
            "Chandigarh",
            "Dadra and Nagar Haveli and Daman and Diu",
            "Delhi",
            "Jammu and Kashmir",
            "Ladakh",
            "Lakshadweep",
            "Puducherry"
        )

        var detectedCity = ""

        for (i in parts.indices) {
            if (indianStatesAndUTs.any { parts[i].contains(it, ignoreCase = true) }) {
                if (i > 0) {
                    detectedCity = parts[i - 1]
                    break
                }
            }
        }

        city = detectedCity
        Log.e(TAG, "🏙 City: $city")

        if (pincode.isNotEmpty() && city.isNotEmpty()) {
            HectorScraper.isAddressStored = true
        }
    }
    // ---------------------------------------------------------
    // LAUNCH APP
    // ---------------------------------------------------------

    private fun launchInstamart() {
        Log.e(TAG, "launchInstamart instamart: ${isInstamartAvailable(applicationContext)}")
        openInstamartDirect(applicationContext)
//        val intent = packageManager.getLaunchIntentForPackage(INSTAMART_PACKAGE)
//        if (intent != null) {
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            startActivity(intent)
//            Log.e(TAG, "📲 Instamart Launched")
//
//            waitForAppToLoad = true
//            handler.postDelayed({ waitForAppToLoad = false }, 4500)
//        } else {
//            Log.e(TAG, "❌ Instamart not installed")
//        }
    }

    fun openInstamartDirect(context: Context) {
        "com.swiggy.android.instamart"

        try {
            val intent = applicationContext.packageManager.getLaunchIntentForPackage(INSTAMART_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                waitForAppToLoad = true
                handler.postDelayed({ waitForAppToLoad = false }, 4500)
                Log.e("A11Y", "🚀 Instamart opened directly")
            } else {
                Log.e("A11Y", "❌ Instamart not installed")
                openInstamartViaHomeFallback()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("A11Y", "❌ Failed to open Instamart directly: ${e.message}")
            openInstamartViaHomeFallback()
        }
    }

    private fun goToHomeScreen() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        Log.e("A11Y", "🏠 Went to Home screen")
    }

    private fun openAppDrawer() {
        val metrics = resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.85f
        val endY = metrics.heightPixels * 0.25f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        dispatchGesture(
            GestureDescription.Builder().addStroke(
                GestureDescription.StrokeDescription(path, 0, 600)
            ).build(), null, null
        )

        Log.e("A11Y", "⬆️ Swiped up to open App Drawer")
    }

    private fun findAndClickInstamart(): Boolean {
        val root = rootInActiveWindow ?: return false

        val appNames = listOf("Instamart", "Swiggy Instamart")

        for (appName in appNames) {
            val nodes = root.findAccessibilityNodeInfosByText(appName)

            for (node in nodes) {

                // ✅ Ensure exact match (launcher safety)
                if (node.text?.toString()?.equals(appName, ignoreCase = true) != true) {
                    continue
                }

                // ✅ Find clickable app icon container
                return findAndClickInstamartIcon()
            }
        }

        return false
    }

    fun findAndClickInstamartIcon(): Boolean {
        val root = rootInActiveWindow ?: return false

        fun traverse(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null

            // Match content-desc = Instamart
            if (node.contentDescription?.toString()?.equals("Instamart", ignoreCase = true) == true) {
                return node
            }

            for (i in 0 until node.childCount) {
                val found = traverse(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        val node = traverse(root)

        if (node != null) {
            val clicked = clickNodeOrParent(node)
            Log.e("A11Y", "🚀 Instamart icon clicked = $clicked")
            return clicked
        }

        Log.e("A11Y", "❌ Instamart icon not found")
        return false
    }

    private fun scrollAppDrawerDown() {
        val metrics = resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.75f
        val endY = metrics.heightPixels * 0.25f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        dispatchGesture(
            GestureDescription.Builder().addStroke(
                GestureDescription.StrokeDescription(path, 0, 600)
            ).build(), null, null
        )

        Log.e("A11Y", "⬇️ Scrolled App Drawer")
    }

    private fun findInstamartWithScroll() {
        if (findAndClickInstamart()) {
            Log.e("A11Y", "✅ Instamart opened from App Drawer")
            appDrawerScrollCount = 0
            return
        }

        if (appDrawerScrollCount >= MAX_APP_DRAWER_SCROLL) {
            Log.e("A11Y", "❌ Instamart not found after scrolling")
            appDrawerScrollCount = 0
            return
        }

        appDrawerScrollCount++
        scrollAppDrawerDown()

        handler.postDelayed({
            findInstamartWithScroll()
        }, 1500)
    }

    private fun openInstamartViaHomeFallback() {
        performGlobalAction(GLOBAL_ACTION_HOME)

        handler.postDelayed({
            openAppDrawer()

            handler.postDelayed({
                findInstamartWithScroll()
            }, 1800)

        }, 1200)
    }

    fun isInstamartAvailable(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                "in.swiggy.android.instamart", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSwiggyApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("in.swiggy.android")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
    // ---------------------------------------------------------
    // SEARCH BAR → TYPE → CLICK SUGGESTION FLOW
    // ---------------------------------------------------------

    private fun clickSearchBar(root: AccessibilityNodeInfo) {
        Log.e(TAG, "🔍 Looking for search bar...")

        val nodes = root.findAccessibilityNodeInfosByText("Search")
        if (nodes.isNullOrEmpty()) {
            Log.e(TAG, "❌ Search bar not found")
            return
        }

        for (node in nodes) {
            var cur = node
            while (!cur.isClickable && cur.parent != null) cur = cur.parent

            if (cur.isClickable) {
                cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "✔ Search bar clicked")
                searchClicked = true
                return
            }
        }

        Log.e(TAG, "⚠ Search found but no clickable parent")
    }

    private fun typeChocolate() {
        val root = rootInActiveWindow ?: return

        val edit = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (edit == null) {
            Log.e(TAG, "❌ Could not find EditText")
            return
        }

        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, HectorScraper.keyword
        )
        edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        Log.e(TAG, "🍫 Typing done")

        handler.postDelayed({
            clickOnSearchResult()
        }, 3000)
    }

    fun clickOnSearchResult() {
        val root = rootInActiveWindow ?: return
        val rv = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/search_results"
        ).firstOrNull()
        if (rv == null) {
            Log.e("A11Y", "❌ RecyclerView not found")
            return
        }
        if (rv.childCount == 0) {
            Log.e("A11Y", "❌ No suggestions")
            return
        }
        val item = rv.getChild(0)
        val rect = Rect()
        item.getBoundsInScreen(rect)
        Log.e("A11Y", "✔ Clicking suggestion @ $rect")
        item.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        handler.postDelayed({
            clickFirstProduct()
        }, 3000)
    }

    fun clickFirstProduct() {
        val root = rootInActiveWindow ?: return

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/open_item_v3"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ No open_item_v3 found on screen")
            return
        }

        val first = nodes[0]

        // LOG bounds
        val rect = Rect()
        first.getBoundsInScreen(rect)
        Log.e("A11Y", "🟢 First product bounds: $rect")

        // Try clicking item
        if (first.isClickable) {
            first.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✔ Clicked product directly")
            handler.postDelayed({
                clickOnShare()
            }, 4000)

//            logProductDetails()
            return
        }
        Log.e("A11Y", "❌ Could not click product or parent")
    }

    fun clickOnShare() {
        val root = rootInActiveWindow ?: return

        val shareNodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/share_icon"
        )

        if (shareNodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ share_icon not found")
            return
        }

        val shareNode = shareNodes[0]

        if (shareNode.isClickable) {
            shareNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d("A11Y", "Clicked share_icon directly")
            handler.postDelayed({
                clickOnMore()
            }, 1500)
        } else {
            var parent = shareNode.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("A11Y", "Clicked parent of share_icon")
                    handler.postDelayed({
                        clickOnMore()
                    }, 1500)
                    return
                }
                parent = parent.parent
            }
            Log.e("A11Y", "❌ No clickable parent found for share_icon")
        }
    }

    private fun clickOnMore() {
        val root = rootInActiveWindow ?: return
        if (root == null) return

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/label"
        )

        val target = nodes.firstOrNull {
            it.text?.toString()?.equals("More", ignoreCase = true) == true
        }

        if (!nodes.isNullOrEmpty()) {
            var clickable = target
            while (clickable != null && !clickable.isClickable) {
                clickable = clickable.parent
            }

            clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ More clicked")
            handler.postDelayed({
                findAndClickHectorRealme()
//                findAndClickHectorScraper()
            }, 2500)
        } else {
            Log.e("A11Y", "❌ More not found")
            handler.postDelayed({
                findAndClickHectorRealme()
            }, 2500)
        }
    }

    fun findAndClickHectorRealme(): Boolean {
        val root = rootInActiveWindow ?: return false

        val labels = root.findAccessibilityNodeInfosByViewId("android:id/text1")

        Log.e(TAG, "findAndClickHectorRealme: ${labels.size}")

        labels.forEachIndexed { index, label ->
            val text = label.text?.toString()

            Log.e("A11Y_CHECK", "[$index] Found label = $text")

            if (text.equals("Hector Scraper", ignoreCase = true)) {

                Log.e("A11Y_CHECK", "✅ Hector Scraper label found")

                val clickableParent = findClickableParent(label)

                if (clickableParent != null) {
                    Log.e(
                        "A11Y_CLICK", "👉 Clicking parent: ${clickableParent.viewIdResourceName}"
                    )

                    clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    handler.postDelayed({
                        if (HectorScraper.storeid.isEmpty()) {
                            clickOnShare()
                        } else {
                            logProductDetails()
                        }
                    }, 2000)
                    return true
                } else {
                    Log.e("A11Y_CLICK", "❌ No clickable parent found")
                }
            }
        }
        return false
    }

    fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        var depth = 0

        while (current != null && depth < 6) { // avoid infinite loops
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isScrollable) return node

        for (i in 0 until node.childCount) {
            val scrollable = findScrollableNode(node.getChild(i))
            if (scrollable != null) return scrollable
        }
        return null
    }

    fun logScrollableNodes(root: AccessibilityNodeInfo?) {
        if (root == null) return

        val scrollables = mutableListOf<AccessibilityNodeInfo>()
        findScrollableNodes(root, scrollables)

        Log.e("A11Y_SCROLL", "Scrollable nodes count = ${scrollables.size}")

        scrollables.forEachIndexed { i, node ->
            Log.e(
                "A11Y_SCROLL", """
            [$i]
            class=${node.className}
            scrollable=${node.isScrollable}
            actions=${node.actionList.map { it.id }}
            """.trimIndent()
            )
        }
    }

    fun findAndClickHectorWithLogs(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false

        val nodes = root.findAccessibilityNodeInfosByViewId("android:id/text1")

        nodes.forEachIndexed { index, node ->
            val text = node.text?.toString()

            Log.e("A11Y_CHECK", "[$index] Checking item: '$text'")

            if (text.equals("Hector Scraper", ignoreCase = true)) {
                Log.e("A11Y_CHECK", "✅ MATCH FOUND → Clicking Hector Scraper")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        Log.e("A11Y_CHECK", "❌ Hector Scraper not found in current view")
        return false
    }

    fun findScrollableNodes(
        node: AccessibilityNodeInfo?, result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        if (node.isScrollable) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            findScrollableNodes(node.getChild(i), result)
        }
    }

    fun logAllShareItems(root: AccessibilityNodeInfo?) {
        if (root == null) return

        val nodes = root.findAccessibilityNodeInfosByViewId("android:id/text1")

        Log.e("A11Y_DEBUG", "===== SHARE ITEMS COUNT = ${nodes.size} =====")

        nodes.forEachIndexed { index, node ->
            Log.e(
                "A11Y_DEBUG", """
            [$index]
            class=${node.className}
            text=${node.text}
            id=${node.viewIdResourceName}
            clickable=${node.isClickable}
            parent=${node.parent?.className}
            """.trimIndent()
            )
        }
    }

    fun clickShareIcon1(root: AccessibilityNodeInfo?) {
        if (root == null) return

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/back_icon"
        )

        if (!nodes.isNullOrEmpty()) {
            val node = nodes.first()

            // Sometimes ImageView itself isn't clickable
            var clickableNode = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }

            clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Share icon clicked")
        } else {
            Log.e("A11Y", "❌ Share icon not found")
        }
    }

    fun clickShareButton1(root: AccessibilityNodeInfo) {
        val nodes = root.findAccessibilityNodeInfosByText("Share")
        nodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun clickSharedIcon(root: AccessibilityNodeInfo?) {
        if (root == null) {
            Log.e("A11Y", "❌ Root is null")
            return
        }

        val targetId = "in.swiggy.android.instamart:id/share_icon"

        val node = findNodeByViewIdRecursive(root, targetId)
        if (node == null) {
            Log.e("A11Y", "❌ Share icon NOT FOUND")
            return
        }

        val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (clicked) {
            Log.e("A11Y", "✅ Share icon CLICKED")
        } else {
            Log.e("A11Y", "⚠ Click FAILED on share icon")
        }
    }

    fun findNodeByViewIdRecursive(root: AccessibilityNodeInfo?, id: String): AccessibilityNodeInfo? {
        if (root == null) return null

        if (root.viewIdResourceName == id) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            val result = findNodeByViewIdRecursive(child, id)
            if (result != null) return result
        }

        return null
    }

    fun printFullViewTree(root: AccessibilityNodeInfo?, indent: String = "") {
        if (root == null) return

        val id = root.viewIdResourceName ?: "null"
        val text = root.text ?: "null"
        val desc = root.contentDescription ?: "null"

        Log.e(
            "A11Y_VIEW",
            "$indent• [${root.className}]\n" + "$indent   id: $id\n" + "$indent   text: $text\n" + "$indent   desc: $desc\n" + "$indent   clickable=${root.isClickable}, enabled=${root.isEnabled}\n"
//                    "$indent   bounds=${root.boundsInScreen}"
        )

        for (i in 0 until root.childCount) {
            printFullViewTree(root.getChild(i), "$indent   ")
        }
    }

    fun dumpAllWindows(service: AccessibilityService) {
        val windows = service.windows
        if (windows.isEmpty()) {
            Log.e("A11Y_TREE", "No windows")
            return
        }

        windows.forEachIndexed { idx, window ->
            val root = window.root ?: return@forEachIndexed

            Log.e("A11Y_TREE", "===== WINDOW $idx (${window}) =====")
            val builder = StringBuilder()
            dumpNodeRecursive(root, builder, 0)
            Log.e("A11Y_TREE", builder.toString())
        }
    }

    fun dumpFullTree(root: AccessibilityNodeInfo?) {
        if (root == null) {
            Log.e("A11Y_TREE", "Root is null")
            return
        }
        val builder = StringBuilder()
        dumpNodeRecursive(root, builder, 0)
        Log.e("A11Y_TREE", "\n$builder")
    }

    private fun dumpNodeRecursive(
        node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int
    ) {
        if (node == null) return

        val indent = "  ".repeat(depth)

        val id = node.viewIdResourceName ?: "null"
        val text = node.text ?: "null"
        val desc = node.contentDescription ?: "null"
        val className = node.className ?: "null"
        val clickable = node.isClickable
        val focusable = node.isFocusable
        val enabled = node.isEnabled

        val rect = Rect()
        node.getBoundsInScreen(rect)

        out.append(
            "$indent• [$className]\n" + "$indent   id: $id\n" + "$indent   text: $text\n" + "$indent   desc: $desc\n" + "$indent   clickable: $clickable, focusable: $focusable, enabled: $enabled\n" + "$indent   bounds: $rect\n\n"
        )

        for (i in 0 until node.childCount) {
            dumpNodeRecursive(node.getChild(i), out, depth + 1)
        }
    }

    fun clickShareIcon(root: AccessibilityNodeInfo) {

        // 1️⃣ Try direct resource-id
        val byId = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/share_icon"
        )
        if (!byId.isNullOrEmpty()) {
            byId[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked share_icon by ID")
            return
        }

        // 2️⃣ Try content description
        val byDesc = findByContentDesc(root, "Share")
        if (byDesc != null) {
            byDesc.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Clicked Share icon by ContentDescription")
            return
        }

        // 3️⃣ Try parent top_view → then find ImageView inside it
        val topView = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/top_view"
        )

        if (!topView.isNullOrEmpty()) {
            val found = findImageInside(topView[0])
            if (found != null) {
                found.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e("A11Y", "✅ Clicked share_icon from inside top_view")
                return
            }
        }

        Log.e("A11Y", "❌ Share icon not found anywhere")
    }

    private fun findByContentDesc(node: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.contentDescription?.toString()?.contains(desc, true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = findByContentDesc(node.getChild(i), desc)
            if (result != null) return result
        }
        return null
    }

    private fun findImageInside(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.className == "android.widget.ImageView") {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = findImageInside(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    fun findNodeByResourceId(node: AccessibilityNodeInfo?, id: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.viewIdResourceName == id) return node

        for (i in 0 until node.childCount) {
            val child = findNodeByResourceId(node.getChild(i), id)
            if (child != null) return child
        }
        return null
    }

    fun clickInstamartShareIcon(root: AccessibilityNodeInfo) {
        val targetId = "in.swiggy.android.instamart:id/share_icon"

        // 🔥 Scan ALL windows (Instamart often places UI in window[1] or window[2])
//        service.windows.forEach { window ->
//            val root = window.root ?: return@forEach

        val node = findNodeByResourceId(root, targetId)
        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.e("A11Y", "✅ Share icon CLICKED")
            return
        }
//        }

        Log.e("A11Y", "❌ Share icon NOT FOUND IN ANY WINDOW")
    }

    fun clickShareButton(root: AccessibilityNodeInfo) {

        // 1️⃣ Try by ViewId (primary method)
        val byId = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/share_icon"
        )
        if (!byId.isNullOrEmpty()) {
            Log.e("A11Y", "✅ Share Icon found by ID")
            byId[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }

        // 2️⃣ Try by contentDescription = "Share"
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            if (node.contentDescription?.toString()?.trim()?.lowercase() == "share") {
                Log.e("A11Y", "✅ Share icon found via contentDescription")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        // 3️⃣ Try by class + location (Instamart share icon is top-right)
        val queue2 = ArrayDeque<AccessibilityNodeInfo>()
        queue2.add(root)

        while (queue2.isNotEmpty()) {
            val node = queue2.removeFirst()

            if (node.className == "android.widget.ImageView") {
                val rect = Rect()
                node.getBoundsInScreen(rect)

                // top-right 20% of screen
                if (rect.top < 400 && rect.right > 800) {
                    Log.e("A11Y", "✅ Share icon found via screen position")
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue2.add(it) }
            }
        }

        // If still not clicked
        Log.e("A11Y", "❌ Share icon not found")

        // 1️⃣ Try by Instamart Share Button ID
//        val shareNodes = root.findAccessibilityNodeInfosByViewId(
//            "in.swiggy.android.instamart:id/share_icon"
//        )
//
//        if (!shareNodes.isNullOrEmpty()) {
//            Log.e("A11Y", "✅ Share button found by ID, clicking…")
//            shareNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
//            return
//        }
//
//        // 2️⃣ Try by content description
//        val descNodes = findNodesByContentDesc(root, "Share")
//        if (descNodes.isNotEmpty()) {
//            Log.e("A11Y", "✅ Share button found by contentDescription, clicking…")
//            descNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
//            return
//        }

        Log.e("A11Y", "❌ Share button not found")

        handler.postDelayed({
            val url = extractSharedLinkFromShareSheet(root)
            if (url != null) {
                Log.e("A11Y", "📦 Product Link = $url")
            } else {
                Log.e("A11Y", "📦 Product Link = not found")
            }
        }, 1000)
    }

    fun findNodesByContentDesc(node: AccessibilityNodeInfo?, desc: String): List<AccessibilityNodeInfo> {
        if (node == null) return emptyList()

        val list = mutableListOf<AccessibilityNodeInfo>()

        if (node.contentDescription?.toString()?.contains(desc, true) == true) {
            list.add(node)
        }

        for (i in 0 until node.childCount) {
            list.addAll(findNodesByContentDesc(node.getChild(i), desc))
        }

        return list
    }

    fun extractSharedLinkFromShareSheet(root: AccessibilityNodeInfo): String? {
        val urls = mutableListOf<String>()
        collectTextWithUrls(root, urls)
        return urls.firstOrNull()
    }

    fun collectTextWithUrls(node: AccessibilityNodeInfo?, result: MutableList<String>) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        if (text != null && text.startsWith("http")) {
            result.add(text)
        }

        for (i in 0 until node.childCount) {
            collectTextWithUrls(node.getChild(i), result)
        }
    }

    fun logProductDetails() {
        handler.postDelayed({
            val root = rootInActiveWindow ?: run {
                Log.e("A11Y", "❌ root is null after product click")
                return@postDelayed
            }

            Log.e(TAG, "logProductDetails URL: ${findUrl(root)}")
            captureProductDetails(root)

            Log.e("A11Y", "====== 📌 PRODUCT DETAILS END ======")
            val contentText = findSellerDetailsContent()
            Log.d("A11Y", "Result = $contentText")

        }, 1200) // WAIT 1.2 sec for page to load
    }

    fun findUrl(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        val text = node.text?.toString() ?: node.contentDescription?.toString()

        if (text != null && text.startsWith("http")) {
            return text
        }

        for (i in 0 until node.childCount) {
            val result = findUrl(node.getChild(i))
            if (result != null) return result
        }

        return null
    }


    fun captureProductDetails(root: AccessibilityNodeInfo) {

        productName = getText(root = root, "in.swiggy.android.instamart:id/title")
        productBrand = extractBrand(getText(root = root, "in.swiggy.android.instamart:id/action_name"))
//        productWeight = getText(root = root, "in.swiggy.android.instamart:id/quantity")
        productWeight = getText(root = root, "in.swiggy.android.instamart:id/quantityTextCrouton")
        productMRP = getText(root = root, "in.swiggy.android.instamart:id/actual_price")
        productSellingPrice = getText(root = root, "in.swiggy.android.instamart:id/final_price")
        productDiscountPercentage = getText(root = root, "in.swiggy.android.instamart:id/offer")
        val quantity = getText(root = root, "in.swiggy.android.instamart:id/quantity")
        val gContent = getText(root = root, "in.swiggy.android.instamart:id/content")
//        gAddButtonText = getText("in.swiggy.android.instamart:id/quantity_text_1")

        // Logs to verify
        Log.e("A11Y", "======= PRODUCT DETAILS STORED =======")
        Log.e("A11Y", "Product      : $productName")
        Log.e("A11Y", "Brand        : $productBrand")
        Log.e("A11Y", "Pack Size    : $productWeight")
        Log.e("A11Y", "MRP          : $productMRP")
        Log.e("A11Y", "Selling Price: $productSellingPrice")
        Log.e("A11Y", "Discount     : $productDiscountPercentage")
        Log.e("A11Y", "Quantity     : $quantity")
        Log.e("A11Y", "Content      : $gContent")
//        Log.e("A11Y", "Add Button   : $gAddButtonText")
    }

    fun getNodeText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        // 1️⃣ Try normal text
        node.text?.toString()?.let { if (it.isNotBlank()) return it }

        // 2️⃣ Try contentDescription
        node.contentDescription?.toString()?.let { if (it.isNotBlank()) return it }

        // 3️⃣ Try hint text (rare but possible)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.let { if (it.isNotBlank()) return it }
        }

        // 4️⃣ Try children text recursively
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val childText = getNodeText(child)
            if (!childText.isNullOrBlank()) return childText
        }

        return null
    }

//    fun findSellerDetailsContent(): String? {
//        val root = rootInActiveWindow ?: run {
//            Log.e("A11Y", "❌ rootInActiveWindow is NULL")
//            return null
//        }
//
//        Log.d("A11Y", "🔍 Starting search for Seller Details content")
//
//        for (attempt in 0..20) {
//            Log.d("A11Y", "🔁 Attempt $attempt to find Seller label")
//
//            // Find all content nodes
//            val labelNodes = root.findAccessibilityNodeInfosByViewId(
//                "in.swiggy.android.instamart:id/content"
//            )
//
//            Log.d("A11Y", "🔍 Found ${labelNodes?.size ?: 0} nodes with id 'content'")
//
//            // Find the node containing "Seller"
//            val targetLabel = labelNodes?.firstOrNull {
//                val txt = it.text?.toString()?.trim()
//                Log.d("A11Y", "   ➡️ Checking node text: $txt")
//                txt?.contains("Seller ", ignoreCase = true) == true
//            }
//
//            if (targetLabel != null) {
//                Log.d("A11Y", "📌 Match found: node contains 'Seller'")
//
//                // Extract next content
//                val content = extractSiblingContent(targetLabel)
//
//                if (content != null) {
//                    Log.d("A11Y", "✅ FINAL CONTENT FOUND: $content")
//                    val rawText = content.trimIndent()
//                    darkStoreName = extractField(rawText, "Address:")
//                    darkStoreId = extractField(rawText, "FSSAI Number:")
//                    merchantId = extractField(rawText, "FSSAI Number:")
//                    darkStoreLocality = extractLocality(rawText)
//                    Log.e(TAG, "findSellerDetailsContent: $darkStoreName  $darkStoreId  $merchantId  $darkStoreLocality")
//                    handler.postDelayed({ scrollToTop() }, 1000)
////                    scrollToTop()
//                    return content
//                } else {
//                    Log.e("A11Y", "❌ extractSiblingContent() could NOT find related content")
//                }
//            } else {
//                Log.d("A11Y", "❌ No label containing 'Seller' found in this scroll")
//            }
//
//            // scroll down and check again
//            val scrollNode = findScrollableNode(root)
//            if (scrollNode != null) {
//                Log.d("A11Y", "↪️ Scrolling down…")
//                scrollNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
//                Thread.sleep(300)
//            } else {
//                Log.e("A11Y", "❌ No scrollable node found — stopping search")
//                break
//            }
//        }
//
//        Log.e("A11Y", "❌ Seller Details content NOT FOUND after scrolling")
//        handler.postDelayed({ scrollToTop() }, 1000)
//        return null
//    }

    fun findSellerDetailsContent(): String? {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is NULL")
            return null
        }

        Log.d("A11Y", "🔍 Looking for 'Seller' content")

        var attempt = 0

        while (attempt < 25) {
            Log.d("A11Y", "🔁 Attempt $attempt")
            attempt++

            val contentNodes = root.findAccessibilityNodeInfosByViewId(
                "in.swiggy.android.instamart:id/content"
            )

            Log.d("A11Y", "🔍 Found ${contentNodes?.size ?: 0} content nodes")

            if (!contentNodes.isNullOrEmpty()) {
                for (node in contentNodes) {
                    val raw = node.text?.toString()?.trim()

                    if (!raw.isNullOrBlank()) {
                        Log.d("A11Y", "➡️ Node text:\n$raw")

                        // Exact match check
//                        if (raw.contains("Seller Name:", ignoreCase = true) || raw.startsWith("Seller", ignoreCase = true)) {
//                            Log.d("A11Y", "📌 Found Seller Node")
//
//                            // Extract values
//                            darkStoreName = extractField(raw, "Address:")
//                            darkStoreLocality = extractLocality(raw)
//
//                            Log.e(
//                                "A11Y", "🟢 Parsed → $darkStoreName | $darkStoreLocality"
//                            )
//
//                            handler.postDelayed({ scrollToTop() }, 800)
//                            return raw
//                        }

                        if (raw.contains("Seller Name:", ignoreCase = true) || raw.startsWith("Seller", ignoreCase = true)) {

                            val darkStore = extractDarkStoreName(raw)

                            Log.e("A11Y", "🏬 Dark Store: $darkStore")

                            darkStoreName = darkStore
                            val address = extractAddressBlock(raw)
                            val pincode = extractPincode(address)
                            val (locality, _) = extractLocalityAndCity(address)

                            Log.e("A11Y", "📍 Address: $address")
                            Log.e("A11Y", "🏙 Locality: $locality")
                            Log.e("A11Y", "🏷 Pincode: $pincode")

//                            darkStoreLocality = locality
                            darkStoreAddress = address
//                            darkStorePincode = pincode

                            handler.postDelayed({ scrollToTop() }, 800)
                            return raw
                        }
                    }
                }
            }

            // Scroll if not found
            val scrollNode = findScrollableNode(root)
            if (scrollNode != null) {
                Log.d("A11Y", "↪️ Scrolling…")
                scrollNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Thread.sleep(250)
            } else {
                Log.e("A11Y", "❌ No scrollable node left")
                handler.postDelayed({ scrollToTop() }, 800)
                return null
            }
        }

        Log.e("A11Y", "❌ Seller Details NOT FOUND after retries")
        handler.postDelayed({ scrollToTop() }, 800)
        return null
    }

    fun extractAddressBlock(raw: String): String {
        val regex = Regex(
            "Address:\\s*(.*?)\\s*(Customer Care:|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return regex.find(raw)?.groupValues?.get(1)?.trim() ?: ""
    }

    fun extractDarkStoreName(raw: String): String {
        val sellerLine = raw.lines().firstOrNull { it.contains("Seller Name:", ignoreCase = true) } ?: return ""

        val fullName = sellerLine.substringAfter("Seller Name:", "").trim()

        return fullName.substringBefore("-").trim()
    }

    fun extractPincode(address: String): String {
        val pinRegex = Regex("\\b\\d{6}\\b")
        return pinRegex.find(address)?.value ?: ""
    }

    fun extractLocalityAndCity(address: String): Pair<String, String> {
        val parts = address.split(",").map { it.trim() }

        val pincodeIndex = parts.indexOfFirst { it.contains(Regex("\\b\\d{6}\\b")) }

        var city = ""
        var locality = ""

        if (pincodeIndex != -1) {
            val lastPart = parts[pincodeIndex]
            city = lastPart.replace(Regex("\\b\\d{6}\\b"), "").trim()
        }

        if (parts.size >= 2) {
            locality = parts[parts.size - 2]
        }

        return locality to city
    }

    fun scrollToTop() {
        Log.d("A11Y", "⬆️ Scrolling back to top...")

        repeat(15) {
            val root = rootInActiveWindow ?: return
            val scrollNode = findScrollableNode(root) ?: return

            // Try scroll backward (up)
            val ok = scrollNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            Log.d("A11Y", "⬆️ Scroll up ($it) success=$ok")

            // If no more upward scrolling → break
            if (!ok) {
                Log.d("A11Y", "🟢 Reached top")
                handler.postDelayed({
                    clickQuantityText()
                }, 3000)
                return
            }

            Thread.sleep(300)
        }
    }

    fun clickQuantityText() {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is null")
            return
        }

        val nodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/quantity_text_1"
        )

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ quantity_text_1 not found on screen")
            return
        }

        val target = nodes[0]
        Log.d("A11Y", "✅ Found quantity_text_1: ${target.text}")

        if (target.isClickable) {
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d("A11Y", "👉 Clicked quantity_text_1 directly")
            return
        }

        // 🔥 If element is not clickable → click its parent
        var parent = target.parent
        while (parent != null) {
            if (parent.isClickable) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("A11Y", "👉 Clicked parent of quantity_text_1")
                handler.postDelayed({
//                    autoIncrementTillCrouton()
                    autoIncrementSafely()
                }, 2000)
                return
            }
            parent = parent.parent
        }

        Log.e("A11Y", "❌ quantity_text_1 is not clickable, no clickable parents")
    }

//    fun autoIncrementTillCrouton() {
//
//        // STEP 1 — Scroll until increment button is visible
//        if (!scrollUntilVisible("in.swiggy.android.instamart:id/increment_button_touch_target")) {
//            Log.e("A11Y", "❌ increment_button_touch_target not found after scrolling")
//            return
//        }
//
//        Log.d("A11Y", "✅ increment_button_touch_target is visible — Starting Auto Clicking...")
//
//        // STEP 2 — Auto click loop
//        while (true) {
//
//            val root = rootInActiveWindow ?: break
//
//            // 🔥 STOP CONDITION: crouton_view visible
//            val croutonNodes = root.findAccessibilityNodeInfosByViewId(
//                "in.swiggy.android.instamart:id/crouton_view"
//            )
//
//            if (!croutonNodes.isNullOrEmpty()) {
//                Log.d("A11Y", "🛑 crouton_view is visible → Stopping clicks")
//                handler.postDelayed({
//                    productInventory = getQuantityTextSafe()
//                    Log.e(TAG, "autoIncrementTillCrouton: $productInventory")
//                    clickViewCartCTA()
//                }, 3000)
//                break
//            }
//
//            // 🔘 Now perform increment click
//            val incNodes = root.findAccessibilityNodeInfosByViewId(
//                "in.swiggy.android.instamart:id/increment_button_touch_target"
//            )
//
//            if (incNodes.isNullOrEmpty()) {
//                Log.e("A11Y", "❌ increment_button_touch_target disappeared")
//                break
//            }
//
//            val incNode = incNodes[0]
//
//            // Click directly if possible
//            if (incNode.isClickable) {
//                incNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                Log.d("A11Y", "👉 Clicked increment_button_touch_target")
//            } else {
//                // Try clicking parent
//                var parent = incNode.parent
//                var clicked = false
//                while (parent != null) {
//                    if (parent.isClickable) {
//                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                        Log.d("A11Y", "👉 Clicked parent of increment_button_touch_target")
//                        clicked = true
//                        break
//                    }
//                    parent = parent.parent
//                }
//                if (!clicked) {
//                    Log.e("A11Y", "❌ No clickable parent for increment_button_touch_target")
//                    break
//                }
//            }
//
//            Thread.sleep(500) // Small delay to handle UI change
//        }
//    }

    fun getQuantityTextSafe(): String {
        val root = rootInActiveWindow ?: return ""

        val ids = listOf(
            "in.swiggy.android.instamart:id/quantity_text_1", "in.swiggy.android.instamart:id/quantity_text_2"
        )

        for (id in ids) {
            val node = findByViewId(root, id)
            if (node != null) {
                val text = node.text?.toString()?.trim().orEmpty()
                Log.e("A11Y", "🟢 Found quantity via $id → $text")
                return text
            } else {
                Log.e("A11Y", "⚠️ $id NOT visible yet")
            }
        }

        Log.e("A11Y", "❌ Quantity views exist but NOT visible on screen")
        return ""
    }

    fun findByViewId(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val list = root.findAccessibilityNodeInfosByViewId(id)
        return list.firstOrNull()
    }

    fun autoIncrementSafely() {
        val root = rootInActiveWindow ?: return

        val incNode = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/increment_button_touch_target"
        ).firstOrNull()

        if (incNode == null) {
            Log.e("A11Y", "❌ Increment button not found")
            return
        }

        // STEP 1 — Read current quantity
        val beforeText = getQuantityTextSafe()
        val beforeQty = extractQuantity(beforeText)

        Log.e("A11Y", "📦 Before increment → qty=$beforeQty")

        // STEP 2 — Click increment
        (if (incNode.isClickable) incNode else findClickableParent1(incNode))?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // STEP 3 — Wait for UI update (non-blocking)
        handler.postDelayed({

            val afterText = getQuantityTextSafe()
            val afterQty = extractQuantity(afterText)

            Log.e("A11Y", "📦 After increment → qty=$afterQty")

            if (afterQty <= beforeQty) {
                sameCountHits++
                Log.e("A11Y", "🛑 Quantity not increasing ($sameCountHits/$MAX_SAME_COUNT)")
            } else {
                sameCountHits = 0
                lastQuantity = afterQty
            }

            // STOP condition
            if (sameCountHits >= MAX_SAME_COUNT) {
                Log.e("A11Y", "🛑 Stopping auto increment — quantity capped")
                handler.postDelayed({ clickViewCartCTA() }, 2000)
                return@postDelayed
            }

            // CONTINUE
            autoIncrementSafely()

        }, 600)

    }

    fun findClickableParent1(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        repeat(6) {
            if (current?.isClickable == true) return current
            current = current?.parent
        }
        return null
    }

    fun extractQuantity(text: String): Int {
        return Regex("(\\d+)").find(text)?.value?.toIntOrNull() ?: 0
    }

    fun findNodeByIdDeep(
        root: AccessibilityNodeInfo?, viewId: String, depth: Int = 0
    ): AccessibilityNodeInfo? {
        if (root == null || depth > 25) return null

        if (root.viewIdResourceName == viewId) return root

        for (i in 0 until root.childCount) {
            val found = findNodeByIdDeep(root.getChild(i), viewId, depth + 1)
            if (found != null) return found
        }
        return null
    }

    fun getText(root: AccessibilityNodeInfo, id: String): String {
        val nodes = root.findAccessibilityNodeInfosByViewId(id)

        if (nodes.isNullOrEmpty()) {
            return ""   // ← always return empty string instead of null
        }

        val txt = nodes.firstOrNull()?.text?.toString()?.trim()

        return txt ?: ""   // ← still safe: never returns null
    }

    fun scrollUntilVisible(resourceId: String, maxScrolls: Int = 10): Boolean {
        repeat(maxScrolls) {

            val root = rootInActiveWindow ?: return false

            val nodes = root.findAccessibilityNodeInfosByViewId(resourceId)
            if (!nodes.isNullOrEmpty()) {
                Log.d("A11Y", "🎯 Found $resourceId after scrolling")
                return true
            }

            val scrollNode = findScrollableNode(root)
            if (scrollNode != null) {
                scrollNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Log.d("A11Y", "📜 Scrolling to find $resourceId")
                Thread.sleep(300)
            } else {
                Log.e("A11Y", "❌ No scrollable node found")
                return false
            }
        }

        return false
    }

    fun findScrollContainer(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val className = node.className?.toString() ?: ""

        if (className.contains("RecyclerView", true) || className.contains("ListView", true) || className.contains("ScrollView", true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val found = findScrollContainer(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    fun forceScroll(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun findAndClickHectorScraper(): Boolean {
        repeat(15) { attempt ->

            val root = rootInActiveWindow ?: return false

            val labels = root.findAccessibilityNodeInfosByViewId("android:id/text1")

            Log.e(TAG, "Attempt $attempt → Found ${labels.size} labels")

            labels.forEachIndexed { index, label ->
                val text = label.text?.toString()
                Log.e("A11Y_CHECK", "[$index] Found label = $text")

                if (text.equals("Hector Scraper", ignoreCase = true)) {

                    Log.e("A11Y_CHECK", "✅ Hector Scraper found")

                    findClickableParent(label)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    return true
                }
            }

            // Scroll
            val scrollContainer = findScrollContainer(root)
            if (scrollContainer == null || !forceScroll(scrollContainer)) {
                Log.e(TAG, "❌ Scroll container not found or scroll failed")
                return false
            }

            Thread.sleep(600)
        }

        return false
    }

    fun clickViewCartCTA() {
        val root = rootInActiveWindow ?: return

        val cartNodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/view_cart_cta"
        )

        // ❌ Not found → swipe & retry
        if (cartNodes.isNullOrEmpty()) {
            handleViewCartRetry("CTA not found")
            return
        }

        val cartNode = cartNodes.first()

        // ✅ Direct click
        if (cartNode.isClickable) {
            cartNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d("A11Y", "🛒 Clicked view_cart_cta directly")

            resetViewCartRetry()
            handler.postDelayed({ getDeliveryOrderTime() }, 7000)
            return
        }

        // ✅ Parent click
        var parent = cartNode.parent
        while (parent != null) {
            if (parent.isClickable) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("A11Y", "🛒 Clicked parent of view_cart_cta")

                resetViewCartRetry()
                handler.postDelayed({ getDeliveryOrderTime() }, 7000)
                return
            }
            parent = parent.parent
        }

        // ❌ Found but not clickable
        handleViewCartRetry("CTA found but not clickable")
    }

    private fun handleViewCartRetry(reason: String) {
        if (viewCartRetryCount >= MAX_VIEW_CART_RETRY) {
            Log.e("A11Y", "❌ View Cart failed after $viewCartRetryCount attempts ($reason)")
            resetViewCartRetry()
            return
        }

        viewCartRetryCount++
        Log.e(
            "A11Y",
            "🔁 Retry View Cart ($viewCartRetryCount/$MAX_VIEW_CART_RETRY) → $reason"
        )

        smallSwipeUp()

        handler.postDelayed({
            clickViewCartCTA()
        }, (900..1400).random().toLong())
    }

    private fun resetViewCartRetry() {
        viewCartRetryCount = 0
    }

    private fun smallSwipeUp() {
        val metrics = resources.displayMetrics

        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.6f
        val endY = metrics.heightPixels * 0.45f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        dispatchGesture(
            GestureDescription.Builder().addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, 300
                    )
                ).build(), null, null
        )

        Log.e("A11Y", "⬆️ Performed small swipe up")
    }

    fun findNodeByPartialContentDesc(node: AccessibilityNodeInfo?, keyword: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val desc = node.contentDescription?.toString()
        if (desc != null && desc.contains(keyword, ignoreCase = true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findNodeByPartialContentDesc(node.getChild(i), keyword)
            if (result != null) return result
        }
        return null
    }

    fun getDeliveryOrderTime() {
        val root = rootInActiveWindow
        val node = findNodeByPartialContentDesc(root, "Mins")

        if (node != null) {
            val dynamicText = node.contentDescription?.toString()
            Log.d("ACC", "Found dynamic text: $dynamicText")
            etaIdentifier = extractMinutes(dynamicText)
            val inventoryVal = getCurrentItemCountText()
            if (inventoryVal != "0") {
                productInventory = inventoryVal
                Log.d("ACC", "Inventory: $productInventory")
            }
//            productInventory = getCurrentItemCountText()


            handler.postDelayed({
//                dumpCurrentScreen()
//                logAllImageViews()
//                logSuperfastDetails()
//                val superFast = findSuperfastNode()
//                Log.e(TAG, "getDeliveryOrderTime1: $superFast")
//                val imageSuperfast = findSuperfastBySibling()
//                Log.e(TAG, "getDeliveryOrderTime2: $imageSuperfast")
//                val image = findSuperfastImage()
//                Log.e(TAG, "getDeliveryOrderTime3: $image")
                addDataOnSheet()
            }, 1000)
        } else {
            Log.d("ACC", "No matching node found")
        }
    }

    fun dumpNode(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null) return

        val indent = " ".repeat(depth * 2)

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        Log.e(
            "A11Y_DUMP", """
        $indent▶ NODE
        $indent class=${node.className}
        $indent id=${node.viewIdResourceName}
        $indent text=${node.text}
        $indent desc=${node.contentDescription}
        $indent clickable=${node.isClickable}
        $indent enabled=${node.isEnabled}
        $indent focusable=${node.isFocusable}
        $indent bounds=$bounds
        """.trimIndent()
        )

        for (i in 0 until node.childCount) {
            dumpNode(node.getChild(i), depth + 1)
        }
    }

    fun getCurrentItemCountText(): String {
        val root = rootInActiveWindow ?: return ""

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)

        while (stack.isNotEmpty()) {
            val node = stack.removeFirst()

            if (node.className == "android.widget.TextView") {
                val txt = node.text?.toString()?.trim()

                if (txt?.startsWith("Current item count is", ignoreCase = true) == true) {
                    Log.e("A11Y", "🟢 Found TextView: $txt")

                    val count = txt.substringAfter("Current item count is").trim()
                    Log.e("A11Y", "🧮 Item Count = $count")

                    return count
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.add(it) }
            }
        }

        Log.e("A11Y", "❌ 'Current item count is' text not found")
        return ""
    }

    fun findSuperfastNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: run {
            Log.e("A11Y", "❌ rootInActiveWindow is null")
            return null
        }

        val nodes = root.findAccessibilityNodeInfosByText("Superfast")

        if (nodes.isNullOrEmpty()) {
            Log.e("A11Y", "❌ Superfast text not found")
            return null
        }

        Log.d("A11Y", "✅ Superfast found, count = ${nodes.size}")

        return nodes[0] // first matching node
    }

    fun logSuperfastDetails() {
        val root = rootInActiveWindow ?: return

        val nodes = root.findAccessibilityNodeInfosByText("Superfast")
        if (nodes.isEmpty()) {
            Log.e(TAG, "logSuperfastDetails: No nodes found of superfast")
        }
        for (node in nodes) {
            Log.d(
                "A11Y", """
            🔹 CLASS  : ${node.className}
            🔹 TEXT   : ${node.text}
            🔹 ID     : ${node.viewIdResourceName}
            🔹 CLICK  : ${node.isClickable}
        """.trimIndent()
            )
        }
    }

    private fun addDataOnSheet() {
        val app = applicationContext as HectorScraper

        productUrl = HectorScraper.productUrl
        productId = productUrl.substringAfterLast("/")      // gets "0IFZHN76PS?share=true"
            .substringBefore("?")
        darkStoreId = HectorScraper.storeid
        merchantId = darkStoreId

        stopFlowTimer()
        val flowElapsedTimeSec = flowElapsedTimeMs / 1000

        Log.e(TAG, "darkStoreId: $darkStoreId")
        Log.e(TAG, "productId: $productId")
        Log.e(TAG, "productName: $productName")
        Log.e(TAG, "productBrand: $productBrand")
        Log.e(TAG, "productUrl: $productUrl")
        Log.e(TAG, "productWeight: $productWeight")
        Log.e(TAG, "level1Category: $level1Category")
        Log.e(TAG, "level1Category: $level1Category")
        Log.e(TAG, "level2Category: $level2Category")
        Log.e(TAG, "level3Category: $level3Category")
        Log.e(TAG, "darkStoreName: $darkStoreName")
        Log.e(TAG, "city: $city")
        Log.e(TAG, "darkStoreLocality: $darkStoreLocality")
        Log.e(TAG, "pincode: $pincode")
        Log.e(TAG, "darkStorePlusCode: $darkStorePlusCode")
        Log.e(TAG, "productInventory: $productInventory")
        Log.e(TAG, "productMRP: $productMRP")
        Log.e(TAG, "productSellingPrice: $productSellingPrice")
        Log.e(TAG, "productDiscountPercentage: $productDiscountPercentage")
        Log.e(TAG, "etaIdentifier: $etaIdentifier")
        Log.e(TAG, "timer: $flowElapsedTimeMs")

        val values = listOf(
            app.getTodayDate(),
            app.getCurrentTime(),
            darkStoreId,
            merchantId,
            productId,
            productName,
            productBrand,
            productUrl,
            productWeight,
            level1Category,
            level2Category,
            level3Category,
            darkStoreName,
            darkStoreAddress,
            city,
            darkStoreLocality,
            pincode,
            darkStorePlusCode,
            if (productInventory != "0") "true" else "false",
            productInventory,
            productMRP,
            productSellingPrice,
            productDiscountPercentage,
            etaIdentifier,
            "",
            1,
            flowElapsedTimeSec
        )
        ExcelManager.addRow(this, values as List<Any>)

        Toast.makeText(applicationContext, "Row Added!", Toast.LENGTH_SHORT).show()

        startDecrementUntilAddToCart()
    }

    private fun checkAndDecrement() {
        val root = rootInActiveWindow ?: run {
            retryCheck()
            return
        }

//        val quantityNodes = root.findAccessibilityNodeInfosByViewId(
//            "in.swiggy.android.instamart:id/quantity_text_1"
//        )
//
//        if (!quantityNodes.isNullOrEmpty()) {
//            Log.e("CART", "✅ quantity_text_1 not found → Add to Cart visible")
//            return
//        }

        // If quantity_text_1 is NOT found → STOP

        // quantity_text_1 exists → click decrement
        val decrementNodes = root.findAccessibilityNodeInfosByViewId(
            "in.swiggy.android.instamart:id/decrement_button_touch_target"
        )

        if (!decrementNodes.isNullOrEmpty()) {
            Log.e("CART", "➖ Clicking decrement")
            decrementNodes.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
            // Wait and re-check
            Handler(Looper.getMainLooper()).postDelayed({
                checkAndDecrement()
            }, 400)
        } else {
            killAppAndBackToScraperApp()
            Log.e("CART", "❌ Decrement button not found")
        }
    }

    private fun retryCheck() {
        Handler(Looper.getMainLooper()).postDelayed({
            checkAndDecrement()
        }, 300)
    }

    private fun startDecrementUntilAddToCart() {
        performGlobalAction(GLOBAL_ACTION_BACK)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAndDecrement()
        }, 600)
    }

    private fun killAppAndBackToScraperApp() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        Handler(Looper.getMainLooper()).postDelayed({
            swipeUp {
                killInstamartApp = true
                Handler(Looper.getMainLooper()).postDelayed({
                    tapCenter()
                }, 2000)
            }
        }, 2000)
    }

    fun extractMinutes(input: String?): String {
        return input?.split(",")?.firstOrNull()?.trim() ?: ""
    }

    fun extractField(text: String, label: String): String {
        val regex = Regex("$label\\s*(.*)")
        val match = regex.find(text)
        Log.e(TAG, "extractField: ${match?.groupValues?.get(1)?.trim().orEmpty()}")
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    fun extractBrand(text: String?): String {
        val regex = Regex("Explore all (.*) items", RegexOption.IGNORE_CASE)
        val match = text?.let { regex.find(it) }
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    fun extractLocality(sellerName: String): String {
        // Remove "Seller Name:" prefix if present
        val clean = sellerName.removePrefix("Seller Name:").trim()

        // Split by "-" and trim parts
        val parts = clean.split("-").map { it.trim() }

        // Area/locality is the 2nd part (index 1), if available
        Log.e(TAG, "extractLocality: ${parts.size} $clean  ${if (parts.size >= 3) parts[1] else ""}")
        return if (parts.size >= 3) parts[1] else ""
    }


    // 3️⃣ Recursive function to find TextView with contentDescription inside cartItemList1
    fun findDescTextView(node: AccessibilityNodeInfo): String? {
        // Check this node
        if (node.className == "android.widget.TextView") {
            val desc = node.contentDescription?.toString()
            if (!desc.isNullOrBlank()) {
                return desc   // FOUND!
            }
        }

        // Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findDescTextView(child)
            if (result != null) return result
        }

        return null
    }

    fun dumpAllWindows() {
        val windows = this.windows
        Log.e("A11Y_TREE", "=========== 🪟 TOTAL WINDOWS: ${windows.size} ===========")

        for (win in windows) {
            Log.e("A11Y_TREE", "\n\n📌 WINDOW: ${win.id} type=${win.type} layer=${win.layer}")
            dumpNodeTree(win.root, 0)
        }
    }

    fun dumpNodeTree(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return

        val indent = "   ".repeat(depth)

        val id = node.viewIdResourceName ?: "null"
        val cls = node.className ?: "null"
        val txt = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""

        val bounds = Rect().apply { node.getBoundsInScreen(this) }

        Log.e("A11Y_TREE", "$indent----------------------------------------------")
        Log.e("A11Y_TREE", "$indent Depth      = $depth")
        Log.e("A11Y_TREE", "$indent ID         = $id")
        Log.e("A11Y_TREE", "$indent Class      = $cls")
        Log.e("A11Y_TREE", "$indent Text       = '$txt'")
        Log.e("A11Y_TREE", "$indent Desc       = '$desc'")
        Log.e("A11Y_TREE", "$indent Clickable  = ${node.isClickable}")
        Log.e("A11Y_TREE", "$indent Focusable  = ${node.isFocusable}")
        Log.e("A11Y_TREE", "$indent Enabled    = ${node.isEnabled}")
        Log.e("A11Y_TREE", "$indent Children   = ${node.childCount}")
        Log.e("A11Y_TREE", "$indent Bounds     = $bounds")

        // 🔽 Print all children
        for (i in 0 until node.childCount) {
            dumpNodeTree(node.getChild(i), depth + 1)
        }
    }

    private fun swipeUp(onComplete: (() -> Unit)? = null) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val startX = width / 2f
        val startY = height * 0.75f
        val endY = height * 0.25f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 400)).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onComplete?.invoke()
            }
        }, null)
    }

    private fun tapCenter() {
        val metrics: DisplayMetrics = resources.displayMetrics
        val x = metrics.widthPixels / 2f
        val y = metrics.heightPixels / 2f

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder().addStroke(
            GestureDescription.StrokeDescription(
                path, 0, 100
            )
        ).build()

        dispatchGesture(gesture, null, null)
    }
}
