package com.example.mcamp25.readstack.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BookApiService {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 40
    ): BookResponse

    @GET("volumes/{id}")
    suspend fun getBook(
        @Path("id") id: String
    ): BookItem
}
