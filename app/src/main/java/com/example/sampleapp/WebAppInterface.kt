package com.example.sampleapp

import android.webkit.JavascriptInterface

class WebAppInterface {
    // 🔥 HTML से अलर्ट रिसीव करने वाला फंक्शन
    @JavascriptInterface
    fun triggerNativeAlert(message: String) {
        HtmlService.triggerNativeAlert(message)
    }
}
