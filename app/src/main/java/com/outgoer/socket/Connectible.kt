package com.outgoer.socket

import com.outgoer.api.chat.model.*
import com.outgoer.api.live.model.*
import io.reactivex.Completable
import io.reactivex.Observable

interface Connectible {
    val isConnected: Boolean
    fun connect()
    fun connectionEmitter(): Observable<Unit>
    fun connectionError(): Observable<Unit>
    fun disconnect()
    fun disconnectEmitter(): Observable<Unit>
    fun observeRoomJoined(): Observable<JoinRoomRequest>
    fun observeNewMessage(): Observable<ChatMessageInfo>
    fun sendMessage(chatSendMessageRequest: ChatSendMessageRequest): Completable
    fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable
    fun leaveRoom(getMessageListRequest: GetMessageListRequest): Completable

    fun updateOnlineStatus(request: UpdateOnlineStatusRequest): Completable
    fun observeOnlineStatus(): Observable<ChatOnlineStatusResponse>

    fun sendMessageIsRead(request: SendMessageIsReadRequest): Completable
    fun observeMessageIsRead(): Observable<SendMessageIsReadRequest>

    fun setUserOffline(request: SetUserOfflineRequest): Completable
    fun setAppOnline(): Completable
    fun observeOtherNewMessages(): Observable<ChatConversationInfo>
    fun sendUserRoom(request: SetUserRoomRequest): Completable

    //Live
    fun liveRoom(request: LiveRoomRequest): Completable
    fun observeLiveWatchingCount(): Observable<LiveEventWatchingCount>
    fun sendComment(request: LiveEventSendOrReadComment): Completable
    fun observeOtherLiveComment(): Observable<LiveEventSendOrReadComment>
    fun liveEnd(request: LiveEventEndSocketEvent): Completable
    fun observeLiveEventEnd(): Observable<LiveEventEndSocketEvent>
    fun liveRoomDisconnect(request: LiveRoomDisconnectRequest): Completable
    fun liveUserKick(request: LiveEventKickUser): Completable
    fun observeLiveEventUserKick(): Observable<LiveEventKickUser>
    fun addReactions(request: AddReactionSocketEvent): Completable
    fun removeReactions(request: RemoveReactionSocketEvent): Completable
    fun sendHeart(request: SendHeartSocketEvent): Completable
    fun observeLiveHeart(): Observable<SendHeartSocketEvent>
    fun observeMessageReaction(): Observable<ChatMessageListener>

    fun typingMessage(request: MessageTypingSocketEvent): Completable

    fun observeTyping(): Observable<MessageTypingSocketEvent>
}