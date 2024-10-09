package com.outgoer.api.chat

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.socket.SocketDataManager
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ChatMessageModule {

    @Provides
    @Singleton
    fun provideChatMessageRetrofitAPI(retrofit: Retrofit): ChatMessageRetrofitAPI {
        return retrofit.create(ChatMessageRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideChatMessageRepository(
        chatMessageRetrofitAPI: ChatMessageRetrofitAPI,
        socketDataManager: SocketDataManager,
        loggedInUserCache: LoggedInUserCache
    ): ChatMessageRepository {
        return ChatMessageRepository(
            chatMessageRetrofitAPI,
            socketDataManager,
            loggedInUserCache
        )
    }
}