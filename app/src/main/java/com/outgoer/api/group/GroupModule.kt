package com.outgoer.api.group

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class GroupModule {

    @Provides
    @Singleton
    fun provideGroupRetrofitAPI(retrofit: Retrofit): GroupRetrofitAPI {
        return retrofit.create(GroupRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideGroupRepository(
        groupRetrofitAPI: GroupRetrofitAPI,
    ): GroupRepository {
        return GroupRepository(groupRetrofitAPI)
    }
}