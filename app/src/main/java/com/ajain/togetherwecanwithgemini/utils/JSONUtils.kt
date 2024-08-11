package com.ajain.togetherwecanwithgemini.utils

import android.content.Context
import com.ajain.togetherwecanwithgemini.R
import com.ajain.togetherwecanwithgemini.data.SDG
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.InputStreamReader

fun loadSDGs(context: Context): List<SDG> {
    val inputStream = context.resources.openRawResource(R.raw.sdgs)
    val reader = InputStreamReader(inputStream)
    val type = object : TypeToken<List<SDG>>() {}.type
    return Gson().fromJson(reader, type)
}