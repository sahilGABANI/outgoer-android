package com.outgoer.api.profile

import com.outgoer.api.authentication.LoggedInUserCache
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ProfileModule {

    @Provides
    @Singleton
    fun provideProfileRetrofitAPI(retrofit: Retrofit): ProfileRetrofitAPI {
        return retrofit.create(ProfileRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideProfileRepository(
        profileRetrofitAPI: ProfileRetrofitAPI,
        loggedInUserCache: LoggedInUserCache
    ): ProfileRepository {
        return ProfileRepository(profileRetrofitAPI, loggedInUserCache)
    }
}