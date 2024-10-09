package com.outgoer.api.chat

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.socket.SocketDataManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class ChatMessageRepository(
    private val chatMessageRetrofitAPI: ChatMessageRetrofitAPI,
    private val socketDataManager: SocketDataManager,
    private val loggedInUserCache: LoggedInUserCache,
) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getConversationId(conversationRequest: ConversationRequest): Single<OutgoerCommonResponse> {
        return chatMessageRetrofitAPI.getConversationId(conversationRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun forwardMessage(msgId: Int, receiverIds: String): Single<OutgoerCommonResponse> {
        return chatMessageRetrofitAPI.forwardMessage(msgId, receiverIds).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun joinRoom(joinRoomRequest: JoinRoomRequest): Completable {
        return socketDataManager.joinRoom(joinRoomRequest)
    }

    fun leaveRoom(getMessageListRequest: GetMessageListRequest): Completable {
        return socketDataManager.leaveRoom(getMessageListRequest)
    }

    fun observeRoomJoined(): Observable<JoinRoomRequest> {
        return socketDataManager.observeRoomJoined()
    }

    fun observeNewMessage(): Observable<ChatMessageInfo> {
        return socketDataManager.observeNewMessage()
    }

    fun sendMessage(chatSendMessageRequest: ChatSendMessageRequest): Completable {
        return socketDataManager.sendMessage(chatSendMessageRequest)
    }

    fun getChatMessageList(pageNo: Int, getMessageListRequest: GetMessageListRequest): Single<List<ChatMessageInfo>> {
        return chatMessageRetrofitAPI.getChatMessageList(pageNo, getMessageListRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getChatReactionList(pageNo: Int, messageId: Int): Single<List<Reaction>> {
        return chatMessageRetrofitAPI.getChatReactionList(pageNo, messageId).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getChatConversationList(getConversationListRequest: GetConversationListRequest): Single<List<ChatConversationInfo>> {
        return chatMessageRetrofitAPI.getChatConversationList(getConversationListRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getGroupChatConversationList(getConversationListRequest: GetConversationListRequest): Single<List<ChatConversationInfo>> {
        return chatMessageRetrofitAPI.getGroupChatConversationList(getConversationListRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun deleteChatConversation(id: Int): Single<OutgoerCommonResponse> {
        return chatMessageRetrofitAPI.deleteChatConversation(id).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun deleteChatMessage(id: Int): Single<OutgoerCommonResponse> {
        return chatMessageRetrofitAPI.deleteChatMessage(id).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun deleteChatGroup(id: Int): Single<OutgoerCommonResponse> {
        return chatMessageRetrofitAPI.deleteChatGroup(id).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun updateOnlineStatus(request: UpdateOnlineStatusRequest): Completable {
        return socketDataManager.updateOnlineStatus(request)
    }

    fun observeOnlineStatus(): Observable<ChatOnlineStatusResponse> {
        return socketDataManager.observeOnlineStatus()
    }

    fun sendMessageIsRead(request: SendMessageIsReadRequest): Completable {
        return socketDataManager.sendMessageIsRead(request)
    }

    fun observeMessageIsRead(): Observable<SendMessageIsReadRequest> {
        return socketDataManager.observeMessageIsRead()
    }

    fun setUserOffline(request: SetUserOfflineRequest): Completable {
        return socketDataManager.setUserOffline(request)
    }

    fun observeOtherNewMessages(): Observable<ChatConversationInfo> {
        return socketDataManager.observeOtherNewMessages()
    }

    fun observeConnection(): Observable<Unit> {
        return socketDataManager.connectionEmitter()
    }

    fun sendUserRoom(): Completable {
        return socketDataManager.sendUserRoom(SetUserRoomRequest(loggedInUserCache.getUserId() ?: 0))
    }


    fun addReactions(addReactionSocketEvent: AddReactionSocketEvent): Completable {
        return socketDataManager.addReactions(addReactionSocketEvent)
    }

    fun observeMessageReaction(): Observable<ChatMessageListener> {
        return socketDataManager.observeMessageReaction()
    }

    fun observeTyping(): Observable<MessageTypingSocketEvent> {
        return socketDataManager.observeTyping()
    }


    fun removeReactions(removeReactionSocketEvent: RemoveReactionSocketEvent): Completable {
        return socketDataManager.removeReactions(removeReactionSocketEvent)
    }

    fun typingMessage(request: MessageTypingSocketEvent): Completable {
        return socketDataManager.typingMessage(request)
    }

}