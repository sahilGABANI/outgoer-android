package com.outgoer.api.notification

import com.outgoer.api.notification.model.NotificationInfo
import com.outgoer.api.notification.model.UpdateNotificationReadStatus
import com.outgoer.api.notification.model.UpdateNotificationReadStatusRequest
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationRetrofitAPI {

    @GET("users/notification")
    fun getAllNotification(@Query("page") pageNo: Int): Single<OutgoerResponse<List<NotificationInfo>>?>

    @GET("users/admin-notification")
    fun getAdminNotification(@Query("page") pageNo: Int): Single<OutgoerResponse<List<NotificationInfo>>?>

    @POST("users/read-notification")
    fun updateNotificationReadStatus(@Body request: UpdateNotificationReadStatusRequest): Single<OutgoerResponse<UpdateNotificationReadStatus>>
}