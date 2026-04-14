package com.hamster.toolbox.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var isShowAddKeywordDialog by mutableStateOf(false)

    private var _speechFinalResult by mutableStateOf("")
    val speechFinalResult: String get() = _speechFinalResult
    fun setSpeechFinalResult(result: String) {
        _speechFinalResult = result
    }

    private var _settingsScrollTarget by mutableStateOf("")
    val settingsScrollTarget: String get() = _settingsScrollTarget
    fun setSettingsScrollTarget(target: String) {
        ++_settingsScrollTrigger
        _settingsScrollTarget = target
    }

    private var _settingsScrollTrigger by mutableIntStateOf(0)
    val settingsScrollTrigger: Int get() = _settingsScrollTrigger

    private var _isSetInvisibleApp by mutableStateOf(false)
    val isSetInvisibleApp: Boolean get() = _isSetInvisibleApp
    fun changeStateOfIsSetInvisibleApp() {
        _isSetInvisibleApp = !_isSetInvisibleApp
    }
}