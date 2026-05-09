package com.hamster.toolbox.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.hamster.toolbox.ai.ChatUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    // 显示添加热词弹窗
    var isShowAddKeywordDialog by mutableStateOf(false)

    // 日记
    var showAddDiaryDialog by mutableStateOf(false)
    var isAddDiaryImage by mutableStateOf(false)

    // 语音识别
    private var _speechFinalResult by mutableStateOf("")
    val speechFinalResult: String get() = _speechFinalResult
    fun setSpeechFinalResult(result: String) {
        _speechFinalResult = result
    }

    // 设置跳转
    private var _settingsScrollTarget by mutableStateOf("")
    val settingsScrollTarget: String get() = _settingsScrollTarget
    fun setSettingsScrollTarget(target: String) {
        ++_settingsScrollTrigger
        _settingsScrollTarget = target
    }
    private var _settingsScrollTrigger by mutableIntStateOf(0)
    val settingsScrollTrigger: Int get() = _settingsScrollTrigger

    // 应用使用时间隐藏App
    private var _isSetInvisibleApp by mutableStateOf(false)
    val isSetInvisibleApp: Boolean get() = _isSetInvisibleApp
    fun changeStateOfIsSetInvisibleApp() {
        _isSetInvisibleApp = !_isSetInvisibleApp
    }

    var showDecibelMeterOffsetDialog by mutableStateOf(false)

    val uiHistory = mutableStateListOf<ChatUiModel>()
    val apiHistory = mutableListOf<com.hamster.toolbox.ai.Message>()

    /**
     * 【核心挂起桥梁】供 Tool 在 execute() 中调用
     * 只要调用了这个方法，当前的工具执行协程就会停在这里，直到 UI 点击了按钮！
     */
    suspend fun requireUserConfirmation(title: String, message: String): Boolean {
        // 1. 创建一个未完成的“未来承诺”
        val deferred = CompletableDeferred<Boolean>()

        // 2. 生成一个确认卡片，包含这个凭证，塞进 UI 列表
        val confirmCard = ChatUiModel.ConfirmCard(title, message, deferred)

        withContext(Dispatchers.Main) {
            uiHistory.add(confirmCard)
        }

        // 3. 【挂起当前协程】等待 UI 层的按钮去调用 deferred.complete()
        val isConfirmed = deferred.await()

        return isConfirmed
    }
}