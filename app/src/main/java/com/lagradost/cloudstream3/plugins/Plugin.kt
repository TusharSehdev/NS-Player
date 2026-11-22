package com.lagradost.cloudstream3.plugins

import android.content.Context
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.utils.ExtractorApi

open class Plugin {
    open var filename: String = ""
    open var openSettings: ((Context) -> Unit)? = null
    val registeredApis = mutableListOf<MainAPI>()
    val registeredExtractors = mutableListOf<ExtractorApi>()

    open fun load(context: Context) {}

    fun registerMainAPI(api: MainAPI) {
        registeredApis.add(api)
    }

    fun registerExtractorAPI(extractor: ExtractorApi) {
        registeredExtractors.add(extractor)
    }
}

open class CloudstreamPlugin : Plugin()
