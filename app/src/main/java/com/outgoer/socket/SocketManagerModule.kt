package com.outgoer.socket

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.base.extension.getSocketBaseUrl
import dagger.Module
import dagger.Provides
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton


@Module
class SocketManagerModule {

    @Provides
    @Singleton
    fun provideOptions(): IO.Options {
        return IO.Options()
    }

    @Provides
    @Singleton
    @Named("SocketOkHttpClient")
    fun provideSocketOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideSocket(@Named("SocketOkHttpClient") okHttpClient: OkHttpClient, options: IO.Options): Socket {
        options.webSocketFactory = okHttpClient
        options.callFactory = okHttpClient
        return IO.socket(getSocketBaseUrl(), options)
    }

    @Provides
    @Singleton
    fun provideSocketService(socket: Socket, loggedInUserCache: LoggedInUserCache): SocketService {
        return SocketService(socket, loggedInUserCache)
    }

    @Provides
    @Singleton
    fun provideSocketDataManager(socketService: SocketService): SocketDataManager {
        return SocketDataManager(socketService)
    }
}
