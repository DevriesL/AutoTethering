package io.github.devriesl.autotethering

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock.elapsedRealtime
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.devriesl.autotethering.SettingsActivity.Companion.KEYWORD_TEXT
import io.github.devriesl.autotethering.SettingsActivity.Companion.SHARED_PREFS_NAME


class TetheringService : AccessibilityService() {
    private lateinit var tetherSwitchKeyword: String
    private var serviceConnected = false
    private var turnedOnTimestamp: Long = 0
    lateinit var usbBroadcastReceiver: UsbBroadcastReceiver

    private fun launchTetherSettings(context: Context) {
        while (true) {
            try {
                if (serviceConnected) {
                    val intent = Intent(Intent.ACTION_MAIN, null)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    intent.component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.TetherSettings"
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    break
                } else {
                    continue
                }
            } catch (e: Exception) {
                Thread.sleep(1_000)
            }
        }
    }

    inner class UsbBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED, UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    if (context != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            launchTetherSettings(context)
                        }, 2000)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED, UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    turnedOnTimestamp = 0
                }
            }
        }
    }

    override fun onCreate() {
        tetherSwitchKeyword = getString(R.string.ethernet_tether_checkbox_text)
        val sharedPrefs = this.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val keyword = sharedPrefs.getString(KEYWORD_TEXT, tetherSwitchKeyword)
        if (!keyword.isNullOrEmpty() && keyword.isNotEmpty()) {
            tetherSwitchKeyword = keyword
        }

        val filter = IntentFilter()
        filter.addAction("android.intent.action.BOOT_COMPLETED")
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        usbBroadcastReceiver = UsbBroadcastReceiver()
        registerReceiver(usbBroadcastReceiver, filter)
        super.onCreate()
    }

    override fun onServiceConnected() {
        serviceConnected = true
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val deviceIterator: Iterator<UsbDevice> = usbManager.deviceList.values.iterator()
        if (deviceIterator.hasNext() || usbManager.accessoryList.isNullOrEmpty()) {
            launchTetherSettings(this)
        }
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (event.packageName == "com.android.settings" && event.source != null) {
                    val rootNode: AccessibilityNodeInfo = event.source

                    val nodeList: List<AccessibilityNodeInfo> = rootNode
                        .findAccessibilityNodeInfosByText(tetherSwitchKeyword);
                    nodeList.forEach {
                        val preferenceNode = it.parent
                        for (index in 0 until preferenceNode.childCount) {
                            val child = preferenceNode.getChild(index)
                            val turnedOnDuration = elapsedRealtime() - turnedOnTimestamp
                            if (turnedOnDuration > 3000 && child.isCheckable && !child.isChecked) {
                                preferenceNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                turnedOnTimestamp = elapsedRealtime()
                            }
                        }
                    }
                }
            }
            else -> {
            }
        }
    }

    override fun onInterrupt() {}

    companion object {
        const val TAG = "TetheringService"
    }
}