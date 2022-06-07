package kz.gov.mia.sos.widget.data.local.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kz.gov.mia.sos.widget.core.Preferences
import kz.gov.mia.sos.widget.domain.repository.SettingsRepository
import kz.inqbox.sdk.domain.model.language.Language

internal class SharedPreferencesSettingsDataSource constructor(
    private val context: Context
) : SettingsRepository {

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferencesSettingsDataSource(context).also { INSTANCE = it }
            }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

    private object Key {
        const val LANGUAGE = "language"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        Preferences.getSharedPreferences(context)
    }

    override fun getLanguage(): Language {
        val key = sharedPreferences.getString(Key.LANGUAGE, null)
        if (key.isNullOrBlank()) return Language.DEFAULT
        return Language.by(key)
    }

    override fun setLanguage(language: Language): Boolean {
        sharedPreferences.edit { putString(Key.LANGUAGE, language.key) }
        return getLanguage().key == language.key
    }

    override fun clear(): Boolean {
        sharedPreferences.edit {
            arrayOf(Key.LANGUAGE).forEach { remove(it) }
        }
        return sharedPreferences.all.isEmpty()
    }

}