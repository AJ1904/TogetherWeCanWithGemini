package com.ajain.togetherwecanwithgemini.viewmodels

import androidx.lifecycle.ViewModel
import com.ajain.togetherwecanwithgemini.data.AppConfig

open class BaseViewModel : ViewModel() {
    fun getLocaleLanguage(): String {
        return AppConfig.deviceLocale.displayLanguage
    }
    fun getLocaleLanguageCode(): String {
        return AppConfig.deviceLocale.language
    }
}
