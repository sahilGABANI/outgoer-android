package com.outgoer.api.viewmodelmodule

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.ChatMessageRepository
import com.outgoer.api.cloudflare.CloudFlareRepository
import com.outgoer.api.createvenue.CreateVenueRepository
import com.outgoer.api.event.EventRepository
import com.outgoer.api.event_category.EventCategoryRepository
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.friend_venue.FriendsVenueRepository
import com.outgoer.api.group.GroupRepository
import com.outgoer.api.hashtag.HashtagRepository
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.music.MusicRepository
import com.outgoer.api.mutual.MutualRepository
import com.outgoer.api.notification.NotificationRepository
import com.outgoer.api.post.PostRepository
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.search.SearchRepository
import com.outgoer.api.sponty.SpontyRepository
import com.outgoer.api.story.StoryRepository
import com.outgoer.api.tagged_post_reels.TaggedPostReelsRepository
import com.outgoer.api.venue.VenueRepository
import com.outgoer.ui.activateaccount.viewmodel.ActivateAccountViewModel
import com.outgoer.ui.add_hashtag.viewmodel.HashtagViewModel
import com.outgoer.ui.addvenuemedia.viewmodel.AddVenueMediaViewModel
import com.outgoer.ui.chat.viewmodel.ChatMessageViewModel
import com.outgoer.ui.chat.viewmodel.CreateNewMessageViewModel
import com.outgoer.ui.comment.viewmodel.PostCommentViewModel
import com.outgoer.ui.create_story.viewmodel.StoryViewModel
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.createevent.viewmodel.EventCategoryViewModel
import com.outgoer.ui.editprofile.viewmodel.EditProfileViewModel
import com.outgoer.ui.followdetail.viewmodel.FollowersViewModel
import com.outgoer.ui.followdetail.viewmodel.FollowingViewModel
import com.outgoer.ui.followdetail.viewmodel.MutualViewModel
import com.outgoer.ui.login.viewmodel.ForgotPasswordViewModel
import com.outgoer.ui.group.viewmodel.GroupViewModel
import com.outgoer.ui.home.chat.viewmodel.ConversationViewModel
import com.outgoer.ui.home.home.viewmodel.HomeViewModel
import com.outgoer.ui.home.map.userinfo.viewmodel.UserInfoViewModel
import com.outgoer.ui.newvenuedetail.viewmodel.VenueListViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.newReels.comment.viewmodel.ReelsCommentViewModel
import com.outgoer.ui.home.newReels.viewmodel.AddNewReelViewModel
import com.outgoer.ui.home.newReels.viewmodel.MainReelsViewModel
import com.outgoer.ui.home.newReels.viewmodel.ReelsViewModel
import com.outgoer.ui.home.newmap.venueevents.viewmodel.VenueEventViewModel
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.VenueDetailsViewModel
import com.outgoer.ui.home.profile.viewmodel.*
import com.outgoer.ui.home.search.account.viewmodel.SearchAccountsViewModel
import com.outgoer.ui.home.search.place.viewmodel.SearchPlacesViewModel
import com.outgoer.ui.home.search.top.viewmodel.SearchTopViewModel
import com.outgoer.ui.home.viewmodel.MainViewModel
import com.outgoer.ui.invitefriends.viewmodel.InviteFriendsLiveStreamViewModel
import com.outgoer.ui.latestevents.viewmodel.LatestEventsViewModel
import com.outgoer.ui.like.viewmodel.PostLikeViewModel
import com.outgoer.ui.livestreamuser.liveuserinfo.viewmodel.LiveUserInfoViewModel
import com.outgoer.ui.livestreamuser.setting.viewmodel.LiveStreamCreateEventSettingViewModel
import com.outgoer.ui.livestreamuser.viewmodel.LiveStreamUserViewModel
import com.outgoer.ui.livestreamvenue.viewmodel.LiveStreamVenueViewModel
import com.outgoer.ui.login.viewmodel.LoginViewModel
import com.outgoer.ui.notification.viewmodel.NotificationViewModel
import com.outgoer.ui.othernearvenue.viewmodel.OtherNearVenueViewModel
import com.outgoer.ui.otherprofile.viewmodel.OtherUserPostViewModel
import com.outgoer.ui.otherprofile.viewmodel.OtherUserProfileViewModel
import com.outgoer.ui.post.viewmodel.AddNewPostViewModel
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewModel
import com.outgoer.ui.posttags.viewmodel.PostTaggedPeopleViewModel
import com.outgoer.ui.reelsdetail.viewmodel.ReelsDetailViewModel
import com.outgoer.ui.reeltags.viewmodel.ReelTaggedPeopleViewModel
import com.outgoer.ui.register.viewmodel.RegisterViewModel
import com.outgoer.ui.login.viewmodel.ResetPasswordViewModel
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.outgoer.ui.suggested.viewmodel.SuggestedUsersViewModel
import com.outgoer.ui.tag.viewmodel.AddTagViewModel
import com.outgoer.ui.tag_venue.viewmodel.TaggedReelsPhotosViewModel
import com.outgoer.ui.venue.viewmodel.CreateVenueViewModel
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewModel
import com.outgoer.ui.venuegallerypreview.viewmodel.VenueGalleryPreviewViewModel
import com.outgoer.ui.verification.viewmodel.VerificationViewModel
import com.outgoer.ui.login.viewmodel.VerifyResetPasswordViewModel
import com.outgoer.ui.music.viewmodel.MusicViewModel
import com.outgoer.ui.report.viewmodel.ReportReasonViewModel
import com.outgoer.ui.save_post_reels.viewmodel.SavedPostReelViewModel
import com.outgoer.ui.userverification.viewmodel.UserVerificationViewModel
import com.outgoer.ui.videorooms.viewmodel.LiveEventVerifyViewModel
import com.outgoer.ui.videorooms.viewmodel.UpdateInviteCoHostStatusViewModel
import com.outgoer.ui.videorooms.viewmodel.VideoRoomsViewModel
import com.outgoer.ui.watchliveevent.viewmodel.LiveWatchingUserViewModel
import com.outgoer.ui.watchliveevent.viewmodel.WatchLiveVideoViewModel
import dagger.Module
import dagger.Provides

@Module
class OutgoerViewModelProvider {

    @Provides
    fun provideRegisterViewModel(
        authenticationRepository: AuthenticationRepository
    ): RegisterViewModel {
        return RegisterViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideLoginViewModel(
        authenticationRepository: AuthenticationRepository
    ): LoginViewModel {
        return LoginViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideVerificationViewModel(
        authenticationRepository: AuthenticationRepository
    ): VerificationViewModel {
        return VerificationViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideForgotPasswordViewModel(
        authenticationRepository: AuthenticationRepository
    ): ForgotPasswordViewModel {
        return ForgotPasswordViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideVerifyResetPasswordViewModel(
        authenticationRepository: AuthenticationRepository
    ): VerifyResetPasswordViewModel {
        return VerifyResetPasswordViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideResetPasswordViewModel(
        authenticationRepository: AuthenticationRepository
    ): ResetPasswordViewModel {
        return ResetPasswordViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideMainViewModel(
        authenticationRepository: AuthenticationRepository
    ): MainViewModel {
        return MainViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideHomeViewModel(
        postRepository: PostRepository,
        storyRepository: StoryRepository,
        spontyRepository: SpontyRepository
    ): HomeViewModel {
        return HomeViewModel(
            postRepository,
            storyRepository,
            spontyRepository
        )
    }

    @Provides
    fun provideProfileViewModel(
        cloudFlareRepository: CloudFlareRepository,
        profileRepository: ProfileRepository,
        authenticationRepository: AuthenticationRepository,
        reelsRepository: ReelsRepository,
        loginUserCache: LoggedInUserCache,
        createVenueRepository: CreateVenueRepository,
        venueRepository: VenueRepository
    ): ProfileViewModel {
        return ProfileViewModel(
            cloudFlareRepository,
            profileRepository,
            authenticationRepository,
            reelsRepository,
            loginUserCache,
            createVenueRepository,
            venueRepository
        )
    }

    @Provides
    fun provideEditProfileViewModel(
        profileRepository: ProfileRepository,
        cloudFlareRepository: CloudFlareRepository,
        loginUserCache: LoggedInUserCache,
        authenticationRepository: AuthenticationRepository,
        followUserRepository: FollowUserRepository,
    ): EditProfileViewModel {
        return EditProfileViewModel(
            profileRepository,
            cloudFlareRepository,
            loginUserCache,
            authenticationRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideAddNewPostViewModel(
        cloudFlareRepository: CloudFlareRepository,
        postRepository: PostRepository,
        followUserRepository: FollowUserRepository,
        loginUserCache: LoggedInUserCache
    ): AddNewPostViewModel {
        return AddNewPostViewModel(
            cloudFlareRepository,
            postRepository,
            followUserRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideSuggestedUsersViewModel(
        profileRepository: ProfileRepository,
        followUserRepository: FollowUserRepository
    ): SuggestedUsersViewModel {
        return SuggestedUsersViewModel(
            profileRepository,
            followUserRepository
        )
    }

    @Provides
    fun providePostLikeViewModel(
        postRepository: PostRepository,
        followUserRepository: FollowUserRepository
    ): PostLikeViewModel {
        return PostLikeViewModel(
            postRepository,
            followUserRepository
        )
    }

    @Provides
    fun providePostCommentViewModel(
        postRepository: PostRepository,
        followUserRepository: FollowUserRepository
    ): PostCommentViewModel {
        return PostCommentViewModel(
            postRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideFollowersViewModel(
        followUserRepository: FollowUserRepository
    ): FollowersViewModel {
        return FollowersViewModel(
            followUserRepository
        )
    }

    @Provides
    fun provideFollowingViewModel(
        followUserRepository: FollowUserRepository,
        groupRepository: GroupRepository
    ): FollowingViewModel {
        return FollowingViewModel(
            followUserRepository,
            groupRepository
        )
    }

    @Provides
    fun provideOtherUserProfileViewModel(
        profileRepository: ProfileRepository,
        chatMessageRepository: ChatMessageRepository,
        reelsRepository: ReelsRepository,
        followUserRepository: FollowUserRepository
    ): OtherUserProfileViewModel {
        return OtherUserProfileViewModel(
            profileRepository,
            chatMessageRepository,
            reelsRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideAddNewReelViewModel(
        cloudFlareRepository: CloudFlareRepository,
        reelsRepository: ReelsRepository,
        loginUserCache: LoggedInUserCache
    ): AddNewReelViewModel {
        return AddNewReelViewModel(
            cloudFlareRepository,
            reelsRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideOtherUserPostViewModel(
        postRepository: PostRepository
    ): OtherUserPostViewModel {
        return OtherUserPostViewModel(
            postRepository
        )
    }

    @Provides
    fun provideMyPostViewModel(
        postRepository: PostRepository,
        loginUserCache: LoggedInUserCache
    ): MyPostViewModel {
        return MyPostViewModel(
            postRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideAddTagViewModel(
        postRepository: PostRepository,
        followUserRepository: FollowUserRepository
    ): AddTagViewModel {
        return AddTagViewModel(
            postRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideReelsViewModel(
        reelsRepository: ReelsRepository,
        followUserRepository: FollowUserRepository,
    ): ReelsViewModel {
        return ReelsViewModel(
            reelsRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideReelsCommentViewModel(
        reelsRepository: ReelsRepository,
        followUserRepository: FollowUserRepository
    ): ReelsCommentViewModel {
        return ReelsCommentViewModel(
            reelsRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideChatMessageViewModel(
        cloudFlareRepository: CloudFlareRepository,
        chatMessageRepository: ChatMessageRepository,
        groupRepository: GroupRepository
    ): ChatMessageViewModel {
        return ChatMessageViewModel(
            cloudFlareRepository,
            chatMessageRepository,
            groupRepository
        )
    }

    @Provides
    fun provideConversationViewModel(
        chatMessageRepository: ChatMessageRepository,
        profileRepository: ProfileRepository,
        followUserRepository: FollowUserRepository
    ): ConversationViewModel {
        return ConversationViewModel(chatMessageRepository, profileRepository, followUserRepository)
    }

    @Provides
    fun providePostDetailViewModel(
        postRepository: PostRepository
    ): PostDetailViewModel {
        return PostDetailViewModel(postRepository)
    }

    @Provides
    fun provideReelsDetailViewModel(
        reelsRepository: ReelsRepository,
        followUserRepository: FollowUserRepository
        ): ReelsDetailViewModel {
        return ReelsDetailViewModel(reelsRepository, followUserRepository)
    }

    @Provides
    fun providePostTaggedPeopleViewModel(
        postRepository: PostRepository,
        followUserRepository: FollowUserRepository
    ): PostTaggedPeopleViewModel {
        return PostTaggedPeopleViewModel(
            postRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideReelTaggedPeopleViewModel(
        reelsRepository: ReelsRepository,
        followUserRepository: FollowUserRepository
    ): ReelTaggedPeopleViewModel {
        return ReelTaggedPeopleViewModel(
            reelsRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideNotificationViewModel(
        notificationRepository: NotificationRepository
    ): NotificationViewModel {
        return NotificationViewModel(
            notificationRepository
        )
    }

    @Provides
    fun provideMapVenueViewModel(
        venueRepository: VenueRepository
    ): MapVenueViewModel {
        return MapVenueViewModel(
            venueRepository
        )
    }

    @Provides
    fun provideVenueListViewModel(
        venueRepository: VenueRepository
    ): VenueListViewModel {
        return VenueListViewModel(
            venueRepository
        )
    }

    @Provides
    fun provideUserInfoViewModel(
        followUserRepository: FollowUserRepository
    ): UserInfoViewModel {
        return UserInfoViewModel(
            followUserRepository
        )
    }

    @Provides
    fun provideOtherNearVenueViewModel(
        venueRepository: VenueRepository
    ): OtherNearVenueViewModel {
        return OtherNearVenueViewModel(
            venueRepository
        )
    }

    @Provides
    fun provideVenueDetailViewModel(
        venueRepository: VenueRepository,
        followUserRepository: FollowUserRepository,
        profileRepository: ProfileRepository
    ): VenueDetailViewModel {
        return VenueDetailViewModel(
            venueRepository,
            followUserRepository,
            profileRepository
        )
    }

    @Provides
    fun provideLatestEventsViewModel(
        venueRepository: VenueRepository
    ): LatestEventsViewModel {
        return LatestEventsViewModel(
            venueRepository
        )
    }

    @Provides
    fun provideSearchTopViewModel(
        searchRepository: SearchRepository
    ): SearchTopViewModel {
        return SearchTopViewModel(
            searchRepository
        )
    }

    @Provides
    fun provideSearchAccountsViewModel(
        searchRepository: SearchRepository,
        followUserRepository: FollowUserRepository
    ): SearchAccountsViewModel {
        return SearchAccountsViewModel(
            searchRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideSearchPlacesViewModel(
        searchRepository: SearchRepository,
        followUserRepository: FollowUserRepository
    ): SearchPlacesViewModel {
        return SearchPlacesViewModel(
            searchRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideLiveStreamUserViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): LiveStreamUserViewModel {
        return LiveStreamUserViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideInviteFriendsLiveStreamViewModel(
        followUserRepository: FollowUserRepository
    ): InviteFriendsLiveStreamViewModel {
        return InviteFriendsLiveStreamViewModel(
            followUserRepository
        )
    }

    @Provides
    fun provideLiveStreamCreateEventSettingViewModel(
        liveRepository: LiveRepository
    ): LiveStreamCreateEventSettingViewModel {
        return LiveStreamCreateEventSettingViewModel(
            liveRepository
        )
    }

    @Provides
    fun provideVideoRoomsViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): VideoRoomsViewModel {
        return VideoRoomsViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideLiveEventVerifyViewModel(
        liveRepository: LiveRepository
    ): LiveEventVerifyViewModel {
        return LiveEventVerifyViewModel(
            liveRepository
        )
    }

    @Provides
    fun provideWatchLiveVideoViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): WatchLiveVideoViewModel {
        return WatchLiveVideoViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideUpdateInviteCoHostStatusViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): UpdateInviteCoHostStatusViewModel {
        return UpdateInviteCoHostStatusViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideLiveWatchingUserViewModel(
        liveRepository: LiveRepository,
    ): LiveWatchingUserViewModel {
        return LiveWatchingUserViewModel(
            liveRepository
        )
    }

    @Provides
    fun provideLiveStreamVenueViewModel(
        liveRepository: LiveRepository,
        loggedInUserCache: LoggedInUserCache
    ): LiveStreamVenueViewModel {
        return LiveStreamVenueViewModel(
            liveRepository,
            loggedInUserCache
        )
    }

    @Provides
    fun provideLiveUserInfoViewModel(
        profileRepository: ProfileRepository,
        followUserRepository: FollowUserRepository
    ): LiveUserInfoViewModel {
        return LiveUserInfoViewModel(
            profileRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideMyFavouriteVenueViewModel(
        venueRepository: VenueRepository
    ): MyFavouriteVenueViewModel {
        return MyFavouriteVenueViewModel(
            venueRepository
        )
    }

    @Provides
    fun provideAddVenueMediaViewModel(
        cloudFlareRepository: CloudFlareRepository,
        venueRepository: VenueRepository,
        loginUserCache: LoggedInUserCache,
        createVenueRepository: CreateVenueRepository
    ): AddVenueMediaViewModel {
        return AddVenueMediaViewModel(
            cloudFlareRepository,
            venueRepository,
            loginUserCache,
            createVenueRepository
        )
    }

    @Provides
    fun provideVenueGalleryPreviewViewModel(
        venueRepository: VenueRepository
    ): VenueGalleryPreviewViewModel {
        return VenueGalleryPreviewViewModel(
            venueRepository
        )
    }

    @Provides
    fun provideNewMessageViewModel(
        profileRepository: ProfileRepository,
        followUserRepository: FollowUserRepository,
        searchRepository: SearchRepository,
        chatMessageRepository: ChatMessageRepository
    ): CreateNewMessageViewModel {
        return CreateNewMessageViewModel(
            profileRepository,
            followUserRepository,
            searchRepository,
            chatMessageRepository
        )
    }

    @Provides
    fun provideActivateAccountViewModel(
        authenticationRepository: AuthenticationRepository
    ): ActivateAccountViewModel {
        return ActivateAccountViewModel(
            authenticationRepository
        )
    }

    @Provides
    fun provideSpontyViewModel(
        spontyRepository: SpontyRepository,
        followUserRepository: FollowUserRepository,
        cloudFlareRepository: CloudFlareRepository,
        loginUserCache: LoggedInUserCache
    ): SpontyViewModel {
        return SpontyViewModel(
            spontyRepository,
            followUserRepository,
            cloudFlareRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideEventViewModel(
        eventRepository: EventRepository,
    ): VenueEventViewModel {
        return VenueEventViewModel(
            eventRepository
        )
    }

    @Provides
    fun provideGroupViewModel(
        cloudFlareRepository: CloudFlareRepository,
        groupRepository: GroupRepository
    ): GroupViewModel {
        return GroupViewModel(
            cloudFlareRepository,
            groupRepository
        )
    }

    @Provides
    fun provideCreateEventViewModel(
        eventRepository: EventRepository,
        cloudFlareRepository: CloudFlareRepository,
        loginUserCache: LoggedInUserCache
    ): CreateEventsViewModel {
        return CreateEventsViewModel(
            eventRepository,
            cloudFlareRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideCreateVenueViewModel(
        cloudFlareRepository: CloudFlareRepository,
        createVenueRepository: CreateVenueRepository
    ): CreateVenueViewModel {
        return CreateVenueViewModel(
            cloudFlareRepository,
            createVenueRepository
        )
    }

    @Provides
    fun provideMutualFriendViewModel(
        mutualRepository: MutualRepository
    ): MutualViewModel {
        return MutualViewModel(
            mutualRepository
        )
    }

    @Provides
    fun provideEventCategoryViewModel(
        eventCategoryRepository: EventCategoryRepository
    ): EventCategoryViewModel {
        return EventCategoryViewModel(
            eventCategoryRepository
        )
    }

    @Provides
    fun provideVenueDetailsCategoryViewModel(
        friendsVenueRepository: FriendsVenueRepository
    ): VenueDetailsViewModel {
        return VenueDetailsViewModel(
            friendsVenueRepository
        )
    }

    @Provides
    fun provideTaggedReelsPhotosViewModel(
        taggedPostReelsRepository: TaggedPostReelsRepository,
        reelsRepository: ReelsRepository,
        postRepository: PostRepository,
        spontyRepository: SpontyRepository,
        followUserRepository: FollowUserRepository
    ): TaggedReelsPhotosViewModel {
        return TaggedReelsPhotosViewModel(
            taggedPostReelsRepository,
            reelsRepository,
            postRepository,
            spontyRepository,
            followUserRepository
        )
    }

    @Provides
    fun provideReportReasonViewModel(
        postRepository: PostRepository
    ): ReportReasonViewModel {
        return ReportReasonViewModel(
            postRepository
        )
    }

    @Provides
    fun provideMainReelsViewModel(
        profileRepository: ProfileRepository,
        venueRepository: VenueRepository
    ): MainReelsViewModel {
        return MainReelsViewModel(
            profileRepository,
            venueRepository
        )
    }
    @Provides
    fun provideUserVerificationViewModel(
        profileRepository: ProfileRepository,
        venueRepository: VenueRepository
    ): UserVerificationViewModel {
        return UserVerificationViewModel(
            profileRepository,
            venueRepository
        )
    }

    @Provides
    fun provideHashtagViewModel(
        hashtagRepository: HashtagRepository
    ): HashtagViewModel {
        return HashtagViewModel(
            hashtagRepository
        )
    }

    @Provides
    fun provideMusicViewModel(
        musicRepository: MusicRepository
    ): MusicViewModel {
        return MusicViewModel(
            musicRepository
        )
    }

    @Provides
    fun provideStoryViewModel(
        storyRepository: StoryRepository,
        cloudFlareRepository: CloudFlareRepository,
        chatMessageRepository: ChatMessageRepository,
        loginUserCache: LoggedInUserCache
    ): StoryViewModel {
        return StoryViewModel(
            storyRepository,
            cloudFlareRepository,
            chatMessageRepository,
            loginUserCache
        )
    }

    @Provides
    fun provideSavedPostReelViewModel(
        postRepository: PostRepository
    ): SavedPostReelViewModel {
        return SavedPostReelViewModel(
            postRepository
        )
    }
}