package com.nibbli.nibbligo.core.runtime

import com.nibbli.nibbligo.core.model.RuntimeKind

interface RuntimePreference {
    fun preferredKind(): RuntimeKind
    fun isLiteRtModelPresent(): Boolean
}
