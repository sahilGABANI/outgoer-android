package com.outgoer.socket

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.base.extension.onSafeNext
import io.reactivex.Completable
import io.reactivex.ObservableEmitter
import io.socket.client.Ack
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import timber.log.Timber

class SocketService(
    private val socket: Socket,
    private val loggedInUserCache: LoggedInUserCache
) {
    private var gson: Gson = GsonBuilder().create()
    val isConnected: Boolean get() = socket.connected()
    val userID: Int? get() = loggedInUserCache.getUserId()

    init {
        initSocket()
    }

    private fun initSocket() {
        socket.apply {
            io().timeout(-1)
        }
        connect()
    }

    fun connect() {
        if (!isConnected) {
            Timber.tag(SOCKET_TAG).i("Call socket connection")
            socket.connect()
        } else {
            Timber.tag(SOCKET_TAG).e("Socket is already connected :::: %s ", isConnected)
        }
    }

    fun disconnect() {
        Timber.tag(SOCKET_TAG).i("Call socket disconnect")
        socket.disconnect()
    }

    fun getGson() = gson

    companion object {
        const val SOCKET_TAG = "<><><> Socket"

        const val EVENT_CONNECT = Socket.EVENT_CONNECT
        const val EVENT_CONNECT_ERROR = Socket.EVENT_CONNECT_ERROR
        const val EVENT_DISCONNECT = Socket.EVENT_DISCONNECT

        const val EVENT_ROOM = "room"
        const val EVENT_SEND_MESSAGE = "send_message"

        const val EVENT_LEAVE_CHAT = "leave_chat"

        const val EVENT_ROOM_JOIN = "room_join"
        const val EVENT_NEW_MESSAGE = "new_message"

        const val EVENT_ONLINE_STATUS = "online"
        const val EVENT_USER_ONLINE = "global_user_online"
        const val EVENT_SET_USER_OFFLINE = "set_user_offline"

        const val EVENT_READ_MESSAGE = "read_message"
        const val EVENT_DOUBLE_READ_MESSAGE = "double_read_message"

        const val EVENT_OTHER_NEW_MESSAGES = "other_new_messages"
        const val EVENT_USER_ROOM = "user_room"

        //Live
        const val EVENT_LIVE_ROOM = "liveroom"
        const val EVENT_LIVE_ROOM_DISCONNECT = "live_disconnect"

        const val EVENT_LIVE_UPDATES = "live_updates"

        const val EVENT_LIVE_ROOM_SEND_COMMENT = "send-comment"
        const val EVENT_LIVE_ROOM_NEW_COMMENT = "live_comment"

        const val KICK_USER = "kick-user"
        const val LEAVE_KICK_USER = "leave_kick_user"

        const val EVENT_SEND_HEART = "send-heart"
        const val EVENT_LIVE_HEART = "live_heart"

        const val LIVE_END = "live_end"
        const val LIVE_EVENT_END = "live_event_end"
        const val APP_ONLINE = "app_online"

        const val TYPING = "typing"
        const val ADD_REACTION = "add-reaction"
        const val MSG_REACTION = "msg-reaction"
        const val REMOVE_REACTION = "remove-reaction"

//        const val EVENT_LIVE_REMOVE_HOST = "remove_live_host"
//        const val EVENT_REMOVE_LIVE_HOST = "remove_host_success"
//        const val LIVE_ADMIN_END = "live_admin_end"
    }

    fun request(name: String, jSONObject: JSONObject): Completable =
        if (isConnected) {
            jSONObject.put("socket_token", loggedInUserCache.getLoginUserSocketToken())
            Timber.tag(SOCKET_TAG).i("Request Event Name : $name")
            Timber.tag(SOCKET_TAG).i("Request Event RequestJson : $jSONObject")
            socket.emit(name, jSONObject)
            Completable.complete()
        } else {
            Completable.error(SocketNotConnectedException("Socket is not connected while calling $name event"))
        }


    fun on(name: String, listener: Emitter.Listener) {
        socket.on(name, listener)
    }

    fun <T> requestWithAck(name: String, jSONObject: JSONObject, emitter: ObservableEmitter<T>, clazz: Class<T>) {
        if (isConnected) {
            Timber.tag(SOCKET_TAG).i("Request Event Name : $name")
            Timber.tag(SOCKET_TAG).i("Request Event RequestJson : $jSONObject")
            socket.emit(name, jSONObject, SocketAck(name, emitter, gson, clazz)) ?: Timber.e("Socket is not init")
        } else {
            Timber.tag(SOCKET_TAG).e("Socket is not connected while calling $name event")
        }
    }
}

class SocketNotConnectedException(message: String) : Throwable(message)

class SocketAck<T>(
    private val eventName: String,
    private val emitter: ObservableEmitter<T>,
    private val gson: Gson,
    private val clazz: Class<T>,
) : Ack {
    override fun call(vararg args: Any?) {
        val response = args.firstOrNull()?.toString()
        if (response == null) {
            emitter.onError(Exception("Socket error"))
        } else {
            Timber.tag(SocketService.SOCKET_TAG).i("Request Event Name : $eventName")
            Timber.tag(SocketService.SOCKET_TAG).i("Request Event ACK Response: $response")
            emitter.onSafeNext(gson.fromJson(args[0].toString(), clazz))
        }
    }
}