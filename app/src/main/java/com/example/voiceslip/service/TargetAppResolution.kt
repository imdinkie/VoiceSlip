package com.example.voiceslip.service

internal fun resolveTargetAppPackage(
    focusedWindowPackage: String?,
    activeRootPackage: String?,
    editableNodePackage: String?,
    inputEditorPackage: String?,
    ownPackage: String
): String? {
    return listOf(
        focusedWindowPackage,
        activeRootPackage,
        editableNodePackage,
        inputEditorPackage
    ).firstOrNull { packageName ->
        !packageName.isNullOrBlank() && packageName != ownPackage
    }
}
