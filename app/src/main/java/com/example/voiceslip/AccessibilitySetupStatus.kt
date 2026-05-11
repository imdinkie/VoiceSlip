package com.example.voiceslip

internal fun isVoiceSlipAccessibilityServiceEnabled(
    packageName: String,
    enabledServiceIds: List<String>,
    serviceConnected: Boolean
): Boolean {
    if (serviceConnected) return true
    return enabledServiceIds.any { serviceId ->
        val parts = serviceId.split('/', limit = 2)
        if (parts.size != 2) return@any false
        val servicePackage = parts[0]
        val className = parts[1].let { if (it.startsWith(".")) servicePackage + it else it }
        servicePackage == packageName &&
            className == "$packageName.service.VoiceSlipAccessibilityService"
    }
}
