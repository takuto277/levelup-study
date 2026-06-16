package org.example.project.core.storage

interface KeyValueStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

expect class KeyValueStore() : KeyValueStorage {
    override fun getString(key: String): String?
    override fun putString(key: String, value: String)
    override fun remove(key: String)
    override fun clear()
}

fun KeyValueStore.isOnboardingDone(): Boolean = getString(ONBOARDING_DONE_KEY) == "true"

fun KeyValueStore.setOnboardingDone() {
    putString(ONBOARDING_DONE_KEY, "true")
}

fun KeyValueStore.resetOnboarding() {
    remove(ONBOARDING_DONE_KEY)
}

const val ONBOARDING_DONE_KEY = "onboarding_done"
