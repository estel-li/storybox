package net.lijue.storybox.data.api

import net.lijue.storybox.core.model.AlbumDto
import net.lijue.storybox.core.model.ApiPageResponse
import net.lijue.storybox.core.model.CategoryDto
import net.lijue.storybox.core.model.FavoriteDto
import net.lijue.storybox.core.model.HealthResponse
import net.lijue.storybox.core.model.PlayHistoryDto
import net.lijue.storybox.core.model.StoryDto
import net.lijue.storybox.core.model.UpdateHistoryRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoryBoxApi {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @GET("api/categories")
    suspend fun getCategories(): ApiPageResponse<CategoryDto>

    @GET("api/categories/{id}/albums")
    suspend fun getAlbumsByCategory(@Path("id") id: Long): ApiPageResponse<AlbumDto>

    @GET("api/albums")
    suspend fun getAlbums(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100
    ): ApiPageResponse<AlbumDto>

    @GET("api/albums/{id}")
    suspend fun getAlbum(@Path("id") id: Long): AlbumDto

    @GET("api/albums/{id}/stories")
    suspend fun getStoriesByAlbum(@Path("id") id: Long): ApiPageResponse<StoryDto>

    @GET("api/stories/{id}")
    suspend fun getStory(@Path("id") id: Long): StoryDto

    @GET("api/search")
    suspend fun search(@Query("q") keyword: String): ApiPageResponse<StoryDto>

    @GET("api/devices/{deviceId}/history")
    suspend fun getHistory(@Path("deviceId") deviceId: String): ApiPageResponse<PlayHistoryDto>

    @POST("api/devices/{deviceId}/history")
    suspend fun updateHistory(
        @Path("deviceId") deviceId: String,
        @Body body: UpdateHistoryRequest
    )

    @GET("api/devices/{deviceId}/favorites")
    suspend fun getFavorites(@Path("deviceId") deviceId: String): ApiPageResponse<FavoriteDto>

    @POST("api/devices/{deviceId}/favorites/{storyId}")
    suspend fun addFavorite(
        @Path("deviceId") deviceId: String,
        @Path("storyId") storyId: Long
    )

    @DELETE("api/devices/{deviceId}/favorites/{storyId}")
    suspend fun removeFavorite(
        @Path("deviceId") deviceId: String,
        @Path("storyId") storyId: Long
    )
}
