package com.outgoer.api.follow


import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class FollowUserModule {

    @Provides
    @Singleton
    fun provideFollowUserRetrofitAPI(retrofit: Retrofit): FollowUserRetrofitAPI {
        return retrofit.create(FollowUserRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideFollowUserRepository(
        followUserRetrofitAPI: FollowUserRetrofitAPI,
    ): FollowUserRepository {
        return FollowUserRepository(followUserRetrofitAPI)
    }
}