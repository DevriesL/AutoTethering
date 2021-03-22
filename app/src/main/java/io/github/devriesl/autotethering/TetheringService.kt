package io.github.devriesl.autotethering

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.hardware.usb.UsbManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TetheringService : AccessibilityService() {
    private var serviceConnected = false
    lateinit var usbBroadcastReceiver: UsbBroadcastReceiver

    inner class UsbBroadcastReceiver : BroadcastReceiver() {
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
                        Thread.sleep(5_000)
                        continue
                    }
                } catch (e: Exception) {
                    Thread.sleep(1_000)
                }
            }
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    if (context != null) {
                        launchTetherSettings(context)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        val filter = IntentFilter()
        filter.addAction("android.intent.action.BOOT_COMPLETED")
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        usbBroadcastReceiver = UsbBroadcastReceiver()
        registerReceiver(usbBroadcastReceiver, filter)
        super.onCreate()
    }

    override fun onServiceConnected() {
        serviceConnected = true
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (event.packageName == "com.android.settings" && event.source != null) {
                    val rootNode: AccessibilityNodeInfo = event.source

                    val nodeList: List<AccessibilityNodeInfo> = rootNode
                        .findAccessibilityNodeInfosByText(getString(R.string.ethernet_tether_checkbox_text));
                    nodeList.forEach {
                        val preferenceNode = it.parent
                        for (index in 0 until preferenceNode.childCount) {
                            val child = preferenceNode.getChild(index)
                            if (child.isCheckable && !child.isChecked)
                                preferenceNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                    Thread.sleep(1_000)
                }
            }
            else -> { }
        }
    }

    override fun onInterrupt() { }

    companion object {
        const val TAG = "TetheringService"
    }
}