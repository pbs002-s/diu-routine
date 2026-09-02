package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class DiuDepartment(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class DiuBatch(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "departmentId") val departmentId: String? = null
)

@JsonClass(generateAdapter = true)
data class DiuSection(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "batchId") val batchId: String? = null,
    @Json(name = "departmentId") val departmentId: String? = null
)

@JsonClass(generateAdapter = true)
data class DiuRoutineItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "weekday") val weekday: String? = null,
    @Json(name = "weekday_index") val weekdayIndex: Int? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "courseCode") val courseCode: String? = null,
    @Json(name = "courseTitle") val courseTitle: String? = null,
    @Json(name = "teacherName") val teacherName: String? = null,
    @Json(name = "teacherInitials") val teacherInitials: String? = null,
    @Json(name = "room") val room: String? = null,
    @Json(name = "building") val building: String? = null,
    @Json(name = "department") val department: String? = null,
    @Json(name = "batch") val batch: String? = null,
    @Json(name = "section") val section: String? = null,
    @Json(name = "sectionTag") val sectionTag: String? = null
)

@JsonClass(generateAdapter = true)
data class DiuServerStatus(
    @Json(name = "live_server") val liveServer: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "last_sync") val lastSync: Long? = null,
    @Json(name = "entries_count") val entriesCount: Int? = null,
    @Json(name = "is_syncing") val isSyncing: Boolean? = null
)

interface DiuLiveRoutineApi {
    @GET("api/status")
    suspend fun getStatus(): DiuServerStatus

    @GET("api/departments")
    suspend fun getDepartments(): List<DiuDepartment>

    @GET("api/batches")
    suspend fun getBatches(@Query("dept") dept: String? = null): List<DiuBatch>

    @GET("api/sections")
    suspend fun getSections(
        @Query("dept") dept: String? = null,
        @Query("batch") batch: String? = null
    ): List<DiuSection>

    @GET("api/routine")
    suspend fun getRoutine(
        @Query("dept") dept: String,
        @Query("batch") batch: String,
        @Query("section") section: String
    ): List<DiuRoutineItem>

    @POST("api/sync")
    suspend fun triggerSync(): DiuServerStatus
}

object DiuLiveClientProvider {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun createClient(baseUrl: String): DiuLiveRoutineApi {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        return retrofit.create(DiuLiveRoutineApi::class.java)
    }
}
