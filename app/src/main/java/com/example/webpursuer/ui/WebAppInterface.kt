package com.example.webpursuer.ui

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject

class WebAppInterface(private val mContext: Context) {

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun recordInteraction(type: String, target: String, value: String) {
        // TODO: Store interaction in a list or database
        // For now, just log it or show a toast
        val message = "Recorded: $type on $target ($value)"
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
        
        // Example of how we might structure the data
        val interaction = JSONObject()
        interaction.put("type", type)
        interaction.put("target", target)
        interaction.put("value", value)
        interaction.put("timestamp", System.currentTimeMillis())
        
        // Callback to Activity to save this interaction (needs an interface or listener)
        // (mContext as? BrowserActivity)?.onInteractionRecorded(interaction)
    }

    @JavascriptInterface
    fun elementSelected(selector: String) {
        (mContext as? BrowserActivity)?.onElementSelected(selector)
    }
}
