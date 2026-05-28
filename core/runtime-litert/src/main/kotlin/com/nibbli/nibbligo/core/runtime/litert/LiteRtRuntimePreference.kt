package com.nibbli.nibbligo.core.runtime.litert

import android.content.Context
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.runtime.RuntimePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtRuntimePreference @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : RuntimePreference {

    override fun preferredKind(): RuntimeKind = runBlocking {
        val pref = userPreferencesRepository.preferredRuntimeKind.first()
        if (pref == "litert") RuntimeKind.LITERT else RuntimeKind.FAKE
    }

    override fun isLiteRtModelPresent(): Boolean {
        val dir = File(context.filesDir, "models")
        return dir.listFiles()?.any { it.name.endsWith(".litertlm") } == true
    }
}
