package com.example.data.api

import com.example.data.models.Inbound
import com.example.data.models.LoginRequest
import com.example.data.models.ThreeXuiResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ThreeXuiApiService {

    @FormUrlEncoded
    @POST("login")
    suspend fun loginForm(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("loginSecret") loginSecret: String? = null
    ): Response<ThreeXuiResponse<Any>>

    @POST("login")
    suspend fun loginJson(
        @Body request: LoginRequest
    ): Response<ThreeXuiResponse<Any>>

    @GET("panel/api/inbounds/list")
    suspend fun getInbounds(): Response<ThreeXuiResponse<List<Inbound>>>

    @GET("panel/api/inbounds/get/{id}")
    suspend fun getInboundById(
        @Path("id") id: Int
    ): Response<ThreeXuiResponse<Inbound>>

    @GET("panel/api/inbounds/getClientTraffics/{email}")
    suspend fun getClientTraffics(
        @Path("email") email: String
    ): Response<ResponseBody>

    @POST("panel/api/inbounds/resetAllTraffics")
    suspend fun resetAllTraffics(): Response<ThreeXuiResponse<Any>>
}
