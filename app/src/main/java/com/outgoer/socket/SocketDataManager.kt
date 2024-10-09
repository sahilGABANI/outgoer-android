package com.outgoer.socket

import com.outgoer.api.chat.model.*
import com.outgoer.api.live.model.*
import io.reactivex.Completable
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber

class SocketDataManager(private val appSocket: SocketService) : Connectible {

    private val connectionEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_CONNECT) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_CONNECT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Socket connected $it")

                if(appSocket.userID ?: 0 > 0) {
                    setAppOnline()
                }
                emitter.onNext(Unit)
            }
        }.share()
    }

    private val connectionErrorEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_CONNECT_ERROR) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_CONNECT_ERROR}")
                Timber.tag(SocketService.SOCKET_TAG).e("ON Error ${it.get(0)}")
                emitter.onNext(Unit)
            }
        }.share()
    }

    private val disconnectEmitter by lazy {
        Observable.create<Unit> { emitter ->
            appSocket.on(SocketService.EVENT_DISCONNECT) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_DISCONNECT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Socket disconnected $it")
                emitter.onNext(Unit)
            }
        }.share()
    }

    private val roomJoinedEmitter by lazy {
        Observable.create<JoinRoomRequest> { emitter ->
            appSocket.on(SocketService.EVENT_ROOM_JOIN) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_ROOM_JOIN}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(JoinRoomRequest::class.java))
            }
        }.share()
    }

    private val newMessageEmitter by lazy {
        Observable.create<ChatMessageInfo> { emitter ->
            appSocket.on(SocketService.EVENT_NEW_MESSAGE) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_NEW_MESSAGE}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(ChatMessageInfo::class.java))
            }
        }.share()
    }

    private val onlineStatusEmitter by lazy {
        Observable.create<ChatOnlineStatusResponse> { emitter ->
            appSocket.on(SocketService.EVENT_USER_ONLINE) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_USER_ONLINE}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(ChatOnlineStatusResponse::class.java))
            }
        }.share()
    }

    private val messageIsReadEmitter by lazy {
        Observable.create<SendMessageIsReadRequest> { emitter ->
            appSocket.on(SocketService.EVENT_DOUBLE_READ_MESSAGE) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_DOUBLE_READ_MESSAGE}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendMessageIsReadRequest::class.java))
            }
        }.share()
    }

    private val otherNewMessagesEmitter by lazy {
        Observable.create<ChatConversationInfo> { emitter ->
            appSocket.on(SocketService.EVENT_OTHER_NEW_MESSAGES) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_OTHER_NEW_MESSAGES}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(ChatConversationInfo::class.java))
            }
        }.share()
    }

    private val liveWatchingCountEmitter by lazy {
        Observable.create<LiveEventWatchingCount> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_UPDATES) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_UPDATES}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventWatchingCount::class.java))
            }
        }.share()
    }

    private val otherLiveCommentEmitter by lazy {
        Observable.create<LiveEventSendOrReadComment> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_ROOM_NEW_COMMENT) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_ROOM_NEW_COMMENT}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventSendOrReadComment::class.java))
            }
        }.share()
    }

    private val liveEventEndEmitter by lazy {
        Observable.create<LiveEventEndSocketEvent> { emitter ->
            appSocket.on(SocketService.LIVE_EVENT_END) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.LIVE_EVENT_END}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventEndSocketEvent::class.java))
            }
        }.share()
    }

    private val liveEventKickUserEmitter by lazy {
        Observable.create<LiveEventKickUser> { emitter ->
            appSocket.on(SocketService.LEAVE_KICK_USER) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.LEAVE_KICK_USER}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(LiveEventKickUser::class.java))
            }
        }.share()
    }

    private val liveHeartEmitter by lazy {
        Observable.create<SendHeartSocketEvent> { emitter ->
            appSocket.on(SocketService.EVENT_LIVE_HEART) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_HEART}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(SendHeartSocketEvent::class.java))
            }
        }.share()
    }

    private val messageReactionEmitter by lazy {
        Observable.create<ChatMessageListener> { emitter ->
            appSocket.on(SocketService.MSG_REACTION) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.MSG_REACTION}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                val response = it.getResponse(ChatMessageListener::class.java)
                emitter.onNext(response)
            }
        }.share()
    }

    private val messageTypingEmitter by lazy {
        Observable.create<MessageTypingSocketEvent> { emitter ->
            appSocket.on(SocketService.TYPING) {
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Name : ${SocketService.EVENT_LIVE_HEART}")
                Timber.tag(SocketService.SOCKET_TAG).i("ON Event Response ${it[0]}")
                emitter.onNext(it.getResponse(MessageTypingSocketEvent::class.java))
            }
        }.share()
    }


    private fun String.toJsonObject() = JSONObject(this)

    private fun <T> Array<Any>.getResponse(clazz: Class<T>): T {
        return appSocket.getGson().fromJson(this[0].toString(), clazz)
    }

    override val isConnected: Boolean get() = appSocket.isConnected
    override fun connect() = appSocket.connect()
    override fun connectionEmitter(): Observable<Unit> = connectionEmitter
    override fun connectionError(): Observable<Unit> = connectionErrorEmitter
    override fun disconnect() = appSocket.disconnect()
    override fun disconnectEmitter(): Observable<Unit> = disconnectEmitter
    override fun observeRoomJoined(): Observable<JoinRoomRequest> = roomJoinedEmitter
    override fun observeNewMessage(): Observable<ChatMessageInfo> = newMessageEmitter

    override fun observeOnlineStatus(): Observable<ChatOnlineStatusResponse> = onlineStatusEmitter

    override fun observeMessageIsRead(): Observable<SendMessageIsReadRequest> = messageIsReadEmitter

    override fun observeMessageReaction(): Observable<ChatMessageListener> = messageReactionEmitter

    override fun observeTyping(): Observable<MessageTypingSocketEvent> = messageTypingEmitter

    override fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable {
        return appSocket.request(SocketService.EVENT_ROOM, appSocket.getGson().toJson(joinRoomRequest).toJsonObject())
    }

    override fun leaveRoom(getMessageListRequest: GetMessageListRequest): Completable {
        return appSocket.request(SocketService.EVENT_LEAVE_CHAT, appSocket.getGson().toJson(getMessageListRequest).toJsonObject())
    }

    override fun sendMessage(chatSendMessageRequest: ChatSendMessageRequest): Completable {
        return appSocket.request(SocketService.EVENT_SEND_MESSAGE, appSocket.getGson().toJson(chatSendMessageRequest).toJsonObject())
    }

    override fun updateOnlineStatus(request: UpdateOnlineStatusRequest): Completable {
        return appSocket.request(SocketService.EVENT_ONLINE_STATUS, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun sendMessageIsRead(request: SendMessageIsReadRequest): Completable {
        return appSocket.request(SocketService.EVENT_READ_MESSAGE, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun setUserOffline(request: SetUserOfflineRequest): Completable {
        return appSocket.request(SocketService.EVENT_SET_USER_OFFLINE, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun setAppOnline(): Completable {
        return appSocket.request(SocketService.APP_ONLINE, appSocket.getGson().toJson(appSocket.userID?.let { SetAppOnlineRequest(it) }).toJsonObject())
    }


    override fun observeOtherNewMessages(): Observable<ChatConversationInfo> = otherNewMessagesEmitter

    override fun sendUserRoom(request: SetUserRoomRequest): Completable {
        return appSocket.request(SocketService.EVENT_USER_ROOM, appSocket.getGson().toJson(request).toJsonObject())
    }

    //Live
    override fun liveRoom(request: LiveRoomRequest): Completable {
        return appSocket.request(SocketService.EVENT_LIVE_ROOM, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun observeLiveWatchingCount(): Observable<LiveEventWatchingCount> = liveWatchingCountEmitter

    override fun sendComment(request: LiveEventSendOrReadComment): Completable {
        return appSocket.request(SocketService.EVENT_LIVE_ROOM_SEND_COMMENT, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun observeOtherLiveComment(): Observable<LiveEventSendOrReadComment> = otherLiveCommentEmitter

    override fun liveEnd(request: LiveEventEndSocketEvent): Completable {
        return appSocket.request(SocketService.LIVE_END, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun observeLiveEventEnd(): Observable<LiveEventEndSocketEvent> = liveEventEndEmitter

    override fun liveRoomDisconnect(request: LiveRoomDisconnectRequest): Completable {
        return appSocket.request(SocketService.EVENT_LIVE_ROOM_DISCONNECT, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun liveUserKick(request: LiveEventKickUser): Completable {
        return appSocket.request(SocketService.KICK_USER, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun observeLiveEventUserKick(): Observable<LiveEventKickUser> = liveEventKickUserEmitter

    override fun sendHeart(request: SendHeartSocketEvent): Completable {
        return appSocket.request(SocketService.EVENT_SEND_HEART, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun addReactions(request: AddReactionSocketEvent): Completable {
        return appSocket.request(SocketService.ADD_REACTION, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun removeReactions(request: RemoveReactionSocketEvent): Completable {
        return appSocket.request(SocketService.REMOVE_REACTION, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun typingMessage(request: MessageTypingSocketEvent): Completable {
        return appSocket.request(SocketService.TYPING, appSocket.getGson().toJson(request).toJsonObject())
    }

    override fun observeLiveHeart(): Observable<SendHeartSocketEvent> = liveHeartEmitter
}