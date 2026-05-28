package com.nibbli.nibbligo.core.model

sealed class PetEvent {
    data object AssistantSuccess : PetEvent()
    data class NewModelTried(val modelId: String) : PetEvent()
    data object PromptLabRun : PetEvent()
    data object ActionCompleted : PetEvent()
    data object AgentStepCompleted : PetEvent()
    data class SkillInvoked(val skillId: String) : PetEvent()
    data object NeglectTick : PetEvent()
    data class Evolution(val stage: LifeStage) : PetEvent()
    data object BecameSick : PetEvent()
}
