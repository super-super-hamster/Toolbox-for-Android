package com.hamster.toolbox.ai

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AiService {

    //POST注解表示当调用该函数时向给定的路径发送一个HTTP POST请求
    //suspend关键字表明这是一个挂起函数，意味着它可以在不阻塞当前线程的情况下执行
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        //将apiKey作为HTTP Header中的Authorization字段
        @Header("Authorization") apiKey: String,
        //将Request对象序列化为JSON字符串并将其作为HTTP Body发送
        @Body request: Request
    ): Response

    @GET("user/balance")
    suspend fun getBalance(
        @Header("Authorization") apiKey: String
    ): BalanceResponse

    //companion object的大括号中包含的是静态的成员和方法
    companion object {
        //const val在编译时就确定下来
        private const val BASE_URL = "https://api.deepseek.com/"

        private val client = OkHttpClient.Builder()
            // 连接超时：与服务器建立连接的时间
            .connectTimeout(30, TimeUnit.SECONDS)
            // 读取超时：服务器生成并返回数据的时间
            .readTimeout(60, TimeUnit.SECONDS)
            // 写入超时：发送数据给服务器的时间
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        //创建了一个AiService接口的单例实例
        //by lazy是一个委托属性，意味着service对象只有在第一次访问时才会被创建
        val service: AiService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()

            retrofit.create(AiService::class.java)
        }
    }
}