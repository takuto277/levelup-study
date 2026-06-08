package org.example.project.di

import org.example.project.data.local.TutorialProgressStore

object TutorialHelper {
    private val store = TutorialProgressStore()

    fun isTutorialCompleted(topic: String): Boolean = store.isCompleted(topic)
    fun markTutorialCompleted(topic: String) { store.markCompleted(topic) }
    fun resetAllTutorials() { store.resetAll() }
}
