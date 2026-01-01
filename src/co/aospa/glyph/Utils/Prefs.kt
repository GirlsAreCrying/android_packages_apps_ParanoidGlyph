package co.aospa.glyph.Utils

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    fun default(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }
}