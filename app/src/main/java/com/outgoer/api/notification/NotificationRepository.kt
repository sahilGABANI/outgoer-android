package com.outgoer.api.notification

import com.outgoer.api.notification.model.NotificationInfo
import com.outgoer.api.notification.model.UpdateNotificationReadStatus
import com.outgoer.api.notification.model.UpdateNotificationReadStatusRequest
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class NotificationRepository(
    private val notificationRetrofitAPI: NotificationRetrofitAPI,
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getAllNotification(pageNo: Int): Single<OutgoerResponse<List<NotificationInfo>>?> {
        return notificationRetrofitAPI.getAllNotification(pageNo)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAdminNotification(pageNo: Int): Single<OutgoerResponse<List<NotificationInfo>>?> {
        return notificationRetrofitAPI.getAdminNotification(pageNo)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }


    fun updateNotificationReadStatus(notificationId: Int): Single<OutgoerResponse<UpdateNotificationReadStatus>> {
        return notificationRetrofitAPI.updateNotificationReadStatus(UpdateNotificationReadStatusRequest(listOf(notificationId)))
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
}