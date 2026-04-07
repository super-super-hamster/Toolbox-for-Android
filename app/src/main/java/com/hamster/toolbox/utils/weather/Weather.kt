package com.hamster.toolbox.utils.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.hamster.toolbox.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun Weather(
    onClick: () -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherNow?>(null) }
    var currentCity by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    val gson = remember { Gson() }

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val apiKey = prefs.getString("weather_api_key", null)
    val apiHost = prefs.getString("weather_api_host", null)

    var locationQuery by remember { mutableStateOf<String?>(null) }
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // 刷新触发器
    var refreshTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            val cacheExpirationMs = 300000L
            delay(cacheExpirationMs + 1000L)

            refreshTrigger++
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            locationQuery = if (location != null) {
                String.format(Locale.US, "%.2f,%.2f", location.longitude, location.latitude)
            } else {
                null
            }
        } else {
            isError = true
        }
    }

    // 和风天气要求中国大陆地区使用GCJ-02坐标系，此处使用的是WGS-84坐标系，最大可能会有几百米的误差,影响不大
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            locationQuery = if (location != null) {
                String.format(Locale.US, "%.2f,%.2f", location.longitude, location.latitude)
            } else {
                null
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    LaunchedEffect(apiKey, apiHost, locationQuery, refreshTrigger) {
        if (apiKey.isNullOrEmpty() || apiHost.isNullOrEmpty()) {
            isError = true
            isLoading = false
            return@LaunchedEffect
        }

        if (locationQuery == null) {
            return@LaunchedEffect
        }

        isLoading = true

        val cacheExpirationMs = 300000L // 缓存有效期5分钟
        val lastFetchTime = prefs.getLong("weather_last_fetch_time", 0L)
        val currentTime = System.currentTimeMillis()

        // 尝试使用缓存
        if (currentTime - lastFetchTime < cacheExpirationMs) {
            val cachedWeatherJson = prefs.getString("weather_cached_data", null)
            val cachedCity = prefs.getString("weather_cached_city", null)

            if (cachedWeatherJson != null) {
                weatherData = gson.fromJson(cachedWeatherJson, WeatherNow::class.java)
                currentCity = cachedCity ?: ""

                WeatherData.editWeatherState(weatherData?.text)
                WeatherData.editLocation(currentCity)

                isError = false
                isLoading = false
                return@LaunchedEffect
            }
        }

        try {
            withContext(Dispatchers.IO) {
                val weatherApi = WeatherApiClient.getApi(apiHost)
                val weatherResp = weatherApi.getWeatherNow(locationQuery!!, apiKey)

                val geoApi = WeatherApiClient.getApi(apiHost)
                val geoResp = geoApi.getCityInfo(locationQuery!!, apiKey)

                withContext(Dispatchers.Main) {
                    if (weatherResp.code == "200" && weatherResp.now != null) {
                        weatherData = weatherResp.now
                        WeatherData.editWeatherState(weatherData?.text)
                        isError = false
                    } else {
                        isError = true
                    }

                    if (geoResp.code == "200" && !geoResp.location.isNullOrEmpty()) {
                        val loc = geoResp.location[0]
                        currentCity = if (loc.adm2 == loc.name) {
                            loc.name
                        } else {
                            "${loc.adm2}-${loc.name}"
                        }
                        WeatherData.editLocation(currentCity)
                    }

                    prefs.edit {
                        putLong("weather_last_fetch_time", System.currentTimeMillis())
                            .putString("weather_cached_data", gson.toJson(weatherResp.now))
                            .putString("weather_cached_city", currentCity)
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isError = true
        } finally {
            isLoading = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(end = 20.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick() }
            )
    ) {
        val content = if (isLoading || isError) {
            Pair(R.drawable.ic_weather_152, "--°C")
        } else {
            weatherData?.let { getQWeatherIconResId(context, it.icon) to "${it.temp}°C" }
        }

        content?.let { (iconRes, tempText) ->

            Box(
                modifier = Modifier.weight(0.6f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxHeight(0.8f)
                )
            }

            Box(
                modifier = Modifier.weight(0.4f),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = tempText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
fun getQWeatherIconResId(context: Context, iconCode: String): Int {
    val resName = "ic_weather_$iconCode"

    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

    return if (resId != 0) resId else R.drawable.ic_weather_100
}