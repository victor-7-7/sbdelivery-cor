package ru.skillbranch.sbdelivery.data.network

import retrofit2.Response
import retrofit2.http.*
import ru.skillbranch.sbdelivery.data.network.res.CategoryRes
import ru.skillbranch.sbdelivery.data.network.res.DishRes
import ru.skillbranch.sbdelivery.data.network.res.RefreshToken
import ru.skillbranch.sbdelivery.data.network.res.Token

interface RestService {

    @POST("auth/refresh")
    suspend fun refreshToken(@Body refreshToken: RefreshToken): Token

    @GET("dishes")
    @Headers("If-Modified-Since: Mon, 1 Jun 2020 08:00:00 GMT")
    suspend fun getDishes(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<List<DishRes>>

    @GET("categories")
    @Headers("If-Modified-Since: Mon, 1 Jun 2020 08:00:00 GMT")
    suspend fun getCategories(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): List<CategoryRes>


}