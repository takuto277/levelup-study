package org.example.project.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.AppEnvironment
import org.example.project.core.network.DevJwtSelector
import org.example.project.core.network.getOrThrow
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.gateway.UserGateway
import org.example.project.domain.repository.UserRepository

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val userGateway: UserGateway,
) : ViewModel() {

    /** refresh() / patch() の連打（SwiftUI onAppear やボタン連打など）で重複実行しないようにする */
    private val refreshMutex = Mutex()
    private val patchMutex = Mutex()

    private val _uiState = MutableStateFlow(SettingsUiState(
        apiBaseUrl = ApiRoutes.BASE_URL,
        selectedEnvironment = AppEnvironment.resolveFromUrl(ApiRoutes.BASE_URL)?.name?.lowercase() ?: "",
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Refresh -> refresh()
            is SettingsIntent.PatchCurrencies -> patch(intent.stonesDelta, intent.goldDelta)
            SettingsIntent.ClearToast -> _uiState.update { it.copy(toast = null) }
        }
    }

    /** Swift / プラットフォーム UI から呼びやすいラッパー */
    fun refreshFromPlatform() = onIntent(SettingsIntent.Refresh)

    fun patchCurrenciesFromPlatform(stonesDelta: Int, goldDelta: Int) =
        onIntent(SettingsIntent.PatchCurrencies(stonesDelta, goldDelta))

    fun clearToastFromPlatform() = onIntent(SettingsIntent.ClearToast)

    /** Swift 互換用: overrideDevUrl なし（iOS は localhost をそのまま使う） */
    fun setEnvironmentFromPlatform(envName: String) {
        setEnvironmentFromPlatform(envName, null)
    }

    /** デバッグビルド用: 環境を切り替え、ApiRoutes.BASE_URL と保存先を更新する */
    fun setEnvironmentFromPlatform(envName: String, overrideDevUrl: String?) {
        val env = AppEnvironment.entries.find { it.name.lowercase() == envName.lowercase() } ?: return
        val url = if (env == AppEnvironment.DEV && overrideDevUrl != null) overrideDevUrl else env.url
        ApiRoutes.BASE_URL = url
        DevJwtSelector.selectForEnvironment(env.name.lowercase())
        UserSessionStore.setDebugEnvironment(env.name.lowercase())
        _uiState.update {
            it.copy(
                apiBaseUrl = url,
                selectedEnvironment = env.name.lowercase(),
            )
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (!refreshMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isLoading = true, toast = null) }
                try {
                    val u = userRepository.syncFromServer()
                    _uiState.update {
                        it.copy(
                            displayedUserId = UserSessionStore.userId.orEmpty(),
                            apiBaseUrl = ApiRoutes.BASE_URL,
                            selectedEnvironment = AppEnvironment.resolveFromUrl(ApiRoutes.BASE_URL)?.name?.lowercase() ?: "",
                            stones = u.stones,
                            gold = u.gold,
                            forceDevSeed = UserSessionStore.isForceDevSeedUserId(),
                            isLoading = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            toast = e.message ?: "ユーザー情報の取得に失敗しました",
                            apiBaseUrl = ApiRoutes.BASE_URL,
                            selectedEnvironment = AppEnvironment.resolveFromUrl(ApiRoutes.BASE_URL)?.name?.lowercase() ?: "",
                        )
                    }
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    private fun patch(stonesDelta: Int, goldDelta: Int) {
        viewModelScope.launch {
            if (!patchMutex.tryLock()) return@launch
            _uiState.update { it.copy(isLoading = true, toast = null) }
            try {
                val uid = UserSessionStore.requireUserId()
                userGateway.debugPatchCurrencies(uid, stonesDelta, goldDelta).getOrThrow()
                val u = userRepository.syncFromServer()
                userRepository.updateCachedUser(u)
                _uiState.update {
                    it.copy(
                        displayedUserId = UserSessionStore.userId.orEmpty(),
                        stones = u.stones,
                        gold = u.gold,
                        isLoading = false,
                        toast = "通貨を更新しました（石 ${u.stones} / ゴールド ${u.gold}）",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toast = e.message ?: "デバッグ API が失敗しました（DEV_MODE=true と seed ユーザーで試してください）",
                    )
                }
            } finally {
                patchMutex.unlock()
            }
        }
    }
}
