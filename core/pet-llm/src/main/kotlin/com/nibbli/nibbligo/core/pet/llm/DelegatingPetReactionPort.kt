package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelegatingPetReactionPort @Inject constructor(
    private val liteRt: LiteRtPetReactionGenerator,
    private val template: TemplatePetReactionGenerator,
    private val userPreferencesRepository: UserPreferencesRepository,
) : PetReactionPort {

    override suspend fun generate(request: PetReactionRequest): PetReaction {
        val personality = userPreferencesRepository.petPersonality.first()
        val enriched = request.copy(personality = personality)
        val useLlm = userPreferencesRepository.usePetLlmReactions.first()
        if (!useLlm) return template.generate(enriched)
        return try {
            liteRt.generate(enriched)
        } catch (_: Exception) {
            template.generate(enriched)
        }
    }
}
