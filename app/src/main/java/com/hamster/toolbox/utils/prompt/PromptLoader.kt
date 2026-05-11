package com.hamster.toolbox.utils.prompt

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PromptLoader {
    fun loadPrompts(context: Context): List<PromptItem> {
        val jsonString = context.assets.open("prompts.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<PromptItem>>() {}.type
        return Gson().fromJson(jsonString, type)
    }

    fun getPromptById(context: Context, id: String): String {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return "Current system time is: $currentTime\n" +loadPrompts(context).find { it.id == id }?.content + "\nUser Input:"
    }
}