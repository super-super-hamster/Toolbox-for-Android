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

    // 分贝仪校准窗口
    var showDecibelMeterOffsetDialog by mutableStateOf(false)

    // 助手记录
    val uiHistory = mutableStateListOf<ChatUiModel>()
    val apiHistory = mutableListOf<com.hamster.toolbox.ai.Message>()

    // 取色器提取的颜色
    var pickedColor by mutableStateOf("")

    // 随机数
    var randomNumberMax by mutableIntStateOf(0)
    var randomNumberMin by mutableIntStateOf(0)
    var generatedRandomNumber by mutableIntStateOf(0)
    var tryGenerateRandomNumber by mutableStateOf(false)

    suspend fun requireUserConfirmation(title: String, message: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        val confirmCard = ChatUiModel.ConfirmCard(title, message, deferred)

        withContext(Dispatchers.Main) {
            uiHistory.add(confirmCard)
        }

        val isConfirmed = deferred.await()

        return isConfirmed
    }
}