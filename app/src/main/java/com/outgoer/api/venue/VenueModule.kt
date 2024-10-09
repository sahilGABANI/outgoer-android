package com.outgoer.api.venue

import com.outgoer.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class VenueModule {

    @Provides
    @Singleton
    fun provideVenueRetrofitAPI(retrofit: Retrofit): VenueRetrofitAPI {
        return retrofit.create(VenueRetrofitAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideVenueRepository(
        venueRetrofitAPI: VenueRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): VenueRepository {
        return VenueRepository(
            venueRetrofitAPI,
            loggedInUserCache
        )
    }
}