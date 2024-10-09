package com.outgoer.api.friend_venue

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class FriendsVenueModule {

    @Provides
    @Singleton
    fun provideFriendsVenueRetrofitAPI(retrofit: Retrofit): FriendsVenueRetrofitAPI {
        return retrofit.create(FriendsVenueRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideFriendsVenueRepository(
        friendsVenueRetrofitAPI: FriendsVenueRetrofitAPI,
    ): FriendsVenueRepository {
        return FriendsVenueRepository(friendsVenueRetrofitAPI)
    }
}