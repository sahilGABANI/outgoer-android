package com.outgoer.api.tagged_post_reels

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class TaggedPostReelsModule {

    @Provides
    @Singleton
    fun provideTaggedPostReelsRetrofitAPI(retrofit: Retrofit): TaggedPostReelsRetrofitAPI {
        return retrofit.create(TaggedPostReelsRetrofitAPI::class.java)
    }


    @Provides
    @Singleton
    fun provideTaggedPostReelsRepository(
        taggedPostReelsRetrofitAPI: TaggedPostReelsRetrofitAPI,
    ): TaggedPostReelsRepository {
        return TaggedPostReelsRepository(taggedPostReelsRetrofitAPI)
    }
}