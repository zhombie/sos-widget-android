package kz.gov.mia.sos.widget.domain.repository

import kz.inqbox.sdk.domain.model.language.Language

interface SettingsRepository {
    fun getLanguage(): Language
    fun setLanguage(language: Language): Boolean

    fun clear(): Boolean
}
