package com.hamster.toolbox.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AiResponse(
    @SerializedName("type") val type: String,
    @SerializedName("content") val content: String
)

@Keep
data class BalanceResponse(
    @SerializedName("is_available") val isAvailable: Boolean,
    @SerializedName("balance_infos") val balanceInfos: List<BalanceInfo>
)

@Keep
data class BalanceInfo(
    @SerializedName("currency") val currency: String,
    @SerializedName("total_balance") val totalBalance: String
)