package com.hamster.toolbox.utils.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.hamster.toolbox.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Locale

@Composable
fun Weather() {
    var weatherData by remember { mutableStateOf<WeatherNow?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val apiKey = prefs.getString("weather_api_key", null)
    val apiHost = prefs.getString("weather_api_host", null)

    var locationQuery by remember { mutableStateOf<String?>(null) }
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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

    LaunchedEffect(apiKey, apiHost, locationQuery) {
        if (apiKey.isNullOrEmpty() || apiHost.isNullOrEmpty()) {
            isError = true
            isLoading = false
            return@LaunchedEffect
        }

        if (locationQuery == null) Log.d("debug", "no location")

        if (locationQuery == null) return@LaunchedEffect

        isLoading = true

        try {
            val response = withContext(Dispatchers.IO) {
                val api = WeatherApiClient.getApi(apiHost)
                api.getWeatherNow(locationQuery!!, apiKey)
            }

            if (response.code == "200" && response.now != null) {
                weatherData = response.now
                isError = false
            } else {
                isError = true
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
        modifier = Modifier.padding(end = 16.dp)
    ) {

        if (isLoading || isError) {
            Icon(painterResource(R.drawable.ic_weather_sun_day), null, tint = Color.Gray)
        } else {
            weatherData?.let { weather ->
                when (weather.text) {
                    "晴" -> {
                        if (LocalTime.now().hour in 6..18) {
                            Icon(painterResource(R.drawable.ic_weather_sun_day), null, tint = Color.Gray)
                        } else {
                            Icon(painterResource(R.drawable.ic_weather_moon), null, tint = Color.Gray)
                        }
                    }
                    "阴" -> {
                        if (LocalTime.now().hour in 6..18) {
                            Icon(painterResource(R.drawable.ic_weather_day_cloudy), null, tint = Color.Gray)
                        } else {
                            Icon(painterResource(R.drawable.ic_weather_night_cloudy), null, tint = Color.Gray)
                        }
                    }
                    "雨" -> Icon(painterResource(R.drawable.ic_weather_rain), null, tint = Color.Gray)
                    else -> Text(text = weather.text, fontSize = 18.sp)
                }

                Text(
                    text = "${weather.temp}°C",
                    fontSize = 18.sp,
                )
            }
        }
    }
}