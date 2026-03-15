package com.hamster.toolbox.utils.weather

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

@Keep
data class WeatherResponse(
    @SerializedName("code") val code: String,
    @SerializedName("now") val now: WeatherNow?
)

@Keep
data class WeatherNow(
    @SerializedName("temp") val temp: String,       // 温度
    @SerializedName("text") val text: String,       // 天气状况
    @SerializedName("icon") val icon: String,       // 天气图标代码
    @SerializedName("windDir") val windDir: String  // 风向
)

interface QWeatherApi {
    @GET("v7/weather/now?lang=zh")
    suspend fun getWeatherNow(
        @Query("location") location: String,
        @Header("X-QW-Api-Key") apiKey: String
    ): WeatherResponse
}

object WeatherApiClient {
    private var currentRetrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var apiInstance: QWeatherApi? = null

    fun getApi(baseUrl: String): QWeatherApi {
        val safeBaseUrl = "https://" + if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl

        if (currentRetrofit == null || currentBaseUrl != safeBaseUrl) {
            currentRetrofit = Retrofit.Builder()
                .baseUrl(safeBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiInstance = currentRetrofit!!.create(QWeatherApi::class.java)
            currentBaseUrl = safeBaseUrl
        }

        return apiInstance!!
    }
}