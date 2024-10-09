package com.outgoer.di

import android.app.Application
import com.outgoer.ui.userverification.VerificationActivity
import com.outgoer.service.NotificationService
import com.outgoer.service.StoryUploadingService
import com.outgoer.service.UploadingPostReelsService
import com.outgoer.ui.activateaccount.ActivateAccountActivity
import com.outgoer.ui.add_hashtag.HashtagActivity
import com.outgoer.ui.addvenuemedia.AddVenueMediaActivity
import com.outgoer.ui.block.BlockProfileActivity
import com.outgoer.ui.chat.CreateNewMessageActivity
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.chat.reaction_bottom_sheet.ReactionBottomSheetView
import com.outgoer.ui.comment.PostCommentBottomSheet
import com.outgoer.ui.comment.PostCommentMoreOptionBottomSheet
import com.outgoer.ui.create_story.AddToStoryActivity
import com.outgoer.ui.create_story.OutgoerVenueFragment
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.createevent.CreateEventsActivity
import com.outgoer.ui.createevent.EventCategoryBottomSheet
import com.outgoer.ui.createevent.VenueLocationBottomSheet
import com.outgoer.ui.createevent.view.NearVenueView
import com.outgoer.ui.createevent.view.VenueLocationView
import com.outgoer.ui.editprofile.AddExternalLinkActivity
import com.outgoer.ui.editprofile.EditProfileActivity
import com.outgoer.ui.event_list.EventListActivity
import com.outgoer.ui.followdetail.FollowDetailActivity
import com.outgoer.ui.followdetail.FollowersFragment
import com.outgoer.ui.followdetail.FollowingFragment
import com.outgoer.ui.followdetail.MutualFragment
import com.outgoer.ui.followdetail.view.FollowersDetailView
import com.outgoer.ui.followdetail.view.FollowingDetailView
import com.outgoer.ui.group.audio.AudioRecordBottomSheet
import com.outgoer.ui.group.create.AddGroupActivity
import com.outgoer.ui.group.create.CreateGroupActivity
import com.outgoer.ui.group.details.GroupDetailsActivity
import com.outgoer.ui.group.edit_profile.EditGroupActivity
import com.outgoer.ui.group.editgroup.EditAdminGroupBottomSheet
import com.outgoer.ui.group.editgroup.EditGroupBottomSheet
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.HomeTabManager
import com.outgoer.ui.home.chat.NewChatConversationActivity
import com.outgoer.ui.home.chat.view.NearbyPeopleView
import com.outgoer.ui.home.chat.view.NewFindFriendView
import com.outgoer.ui.home.create.CreateNewReelInfoActivity
import com.outgoer.ui.home.home.HomeFragment
import com.outgoer.ui.home.home.ReelsActivity
import com.outgoer.ui.home.home.SharePostReelBottomSheet
import com.outgoer.ui.home.home.view.HomePagePostView
import com.outgoer.ui.home.home.view.HomePageStoryView
import com.outgoer.ui.home.home.view.HomePageVideoPostView
import com.outgoer.ui.home.home.view.StoryView
import com.outgoer.ui.home.map.userinfo.UserInfoBottomSheet
import com.outgoer.ui.home.map.venueinfo.VenueInfoBottomSheet
import com.outgoer.ui.home.newReels.DiscoverReelsFragment
import com.outgoer.ui.home.newReels.NewReelsFragment
import com.outgoer.ui.home.newReels.comment.NewReelsCommentBottomSheet
import com.outgoer.ui.home.newReels.hashtag.NewReelsHashtagActivity
import com.outgoer.ui.home.newReels.hashtag.PlayReelsByHashtagActivity
import com.outgoer.ui.home.newReels.view.NewPlayReelView
import com.outgoer.ui.home.newmap.venueevents.EventMediaFragment
import com.outgoer.ui.home.newmap.venueevents.VenueEventDetailActivity
import com.outgoer.ui.home.newmap.venueevents.VenueEventsFragment
import com.outgoer.ui.home.newmap.venueevents.joinrequests.JoinRequestFragment
import com.outgoer.ui.home.newmap.venueevents.location.LocationFragment
import com.outgoer.ui.home.newmap.venueevents.view.EventVenueView
import com.outgoer.ui.home.newmap.venuemap.NewVenueMapFragment
import com.outgoer.ui.home.newmap.venuemap.NewVenuePeopleFragment
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CastMessagingBottomSheet
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CheckInBottomSheet
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.VenueDetailsBottomsheet
import com.outgoer.ui.home.newmap.venuemap.view.VenueListView
import com.outgoer.ui.home.newmap.venuesponty.SpontyTagBottomSheet
import com.outgoer.ui.home.profile.newprofile.NewMyFavouriteVenueFragment
import com.outgoer.ui.home.profile.newprofile.NewMyPostsFragment
import com.outgoer.ui.home.profile.newprofile.NewMyProfileFragment
import com.outgoer.ui.home.profile.newprofile.NewMyReelFragment
import com.outgoer.ui.home.profile.newprofile.SwitchAccountBottomSheet
import com.outgoer.ui.home.profile.newprofile.setting.AccountStatusActivity
import com.outgoer.ui.home.profile.newprofile.setting.NewProfileSettingActivity
import com.outgoer.ui.home.profile.newprofile.view.NewMyFavouriteVenueView
import com.outgoer.ui.home.profile.venue_profile.VenueProfileFragment
import com.outgoer.ui.home.reels.view.PlayReelView
import com.outgoer.ui.home.search.SearchActivity
import com.outgoer.ui.home.search.SearchFragment
import com.outgoer.ui.home.search.account.SearchAccountsFragment
import com.outgoer.ui.home.search.account.view.SearchAccountsView
import com.outgoer.ui.home.search.place.SearchPlacesFragment
import com.outgoer.ui.home.search.place.view.SearchPlacesView
import com.outgoer.ui.home.search.top.SearchTopFragment
import com.outgoer.ui.invitefriends.InviteFriendsLiveStreamBottomSheet
import com.outgoer.ui.latestevents.EventInfoBottomSheet
import com.outgoer.ui.latestevents.LatestEventsActivity
import com.outgoer.ui.like.LikesActivity
import com.outgoer.ui.like.view.PostLikesView
import com.outgoer.ui.livestreamuser.LiveStreamUserActivity
import com.outgoer.ui.livestreamuser.liveuserinfo.LiveUserInfoBottomSheet
import com.outgoer.ui.livestreamuser.setting.LiveStreamCreateEventSettingBottomSheet
import com.outgoer.ui.livestreamvenue.LiveStreamVenueActivity
import com.outgoer.ui.login.AddUsernameEmailSocialLoginBottomSheet
import com.outgoer.ui.login.LoginBottomSheet
import com.outgoer.ui.login.bottomsheet.*
import com.outgoer.ui.music.AddMusicActivity
import com.outgoer.ui.music.MusicListFragment
import com.outgoer.ui.newnotification.ActivityNotificationFragment
import com.outgoer.ui.newnotification.GeneralNotificationFragment
import com.outgoer.ui.newnotification.NewNotificationActivity
import com.outgoer.ui.newnotification.view.ActivityNotificationView
import com.outgoer.ui.newnotification.view.GeneralNotificationView
import com.outgoer.ui.newvenuedetail.*
import com.outgoer.ui.othernearvenue.OtherNearVenueActivity
import com.outgoer.ui.othernearvenue.view.OtherNearVenueView
import com.outgoer.ui.otherprofile.*
import com.outgoer.ui.post.AddNewPostInfoActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.posttags.PostTaggedPeopleBottomSheet
import com.outgoer.ui.posttags.view.PostTaggedPeopleView
import com.outgoer.ui.progress_dialog.ProgressDialogFragment
import com.outgoer.ui.reels.comment.ReelCommentMoreOptionBottomSheet
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.reeltags.ReelTaggedPeopleBottomSheet
import com.outgoer.ui.reeltags.view.ReelTaggedPeopleView
import com.outgoer.ui.register.RegisterBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.ui.save_post_reels.SavePostListFragment
import com.outgoer.ui.savecredentials.SaveInfoActivity
import com.outgoer.ui.splash.NewSplashActivity
import com.outgoer.ui.sponty.*
import com.outgoer.ui.sponty.comment.SpontyCommentMoreOptionBottomSheet
import com.outgoer.ui.sponty.comment.SpontyReplyBottomSheet
import com.outgoer.ui.story.StoryListFragment
import com.outgoer.ui.story.StoryUserBottomSheet
import com.outgoer.ui.suggested.SuggestedUsersActivity
import com.outgoer.ui.tag.AddTagToPostActivity
import com.outgoer.ui.tag_venue.VenueTaggedPostFragment
import com.outgoer.ui.tag_venue.VenueTaggedReelFragment
import com.outgoer.ui.tag_venue.VenueTaggedSpontyFragment
import com.outgoer.ui.vennue_list.VenueListActivity
import com.outgoer.ui.venue.*
import com.outgoer.ui.venue.update.*
import com.outgoer.ui.venuedetail.VenueDetailActivity
import com.outgoer.ui.venuedetail.view.VenueDetailOtherNearPlacesView
import com.outgoer.ui.venuegallerypreview.VenueGalleryPreviewActivity
import com.outgoer.ui.videorooms.LiveEventLockDialogFragment
import com.outgoer.ui.videorooms.UpdateInviteCoHostStatusDialog
import com.outgoer.ui.videorooms.VideoRoomFragment
import com.outgoer.ui.videorooms.VideoRoomsActivity
import com.outgoer.ui.watchliveevent.LiveWatchBottomSheetFragment
import com.outgoer.ui.watchliveevent.WatchLiveEventActivity

/**
 *
 * This base app class will be extended by either Main or Demo project.
 *
 * It then will provide library project app component accordingly.
 *
 */
abstract class BaseUiApp : Application() {
    abstract fun getAppComponent(): BaseAppComponent
    abstract fun setAppComponent(baseAppComponent: BaseAppComponent)
}

/**
 * Base app component
 *
 * This class should have all the inject targets classes
 *
 */
interface BaseAppComponent {
    fun inject(app: Application)

    fun inject(notificationService: NotificationService)

    fun inject(homeActivity: HomeActivity)
    fun inject(reelsActivity: ReelsActivity)
    fun inject(homeTabManager: HomeTabManager)
    fun inject(homeFragment: HomeFragment)
    fun inject(searchFragment: SearchFragment)
    fun inject(searchTopFragment: SearchTopFragment)
    fun inject(searchAccountsFragment: SearchAccountsFragment)
    fun inject(searchPlacesFragment: SearchPlacesFragment)
    fun inject(searchAccountsView: SearchAccountsView)

    fun inject(editProfileActivity: EditProfileActivity)

    fun inject(addNewPostInfoActivity: AddNewPostInfoActivity)
    fun inject(suggestedUsersActivity: SuggestedUsersActivity)
    fun inject(addTagToPostActivity: AddTagToPostActivity)
    fun inject(likesActivity: LikesActivity)
    fun inject(postLikesView: PostLikesView)
    fun inject(postCommentBottomSheet: PostCommentBottomSheet)
    fun inject(followersFragment: FollowersFragment)
    fun inject(followingFragment: FollowingFragment)
    fun inject(followDetailActivity: FollowDetailActivity)
    fun inject(postCommentMoreOptionBottomSheet: PostCommentMoreOptionBottomSheet)

    fun inject(followersDetailView: FollowersDetailView)
    fun inject(followingDetailView: FollowingDetailView)
    fun inject(searchPlacesView: SearchPlacesView)


    fun inject(playReelView: PlayReelView)
    fun inject(reelCommentMoreOptionBottomSheet: ReelCommentMoreOptionBottomSheet)

    fun inject(postDetailActivity: PostDetailActivity)
    fun inject(reelsDetailActivity: ReelsDetailActivity)

    fun inject(postTaggedPeopleBottomSheet: PostTaggedPeopleBottomSheet)
    fun inject(postTaggedPeopleView: PostTaggedPeopleView)

    fun inject(reelTaggedPeopleBottomSheet: ReelTaggedPeopleBottomSheet)
    fun inject(reelTaggedPeopleView: ReelTaggedPeopleView)

    fun inject(userInfoBottomSheet: UserInfoBottomSheet)
    fun inject(otherNearVenueActivity: OtherNearVenueActivity)
    fun inject(venueDetailActivity: VenueDetailActivity)

    fun inject(latestEventsActivity: LatestEventsActivity)

    fun inject(addUsernameEmailSocialLoginBottomSheet: AddUsernameEmailSocialLoginBottomSheet)

    fun inject(liveStreamUserActivity: LiveStreamUserActivity)
    fun inject(liveStreamCreateEventSettingBottomSheet: LiveStreamCreateEventSettingBottomSheet)

    fun inject(videoRoomsActivity: VideoRoomsActivity)
    fun inject(liveEventLockDialogFragment: LiveEventLockDialogFragment)
    fun inject(watchLiveEventActivity: WatchLiveEventActivity)
    fun inject(updateInviteCoHostStatusDialog: UpdateInviteCoHostStatusDialog)

    fun inject(inviteFriendsLiveStreamBottomSheet: InviteFriendsLiveStreamBottomSheet)
    fun inject(liveWatchBottomSheetFragment: LiveWatchBottomSheetFragment)
    fun inject(liveStreamVenueActivity: LiveStreamVenueActivity)

    fun inject(liveUserInfoBottomSheet: LiveUserInfoBottomSheet)

    fun inject(otherNearVenueView: OtherNearVenueView)
    fun inject(venueDetailOtherNearPlacesView: VenueDetailOtherNearPlacesView)

    fun inject(addVenueMediaActivity: AddVenueMediaActivity)

    fun inject(registerVenueActivity: RegisterVenueActivity)
    fun inject(venueGalleryPreviewActivity: VenueGalleryPreviewActivity)

    fun inject(venueUpdateActivity: VenueUpdateActivity)

    fun inject(eventInfoBottomSheet: EventInfoBottomSheet)

    fun inject(activateAccountActivity: ActivateAccountActivity)

    fun inject(newSplashActivity: NewSplashActivity)

    fun inject(newPlayReelView: NewPlayReelView)
    fun inject(newMyProfileFragment: NewMyProfileFragment)
    fun inject(newMyPostsFragment: NewMyPostsFragment)
    fun inject(generalNotificationFragment: GeneralNotificationFragment)
    fun inject(newOtherUserProfileActivity: NewOtherUserProfileActivity)
    fun inject(newOtherUserPostFragment: NewOtherUserPostFragment)
    fun inject(discoverReelsFragment: DiscoverReelsFragment)
    fun inject(newReelsCommentBottomSheet: NewReelsCommentBottomSheet)
    fun inject(activityNotificationView: ActivityNotificationView)
    fun inject(generalNotificationView: GeneralNotificationView)
    fun inject(activityNotificationFragment: ActivityNotificationFragment)
    fun inject(newChatActivity: NewChatActivity)
    fun inject(newProfileSettingActivity: NewProfileSettingActivity)
    fun inject(newMyFavouriteVenueFragment: NewMyFavouriteVenueFragment)
    fun inject(newVenueMapFragment: NewVenueMapFragment)
    fun inject(newMapFragment: com.outgoer.ui.home.newmap.NewMapFragment)
    fun inject(newVenueDetailActivity: NewVenueDetailActivity)
    fun inject(createReelsDetailActivity: CreateNewReelInfoActivity)
    fun inject(newReelsHashtagActivity: NewReelsHashtagActivity)
    fun inject(playReelsByHashtagActivity: PlayReelsByHashtagActivity)


    fun inject(signInBottomSheet: SignInBottomSheet)
    fun inject(signupBottomSheet: SignupBottomSheet)
    fun inject(otpVerificationBottomSheet: OtpVerificationBottomSheet)
    fun inject(forgotPasswordBottomSheet: ForgotPasswordBottomSheet)
    fun inject(resetOtpVerificationBottomSheet: ResetOtpVerificationBottomSheet)
    fun inject(resetPasswordBottomSheet: ResetPasswordBottomSheet)
    fun inject(loginBottomSheet: LoginBottomSheet)
    fun inject(registerBottomSheet: RegisterBottomSheet)
    fun inject(newMyReelFragment: NewMyReelFragment)
    fun inject(newFindFriendView: NewFindFriendView)
    fun inject(createNewMessageActivity: CreateNewMessageActivity)
    fun inject(spontyListFragment: SpontyListFragment)
    fun inject(createSpontyActivity: CreateSpontyActivity)
    fun inject(spontyReplyBottomsheet: SpontyReplyBottomSheet)
    fun inject(spontyTagBottomSheet: SpontyTagBottomSheet)
    fun inject(venueEventsFragment: VenueEventsFragment)
    fun inject(venueEventDetailActivity: VenueEventDetailActivity)
    fun inject(createEventsActivity: CreateEventsActivity)
    fun inject(addMediaEventActivity: AddMediaEventActivity)
    fun inject(joinRequestFragment: JoinRequestFragment)
    fun inject(searchActivity: SearchActivity)
    fun inject(addGroupActivity: AddGroupActivity)
    fun inject(createGroupActivity: CreateGroupActivity)
    fun inject(groupDetailsActivity: GroupDetailsActivity)
    fun inject(editGroupActivity: EditGroupActivity)
    fun inject(audioRecordBottomSheet: AudioRecordBottomSheet)
    fun inject(editGroupBottomSheet: EditGroupBottomSheet)
    fun inject(editAdminGroupBottomSheet: EditAdminGroupBottomSheet)
    fun inject(venueMediaActivity: VenueMediaActivity)
    fun inject(addExternalLinkActivity: AddExternalLinkActivity)
    fun inject(mutualFragment: MutualFragment)
    fun inject(venueCategoryActivity: VenueCategoryActivity)
    fun inject(venueDetailReviewFragment: VenueDetailReviewFragment)
    fun inject(venueReviewActivity: VenueReviewActivity)
    fun inject(venueDetailPhotosFragment: VenueDetailPhotosFragment)
    fun inject(venueProfileFragment: VenueProfileFragment)
    fun inject(venueTimingUpdateActivity: VenueTimingUpdateActivity)
    fun inject(venueDetailAboutFragment: VenueDetailAboutFragment)
    fun inject(venueInfoUpdateActivity: VenueInfoUpdateActivity)
    fun inject(eventCategoryBottomSheet: EventCategoryBottomSheet)
    fun inject(newReelsFragment: NewReelsFragment)
    fun inject(checkInBottomSheet: CheckInBottomSheet)
    fun inject(venueDetailsBottomsheet: VenueDetailsBottomsheet)
    fun inject(venueListActivity: VenueListActivity)
    fun inject(venueTaggedReelFragment: VenueTaggedReelFragment)
    fun inject(venueTaggedPostFragment: VenueTaggedPostFragment)
    fun inject(newVenuePeopleFragment: NewVenuePeopleFragment)
    fun inject(venueTaggedSpontyFragment: VenueTaggedSpontyFragment)
    fun inject(spontyDetailsActivity: SpontyDetailsActivity)
    fun inject(eventListActivity: EventListActivity)
    fun inject(venueDetailsEventFragment: VenueDetailsEventFragment)
    fun inject(eventMediaFragment: EventMediaFragment)
    fun inject(reportBottomSheet: ReportBottomSheet)
    fun inject(videoRoomFragment: VideoRoomFragment)
    fun inject(newChatConversationActivity: NewChatConversationActivity)
    fun inject(verificationActivity: VerificationActivity)
    fun inject(progressDialogFragment: ProgressDialogFragment)
    fun inject(venueInfoActivity: VenueInfoActivity)
    fun inject(venueAvailabilityActivity: VenueAvailabilityActivity)
    fun inject(hashtagActivity: HashtagActivity)
    fun inject(addMusicActivity: AddMusicActivity)
    fun inject(musicListFragment: MusicListFragment)
    fun inject(uploadingPostReelsService: UploadingPostReelsService)
    fun inject(addToStoryActivity: AddToStoryActivity)
    fun inject(storyListFragment: StoryListFragment)
    fun inject(outgoerVenueFragment: OutgoerVenueFragment)
    fun inject(storyView: StoryView)
    fun inject(storyUploadingService: StoryUploadingService)
    fun inject(newNotificationActivity: NewNotificationActivity)
    fun inject(accountStatusActivity: AccountStatusActivity)
    fun inject(homePageStoryView: HomePageStoryView)
    fun inject(homePageVideoPostView: HomePageVideoPostView)
    fun inject(sharePostReelBottomSheet: SharePostReelBottomSheet)
    fun inject(savePostListFragment: SavePostListFragment)
    fun inject(homePagePostView: HomePagePostView)
    fun inject(spontyVenueLocationBottomSheet: SpontyVenueLocationBottomSheet)
    fun inject(venueLocationBottomSheet1: VenueLocationBottomSheet)
    fun inject(storyUserBottomSheet: StoryUserBottomSheet)
    fun inject(castMessagingBottomSheet: CastMessagingBottomSheet)
    fun inject(spontyCommentMoreOptionBottomSheet: SpontyCommentMoreOptionBottomSheet)
    fun inject(venueInfoBottomSheet: VenueInfoBottomSheet)
    fun inject(newMyFavouriteVenueView: NewMyFavouriteVenueView)
    fun inject(venueListView: VenueListView)
    fun inject(nearbyPeopleView: NearbyPeopleView)
    fun inject(nearVenueView: NearVenueView)
    fun inject(venueLocationView: VenueLocationView)
    fun inject(eventVenueView: EventVenueView)
    fun inject(locationFragment: LocationFragment)
    fun inject(blockProfileActivity: BlockProfileActivity)
    fun inject(switchAccountBottomSheet: SwitchAccountBottomSheet)
    fun inject(saveInfoActivity: SaveInfoActivity)
    fun inject(reactionBottomSheetView: ReactionBottomSheetView)
}

/**
 * Extension for getting component more easily
 */
fun BaseUiApp.getComponent(): BaseAppComponent {
    return this.getAppComponent()
}
