package com.outgoer.api.event

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class EventModule {

    @Provides
    @Singleton
    fun provideEventsRetrofitAPI(retrofit: Retrofit): EventRetrofitAPI {
        return retrofit.create(EventRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideEventsRepository(
            eventRetrofitAPI: EventRetrofitAPI,
    ): EventRepository {
        return EventRepository(eventRetrofitAPI)
    }
}