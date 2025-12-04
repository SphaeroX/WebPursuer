package com.example.webpursuer.ui

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val mContext: Context) {

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun recordInteraction(type: String, target: String, value: String) {
        (mContext as? BrowserActivity)?.onInteractionRecorded(type, target, value)
    }

    @JavascriptInterface
    fun elementSelected(selector: String) {
        // Deprecated, use updateSelector or confirmSelection
        (mContext as? BrowserActivity)?.onElementSelected(selector)
    }

    @JavascriptInterface
    fun updateSelector(selector: String) {
        (mContext as? BrowserActivity)?.onSelectorUpdated(selector)
    }

    @JavascriptInterface
    fun confirmSelection(selector: String) {
        (mContext as? BrowserActivity)?.onSelectionConfirmed(selector)
    }
}
