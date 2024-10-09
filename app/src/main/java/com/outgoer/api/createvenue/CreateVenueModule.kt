package com.outgoer.api.createvenue

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class CreateVenueModule {

    @Provides
    @Singleton
    fun provideVenueRetrofitAPI(retrofit: Retrofit): CreateVenueRetrofitAPI {
        return retrofit.create(CreateVenueRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideVenueRepository(
        createVenueRetrofitAPI: CreateVenueRetrofitAPI
    ): CreateVenueRepository {
        return CreateVenueRepository(
            createVenueRetrofitAPI
        )
    }
}