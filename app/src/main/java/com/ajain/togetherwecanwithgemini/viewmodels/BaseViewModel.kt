package com.ajain.togetherwecanwithgemini.viewmodels

import androidx.lifecycle.ViewModel
import com.ajain.togetherwecanwithgemini.data.AppConfig

// BaseViewModel provides common functionality for view models related to locale management
open class BaseViewModel : ViewModel() {
    
    // Retrieves the display language of the device locale
    fun getLocaleLanguage(): String {
        return AppConfig.deviceLocale.displayLanguage
    }
    
    // Retrieves the language code of the device locale
    fun getLocaleLanguageCode(): String {
        return AppConfig.deviceLocale.language
    }
}
