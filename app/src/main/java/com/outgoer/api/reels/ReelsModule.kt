package com.outgoer.api.reels

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ReelsModule {

    @Provides
    @Singleton
    fun provideReelsRetrofitAPI(retrofit: Retrofit): ReelsRetrofitAPI {
        return retrofit.create(ReelsRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideReelsRepository(
        reelsRetrofitAPI: ReelsRetrofitAPI,
    ): ReelsRepository {
        return ReelsRepository(reelsRetrofitAPI)
    }
}