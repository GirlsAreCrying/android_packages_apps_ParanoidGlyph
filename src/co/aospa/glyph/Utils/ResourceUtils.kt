package co.aospa.glyph.Utils

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import com.android.internal.util.ArrayUtils
import co.aospa.glyph.Constants.Constants
import java.io.IOException
import java.io.InputStream

object ResourceUtils {
    private const val TAG = "GlyphResourceUtils"
    private const val DEBUG = true

    private var context: Context? = null
    private var assetManager: AssetManager? = null
    private var resources: Resources? = null

    private var callAnimations: Array<String>? = null
    private var notificationAnimations: Array<String>? = null

    private fun getContext(): Context {
        if (context == null) {
            context = Constants.CONTEXT
                ?: throw IllegalStateException("Constants.CONTEXT is not initialized")
        }
        return context!!
    }

    private fun getAssetManager(): AssetManager {
        if (assetManager == null) {
            assetManager = getContext().assets
        }
        return assetManager!!
    }

    private fun getResources(): Resources {
        if (resources == null) {
            resources = getContext().resources
        }
        return resources!!
    }

    @JvmStatic
    fun getIdentifier(id: String, type: String): Int {
        val ctx = getContext()
        return getResources().getIdentifier(id, type, ctx.packageName)
    }

    @JvmStatic
    fun getBoolean(id: String): Boolean =
        getResources().getBoolean(getIdentifier(id, "bool"))

    @JvmStatic
    fun getString(id: String): String =
        getResources().getString(getIdentifier(id, "string"))

    @JvmStatic
    fun getInteger(id: String): Int =
        getResources().getInteger(getIdentifier(id, "integer"))

    @JvmStatic
    fun getStringArray(id: String): Array<String> =
        getResources().getStringArray(getIdentifier(id, "array"))

    @JvmStatic
    fun getIntArray(id: String): IntArray =
        getResources().getIntArray(getIdentifier(id, "array"))

    @JvmStatic
    fun getCallAnimations(): Array<String> {
        if (callAnimations == null) {
            callAnimations = try {
                val assets = getAssetManager().list("call") ?: emptyArray()
                assets.map { it.replace(".csv", "") }.toTypedArray()
            } catch (_: IOException) {
                emptyArray()
            }
        }
        return callAnimations!!
    }

    @JvmStatic
    fun getNotificationAnimations(): Array<String> {
        if (notificationAnimations == null) {
            notificationAnimations = try {
                val assets = getAssetManager().list("notification") ?: emptyArray()
                assets.map { it.replace(".csv", "") }.toTypedArray()
            } catch (_: IOException) {
                emptyArray()
            }
        }
        return notificationAnimations!!
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getCallAnimation(name: String): InputStream {
        if (callAnimations == null) getCallAnimations()

        return if (ArrayUtils.contains(callAnimations, name)) {
            getAssetManager().open("call/$name.csv")
        } else {
            getAssetManager().open("call/${getString("glyph_settings_call_animations_default")}.csv")
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getNotificationAnimation(name: String): InputStream {
        if (notificationAnimations == null) getNotificationAnimations()

        return if (ArrayUtils.contains(notificationAnimations, name)) {
            getAssetManager().open("notification/$name.csv")
        } else {
            getAssetManager().open("call/${getString("glyph_settings_notifs_animations_default")}.csv")
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getAnimation(name: String): InputStream {
        if (callAnimations == null) getCallAnimations()
        if (notificationAnimations == null) getNotificationAnimations()

        return when {
            ArrayUtils.contains(callAnimations, name) -> getCallAnimation(name)
            ArrayUtils.contains(notificationAnimations, name) -> getNotificationAnimation(name)
            else -> getAssetManager().open("$name.csv")
        }
    }
}
