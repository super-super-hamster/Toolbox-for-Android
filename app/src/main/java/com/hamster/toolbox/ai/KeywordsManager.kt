package com.hamster.toolbox.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object KeywordManager {
    private const val TARGET_FILE_NAME = "custom_keywords.json"
    private const val ASSETS_FILE_NAME = "keywords.json"
    private val gson = Gson()

    var isNew = false

    fun loadCustomKeywords(context: Context): MutableList<KeywordsData> {
        val targetFile = File(context.filesDir, TARGET_FILE_NAME)

        if (!targetFile.exists()) {
            try {
                targetFile.writeText("[]")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return try {
            val targetJsonString = targetFile.readText()
            val listType = object : TypeToken<List<KeywordsData>>() {}.type
            gson.fromJson(targetJsonString, listType) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    private fun loadAssetsKeywords(context: Context): MutableList<KeywordsData> {
        val jsonString = context.assets.open(ASSETS_FILE_NAME).bufferedReader().use {
            it.readText()
        }

        return try {
            val listType = object : TypeToken<List<KeywordsData>>() {}.type
            gson.fromJson(jsonString, listType) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun loadAllKeywords(context: Context): MutableList<KeywordsData> {
        val resultList = loadAssetsKeywords(context)
        resultList.addAll(loadCustomKeywords(context))
        return resultList
    }

    fun addKeyword(context: Context, data: KeywordsData) {
        val list = loadCustomKeywords(context)

        if (list.none { it.word == data.word }) {
            list.add(data)
            saveList(context, list)
        }

        isNew = false
    }

    fun removeKeyword(context: Context, word: String) {
        val list = loadCustomKeywords(context)
        val removed = list.removeAll { it.word == word }
        if (removed) {
            saveList(context, list)
        }

        isNew = false
    }

    private fun saveList(context: Context, list: List<KeywordsData>) {
        try {
            val file = File(context.filesDir, TARGET_FILE_NAME)
            file.writeText(gson.toJson(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}