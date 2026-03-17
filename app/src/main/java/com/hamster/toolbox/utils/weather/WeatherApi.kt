package com.hamster.toolbox.utils.weather

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
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

@Keep
data class GeoResponse(
    @SerializedName("code") val code: String,
    @SerializedName("location") val location: List<GeoLocation>?
)

@Keep
data class GeoLocation(
    @SerializedName("name") val name: String, // 地区/县名
    @SerializedName("adm2") val adm2: String, // 市名
    @SerializedName("adm1") val adm1: String  // 省/州/直辖市名
)

interface QWeatherApi {
    @GET("v7/weather/now?lang=zh")
    suspend fun getWeatherNow(
        @Query("location") location: String,
        @Header("X-QW-Api-Key") apiKey: String
    ): WeatherResponse

    @GET("geo/v2/city/lookup?lang=zh")
    suspend fun getCityInfo(
        @Query("location") location: String,
        @Header("X-QW-Api-Key") apiKey: String
    ): GeoResponse
}

object WeatherApiClient {
    private var currentRetrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var apiInstance: QWeatherApi? = null

    fun getApi(baseUrl: String): QWeatherApi {
        val cleanUrl = baseUrl.removePrefix("https://").removePrefix("http://")

        val safeBaseUrl = "https://" + if (!cleanUrl.endsWith("/")) "$cleanUrl/" else cleanUrl

        Log.d("debug", safeBaseUrl)

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

object WeatherData {
    private var weatherState: String? = null
    private var location: String? = null

    fun editWeatherState(newWeatherState: String?) {
        weatherState = newWeatherState
    }

    fun editLocation(newLocation: String?) {
        location = newLocation
    }

    fun getWeatherState(): String? {
        return weatherState
    }

    fun getLocation(): String? {
        return location
    }
}