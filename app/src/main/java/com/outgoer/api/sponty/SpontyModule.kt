package com.outgoer.api.sponty

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class SpontyModule {

    @Provides
    @Singleton
    fun provideSpontyRetrofitAPI(retrofit: Retrofit): SpontyRetrofitAPI {
        return retrofit.create(SpontyRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideSpontyRepository(
        spontyRetrofitAPI: SpontyRetrofitAPI
    ): SpontyRepository {
        return SpontyRepository(spontyRetrofitAPI)
    }
}