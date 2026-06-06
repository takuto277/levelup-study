package org.example.project.core.storage

expect class KeyValueStore() {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

fun KeyValueStore.isOnboardingDone(): Boolean = getString(ONBOARDING_DONE_KEY) == "true"

fun KeyValueStore.setOnboardingDone() {
    putString(ONBOARDING_DONE_KEY, "true")
}

fun KeyValueStore.resetOnboarding() {
    remove(ONBOARDING_DONE_KEY)
}

const val ONBOARDING_DONE_KEY = "onboarding_done"
