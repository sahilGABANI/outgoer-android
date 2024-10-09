package com.outgoer.api.mutual

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class MutualModule {

    @Provides
    @Singleton
    fun provideMutualUserRetrofitAPI(retrofit: Retrofit): MutualRetrofitAPI {
        return retrofit.create(MutualRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideMutualUserRepository(
        mutualRetrofitAPI: MutualRetrofitAPI,
    ): MutualRepository {
        return MutualRepository(mutualRetrofitAPI)
    }
}