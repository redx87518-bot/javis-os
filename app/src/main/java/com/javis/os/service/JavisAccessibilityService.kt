package com.javis.os.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.javis.os.MainActivity

class JavisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: android.view.KeyEvent?): Boolean {
        return super.onKeyEvent(event)
    }

    fun activateJavis() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.javis.os.ACTIVATE_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    fun performSearch(query: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val searchNodes = findSearchNodes(rootNode)
        if (searchNodes.isEmpty()) return false

        val searchNode = searchNodes.first()
        searchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val args = android.os.Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
        }
        searchNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return true
    }

    private fun findSearchNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val viewId = node.viewIdResourceName ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val hint = node.hintText?.toString()?.lowercase() ?: ""

        if (node.isEditable || viewId.contains("search") || contentDesc.contains("search") || hint.contains("search")) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            results.addAll(findSearchNodes(child))
        }
        return results
    }
}
