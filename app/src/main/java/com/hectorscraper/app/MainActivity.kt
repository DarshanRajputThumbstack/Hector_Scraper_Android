package com.hectorscraper.app

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hectorscraper.app.api.HectorViewModel
import com.hectorscraper.app.api.HectorViewModelFactory
import com.hectorscraper.app.api.model.NetworkStatus
import com.hectorscraper.app.databinding.ActivityMainBinding
import com.hectorscraper.app.databinding.DialogPincodeBinding
import com.hectorscraper.app.utils.AccessibilityChecker
import com.hectorscraper.app.utils.AccessibilityChecker.canMockLocation
import com.hectorscraper.app.utils.AccessibilityChecker.getLatLngFromPincodeOSM
import com.hectorscraper.app.utils.AccessibilityChecker.hasLocationPermission
import com.hectorscraper.app.utils.AccessibilityChecker.hasNotificationPermission
import com.hectorscraper.app.utils.AccessibilityChecker.openMockLocationSettings
import com.hectorscraper.app.utils.AccessibilityChecker.pincodeToLatLng
import com.hectorscraper.app.utils.AccessibilityChecker.setMockLocation
import com.hectorscraper.app.utils.CustomProgressDialog
import com.hectorscraper.app.utils.ExcelManager
import com.hectorscraper.app.utils.HectorScraper
import com.hectorscraper.app.utils.HectorScraper.Companion.currentIndex
import com.hectorscraper.app.utils.HectorScraper.Companion.pincodeList

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    lateinit var customProgressDialog: CustomProgressDialog
    private var currentPincode = ""

    private val viewModel by lazy {
        ViewModelProvider(this, HectorViewModelFactory(application))[HectorViewModel::class.java]
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->

        val locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) result[Manifest.permission.POST_NOTIFICATIONS] == true
        else true

        if (locationGranted && notificationGranted) {
            onAllPermissionsGranted()
        } else {
            showPermissionBlockedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        init()
    }

    private fun init() = with(binding) {
        currentPincode = pincodeList[currentIndex]
        customProgressDialog = CustomProgressDialog(this@MainActivity)
        customProgressDialog.setCanceledOnTouchOutside(false)
        setUpObserver()
//        viewModel.doRequestForJobCategory()
        requestRequiredPermissions()
        btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnStartAutomation.setOnClickListener {
//            val intent = Intent(ACTION_START_AUTOMATION)
//            sendBroadcast(intent)
            Toast.makeText(this@MainActivity, "Current Pincode: $currentPincode", Toast.LENGTH_SHORT).show()
            setLatLong(currentPincode)
//            sendBroadcast(
//                Intent().apply {
//                    action = MyAccessibilityService.ACTION_START_AUTOMATION
//                })
//            Toast.makeText(applicationContext, "Sent start command — enable accessibility and try again if needed", Toast.LENGTH_SHORT).show()
        }

        binding.btnExportData.setOnClickListener {
            exportCsv(this@MainActivity)
        }

        binding.ivEditPincode.setOnClickListener {
            showPincodeDialog()
        }
    }

    private fun onAllPermissionsGranted() {
        // Enable app features here
        // Start mock service / load dashboard
    }

    private fun showPermissionBlockedDialog() {
        AlertDialog.Builder(this).setTitle("Permissions Required").setMessage(
            "Location and notification permissions are required to use this app. " + "Without them, app features cannot work."
        ).setCancelable(false).setPositiveButton("Grant Permissions") { _, _ ->
            openAppSettings()
        }.show()
    }

    fun showPincodeDialog() {

        val dialogBinding = DialogPincodeBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this).setTitle("Enter Pincode").setView(dialogBinding.root).setCancelable(false).setPositiveButton("OK", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }.create()

        dialog.setOnShowListener {

            val okBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            okBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            cancelBtn.setTextColor(ContextCompat.getColor(this, R.color.white))

            okBtn.setOnClickListener {
                val pincode = dialogBinding.etPincode.text.toString().trim()

                // ✅ 6-digit validation
                if (!pincode.matches(Regex("^[0-9]{6}$"))) {
                    dialogBinding.etPincode.error = "Enter valid 6-digit pincode"
                    return@setOnClickListener
                }

                // ✅ Update parent TextView
                binding.tvPincode.text = "Pincode: $pincode"
                setLatLong(pincode)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun setLatLong(pincode: String) {
        showLoader()
        getLatLngFromPincodeOSM(pincode) { latLng ->
            runOnUiThread {

                // 1️⃣ LatLng check
                if (latLng == null) {
                    Log.e("PINCODE", "Invalid pincode")
                    return@runOnUiThread
                }

                // 2️⃣ Permission check
                if (!canMockLocation(this)) {
                    Log.e("MOCK_LOCATION", "❌ App is NOT allowed as mock location provider")
                    // Optionally guide user
                    openMockLocationSettings(this)
                    return@runOnUiThread
                }

                // 3️⃣ Safe execution
                Log.e("PINCODE", " ${currentPincode}  Lat=${latLng.lat}, Lng=${latLng.lng}")
                HectorScraper.currentLat = latLng.lat
                HectorScraper.currentLng = latLng.lng
                Log.e("PINCODE", "${currentPincode}   Lat=${HectorScraper.currentLat}, Lng=${HectorScraper.currentLng}")

                val intent = Intent(this@MainActivity, MockLocationService::class.java).apply {
                    putExtra("LAT", latLng.lat)   // dynamic
                    putExtra("LNG", latLng.lng)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            hideLoader()
            sendBroadcast(Intent().apply {
                action = MyAccessibilityService.ACTION_START_AUTOMATION
            })
            Toast.makeText(applicationContext, "Sent start command — enable accessibility and try again if needed", Toast.LENGTH_SHORT).show()
        }, 4000)
    }

    private fun checkAccessibilityPermission() {
        val enabled = AccessibilityChecker.isServiceEnabled(
            this, MyAccessibilityService::class.java
        )

        Log.e("TAG", "checkAccessibilityPermission: $enabled")

        if (!enabled) {
            showAccessibilityDialog()
        }
    }

    private fun showAccessibilityDialog() {
//        AlertDialog.Builder(this).setTitle("Enable Accessibility Permission")
//            .setMessage("To run Instamart automation, please enable the Accessibility Service.").setCancelable(false)
//            .setPositiveButton("Open Settings") { _, _ ->
//                openAccessibilitySettings()
//            }.setNegativeButton("Cancel", null).show()

        val dialog = AlertDialog.Builder(this).setTitle("Enable Accessibility Permission")
            .setMessage("To run Instamart automation, please enable the Accessibility Service.").setCancelable(false).setPositiveButton("Open Settings", null)
            .setNegativeButton("Cancel", null).create()

        dialog.setOnShowListener {

            val positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            negativeBtn.setTextColor(ContextCompat.getColor(this, R.color.white))

            positiveBtn.setOnClickListener {
                openAccessibilitySettings()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpObserver() {
        viewModel.categoryData.observe(this) {
            HectorScraper.keyword = it.category.toString()
            Toast.makeText(applicationContext, "${it.category}", Toast.LENGTH_SHORT).show()
        }
        viewModel.getNetworkStates().observe(this) {
            binding.apply {
                when (it) {
                    is NetworkStatus.Running -> {
                        showLoader()
                    }

                    is NetworkStatus.Failed -> {
                        hideLoader()
                        Toast.makeText(applicationContext, it.msg, Toast.LENGTH_SHORT).show()
                    }

                    is NetworkStatus.SessionExpired -> {
                        hideLoader()
                        Toast.makeText(applicationContext, it.msg, Toast.LENGTH_SHORT).show()
                    }

                    is NetworkStatus.Internet -> {
                        hideLoader()
                    }

                    is NetworkStatus.Success -> {
                        hideLoader()
                    }

                    else -> {
                        hideLoader()
                    }
                }
            }
        }
    }

    private fun exportCsv(context: Context) {
        ExcelManager.exportExcel(context)
    }

    fun hideLoader() {
        try {
            loader(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showLoader() {
        loader(true)
    }

    fun loader(show: Boolean) {
        try {
            if (show) {
                if (!customProgressDialog.isShowing) customProgressDialog.show()
            } else {
                if (customProgressDialog.isShowing) customProgressDialog.dismiss()
            }
        } catch (e: Exception) {
            Log.e("TAG", "loader: " + e.message.toString())
        }
    }


    override fun onResume() {
        super.onResume()
        Log.e("TAG", "onResume: $currentIndex")
        checkAccessibilityPermission()
        if (hasLocationPermission(this) && hasNotificationPermission(this)) {
            onAllPermissionsGranted()
        }
        if (HectorScraper.killInstamartApp) {
            HectorScraper.isAddressStored = false
            currentIndex++
            if (currentIndex >= pincodeList.size) {
                Log.e("PIN_PROCESS", "✅ All pincodes completed")
                Toast.makeText(this@MainActivity, "✅ All pincodes completed", Toast.LENGTH_SHORT).show()
                return
            }

            currentPincode = pincodeList[currentIndex]
            Log.e("TAG", "onResume: $currentIndex  $currentPincode   ${pincodeList.size}")
            binding.btnStartAutomation.performClick()
            HectorScraper.killInstamartApp = false
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    fun setDeviceMockLocation() {
        val pin = "400001"
        val latLng = pincodeToLatLng(pin)

        if (latLng != null) {
            setMockLocation(this, latLng.lat, latLng.lng)
        } else {
            Log.e("PIN", "Unknown pincode")
        }
    }
}