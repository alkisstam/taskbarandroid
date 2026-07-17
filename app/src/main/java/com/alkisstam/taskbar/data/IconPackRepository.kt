package com.alkisstam.taskbar.data

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import javax.inject.Inject
import javax.inject.Singleton

data class IconPackInfo(val packageName: String, val label: String)

@Singleton
class IconPackRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val themeActions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme"
    )

    private class PackData(
        val resources: Resources,
        // exact "pkg/activity" -> drawable name, plus first drawable seen per package
        val componentMap: Map<String, String>,
        val packageFallback: Map<String, String>
    )

    @Volatile private var cachedPack: Pair<String, PackData>? = null

    // Cached Resources go stale when the pack apk is updated or removed.
    fun invalidateCache() {
        cachedPack = null
    }

    fun listInstalledPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        return themeActions
            .flatMap { action -> pm.queryIntentActivities(Intent(action), 0) }
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .map { IconPackInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    fun getIcon(packPackage: String, appPackage: String, activityName: String): Drawable? {
        val pack = loadPack(packPackage) ?: return null
        val drawableName = pack.componentMap["$appPackage/$activityName"]
            ?: pack.packageFallback[appPackage]
            ?: return null
        return try {
            val resId = pack.resources.getIdentifier(drawableName, "drawable", packPackage)
            if (resId == 0) null
            else pack.resources.getDrawableForDensity(resId, DisplayMetrics.DENSITY_XHIGH, null)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadPack(packPackage: String): PackData? {
        cachedPack?.let { (pkg, data) -> if (pkg == packPackage) return data }
        val data = try {
            val res = context.packageManager.getResourcesForApplication(packPackage)
            val componentMap = mutableMapOf<String, String>()
            val packageFallback = mutableMapOf<String, String>()
            parseAppFilter(res, packPackage) { component, drawable ->
                // component format: ComponentInfo{pkg/activity}
                val inner = component.substringAfter("ComponentInfo{", "").substringBefore("}", "")
                if (inner.contains('/')) {
                    componentMap.putIfAbsent(inner, drawable)
                    packageFallback.putIfAbsent(inner.substringBefore('/'), drawable)
                }
            }
            PackData(res, componentMap, packageFallback)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to load icon pack $packPackage", e)
            null
        } ?: return null
        cachedPack = packPackage to data
        return data
    }

    private inline fun parseAppFilter(res: Resources, packPackage: String, onItem: (component: String, drawable: String) -> Unit) {
        // Packs ship appfilter.xml either in assets or as an xml resource.
        try {
            res.assets.open("appfilter.xml").use { stream ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(stream, null)
                parseItems(parser, onItem)
                return
            }
        } catch (e: Exception) {
            // fall through to xml resource
        }
        val xmlId = res.getIdentifier("appfilter", "xml", packPackage)
        if (xmlId != 0) {
            parseItems(res.getXml(xmlId), onItem)
        }
    }

    private inline fun parseItems(parser: XmlPullParser, onItem: (component: String, drawable: String) -> Unit) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                var component: String? = null
                var drawable: String? = null
                for (i in 0 until parser.attributeCount) {
                    when (parser.getAttributeName(i)) {
                        "component" -> component = parser.getAttributeValue(i)
                        "drawable" -> drawable = parser.getAttributeValue(i)
                    }
                }
                if (component != null && drawable != null) onItem(component, drawable)
            }
            event = parser.next()
        }
    }

    companion object {
        private const val TAG = "IconPackRepository"
    }
}
