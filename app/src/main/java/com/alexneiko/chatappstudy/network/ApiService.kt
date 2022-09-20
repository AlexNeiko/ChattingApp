package com.alexneiko.chatappstudy.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ApiService {

    @POST("send")
    fun sendMessage(@HeaderMap headers: HashMap<String, String>, @Body messageBody: String): Call<String>
}