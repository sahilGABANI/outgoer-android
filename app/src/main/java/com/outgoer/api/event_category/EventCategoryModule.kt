package com.outgoer.api.event_category


import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class EventCategoryModule {

    @Provides
    @Singleton
    fun provideEventsRetrofitAPI(retrofit: Retrofit): EventCategoryRetrofitAPI {
        return retrofit.create(EventCategoryRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideEventsRepository(
        eventCategoryRetrofitAPI: EventCategoryRetrofitAPI,
    ): EventCategoryRepository {
        return EventCategoryRepository(eventCategoryRetrofitAPI)
    }
}