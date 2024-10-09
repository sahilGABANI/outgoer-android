package com.outgoer.di

import android.app.Application
import android.content.Context
import com.outgoer.api.authentication.AuthenticationModule
import com.outgoer.api.cloudflare.CloudFlareModule
import com.outgoer.api.chat.ChatMessageModule
import com.outgoer.api.createvenue.CreateVenueModule
import com.outgoer.api.event.EventModule
import com.outgoer.api.event_category.EventCategoryModule
import com.outgoer.api.follow.FollowUserModule
import com.outgoer.api.friend_venue.FriendsVenueModule
import com.outgoer.api.group.GroupModule
import com.outgoer.api.hashtag.HashtagModule
import com.outgoer.api.live.LiveModule
import com.outgoer.api.music.MusicModule
import com.outgoer.api.mutual.MutualModule
import com.outgoer.api.notification.NotificationModule
import com.outgoer.api.post.PostModule
import com.outgoer.api.profile.ProfileModule
import com.outgoer.api.reels.ReelsModule
import com.outgoer.api.search.SearchModule
import com.outgoer.api.sponty.SpontyModule
import com.outgoer.api.story.StoryModule
import com.outgoer.api.tagged_post_reels.TaggedPostReelsModule
import com.outgoer.api.venue.VenueModule
import com.outgoer.api.viewmodelmodule.OutgoerViewModelProvider
import com.outgoer.application.Outgoer
import com.outgoer.base.network.NetworkModule
import com.outgoer.base.prefs.PrefsModule
import com.outgoer.socket.SocketManagerModule
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class OutgoerAppModule(val app: Application) {
    @Provides
    @Singleton
    fun provideApplication(): Application {
        return app
    }

    @Provides
    @Singleton
    fun provideContext(): Context {
        return app
    }
}

@Singleton
@Component(
    modules = [
        OutgoerAppModule::class,
        NetworkModule::class,
        PrefsModule::class,
        AuthenticationModule::class,
        PostModule::class,
        ProfileModule::class,
        FollowUserModule::class,
        OutgoerViewModelProvider::class,
        ReelsModule::class,
        CloudFlareModule::class,
        ChatMessageModule::class,
        SocketManagerModule::class,
        NotificationModule::class,
        VenueModule::class,
        SearchModule::class,
        LiveModule::class,
        SpontyModule::class,
        EventModule::class,
        GroupModule::class,
        CreateVenueModule::class,
        MutualModule::class,
        EventCategoryModule::class,
        FriendsVenueModule::class,
        TaggedPostReelsModule::class,
        HashtagModule::class,
        MusicModule::class,
        StoryModule::class
    ]
)

interface OutgoerAppComponent : BaseAppComponent {
    fun inject(app: Outgoer)
}