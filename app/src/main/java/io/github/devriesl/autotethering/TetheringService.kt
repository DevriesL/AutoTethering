package io.github.devriesl.autotethering

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TetheringService : AccessibilityService() {
    lateinit var usbBroadcastReceiver: UsbBroadcastReceiver

    inner class UsbBroadcastReceiver : BroadcastReceiver() {
        private fun launchTetherSettings(context: Context) {
            while (true) {
                try {
                    val intent = Intent(Intent.ACTION_MAIN, null)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    intent.component = ComponentName(
                        "com.android.settings",
                        "com.android.settings.TetherSettings"
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    break
                } catch (e: java.lang.Exception) {
                    Thread.sleep(1_000)
                }
            }
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null) {
                launchTetherSettings(context)
            }
        }
    }

    override fun onCreate() {
        val filter = IntentFilter()
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        usbBroadcastReceiver = UsbBroadcastReceiver()
        registerReceiver(usbBroadcastReceiver, filter)
        super.onCreate()
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (event.packageName == "com.android.settings" && event.source != null) {
                    val rootNode: AccessibilityNodeInfo = event.source

                    val nodeList: List<AccessibilityNodeInfo> = rootNode
                        .findAccessibilityNodeInfosByText("以太网络共享");
                    nodeList.forEach {
                        val preferenceNode = it.parent
                        for (index in 0 until preferenceNode.childCount) {
                            val child = preferenceNode.getChild(index)
                            if (child.isCheckable && child.text == "关闭")
                                preferenceNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
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