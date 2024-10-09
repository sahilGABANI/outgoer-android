package com.outgoer.api.chat

import com.outgoer.api.chat.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.*

interface ChatMessageRetrofitAPI {

    @POST("conversation/get_conversation_id")
    fun getConversationId(@Body conversationRequest: ConversationRequest): Single<OutgoerCommonResponse>

    @POST("conversation/get_message_conversation")
    fun getChatMessageList(
        @Query("page") pageNo: Int,
        @Body getMessageListRequest: GetMessageListRequest,
    ): Single<OutgoerResponse<List<ChatMessageInfo>>>

    @POST("conversation/get_message_reaction")
    fun getChatReactionList(
        @Query("page") pageNo: Int,
        @Query("message_id") messageId: Int
    ): Single<OutgoerResponse<List<Reaction>>>

    @POST("conversation/get_conversation_user_list")
    fun getChatConversationList(@Body getConversationListRequest: GetConversationListRequest): Single<OutgoerResponse<List<ChatConversationInfo>>>

    @POST("conversation/get_conversation_user_group_list")
    fun getGroupChatConversationList(@Body getConversationListRequest: GetConversationListRequest): Single<OutgoerResponse<List<ChatConversationInfo>>>

    @DELETE("conversation/delete_conversation/{id}")
    fun deleteChatConversation(@Path("id") id: Int): Single<OutgoerCommonResponse>

    @DELETE("conversation/delete_message/{id}")
    fun deleteChatMessage(@Path("id") id: Int): Single<OutgoerCommonResponse>

    @DELETE("conversation/delete_group/{id}")
    fun deleteChatGroup(@Path("id") id: Int): Single<OutgoerCommonResponse>

    @POST("conversation/forward")
    fun forwardMessage(
        @Query("id") id: Int,
        @Query("forward_ids") forwardIds: String
    ): Single<OutgoerCommonResponse>

}