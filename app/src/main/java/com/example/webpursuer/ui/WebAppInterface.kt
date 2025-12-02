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
        (mContext as? BrowserActivity)?.onElementSelected(selector)
    }
}
