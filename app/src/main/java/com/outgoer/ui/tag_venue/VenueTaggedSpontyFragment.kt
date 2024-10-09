package com.outgoer.ui.tag_venue

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentVenueTaggedSpontyBinding
import com.outgoer.ui.newvenuedetail.FullScreenActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.sponty.comment.SpontyReplyBottomSheet
import com.outgoer.ui.sponty.SpontyUserBottomsheet
import com.outgoer.ui.sponty.view.SpontyListAdapter
import com.outgoer.ui.tag_venue.viewmodel.TaggedReelsPhotosViewModel
import com.outgoer.ui.tag_venue.viewmodel.VenueTaggedViewState
import com.outgoer.ui.temp.TempActivity
import com.outgoer.utils.Utility
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import javax.inject.Inject

class VenueTaggedSpontyFragment : BaseFragment() {

    private var _binding: FragmentVenueTaggedSpontyBinding? = null
    private val binding get() = _binding!!

    private lateinit var spontyListAdapter: SpontyListAdapter
    var listofsponty: ArrayList<SpontyResponse> = arrayListOf()


    var index: Int = -1

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<TaggedReelsPhotosViewModel>
    private lateinit var taggedReelsPhotosViewModel: TaggedReelsPhotosViewModel
    private var venueId: Int = 0
    private lateinit var taggedSpontyContext: Context

    companion object {
        private val VENUE_ID = "VENUE_ID"

        @JvmStatic
        fun newInstance(venueId: Int): VenueTaggedSpontyFragment {
            val venueTaggedReelFragment = VenueTaggedSpontyFragment()

            val bundle = Bundle()
            bundle.putInt(VENUE_ID, venueId)

            venueTaggedReelFragment.arguments = bundle

            return venueTaggedReelFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        taggedReelsPhotosViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVenueTaggedSpontyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taggedSpontyContext = view.context
        venueId = arguments?.let {
            it.getInt(VENUE_ID)
        } ?: 0

        listenToViewEvents()
        listenToViewModel()
    }

    override fun onResume() {
        super.onResume()
        taggedReelsPhotosViewModel.resetPaginationVenuePostReelList(TaggedPostReelsRequest("3", venueId))

    }

    private fun listenToViewEvents() {

        spontyListAdapter = SpontyListAdapter(requireContext()).apply {
            spontyActionState.subscribeAndObserveOnMainThread { state ->
                when(state) {
                    is SpontyActionState.CommentClick -> {
                        val spontyId = state.commentInfo.id
                        val spontyReplyBottomSheet = SpontyReplyBottomSheet.newInstance(state.commentInfo.id)
                        spontyReplyBottomSheet.commentActionState.subscribeAndObserveOnMainThread { res ->
                            listofsponty.find { item -> item.id == spontyId }?.apply {
                                totalComments = res
                            }

                            spontyListAdapter.listOfSponty = listofsponty
                        }
                        spontyReplyBottomSheet.show(childFragmentManager, "SpontyReplyBottomSheet")
                    }
                    is SpontyActionState.LikeDisLike ->{
                        state.commentInfo.id.let { it1 ->
                            taggedReelsPhotosViewModel.addRemoveSpontyLike(SpontyActionRequest(it1))
                        }
                    }
                    is SpontyActionState.JoinUnJoinClick -> {
                        index = listofsponty.indexOf(state.spontyResponse)
                        taggedReelsPhotosViewModel.addRemoveSponty(AllJoinSpontyRequest(spontyId = state.spontyResponse.id))
                    }
                    is SpontyActionState.TaggedUser -> {
                        val loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                        val clickedText = state.clickedText
                        val tagsList = state.commentInfo.spontyTags
                        if (!tagsList.isNullOrEmpty()) {
                            val tag = tagsList.firstOrNull { cInfo ->
                                cInfo.user?.username == clickedText
                            }
                            if (tag != null) {
                                if (loggedInUserId != tag.userId) {
                                    if (tag.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                        if (loggedInUserCache.getUserId() == tag.userId) {
                                            RxBus.publish(RxEvent.OpenVenueUserProfile)
                                        } else {
                                            startActivityWithDefaultAnimation(
                                                NewVenueDetailActivity.getIntent(
                                                    requireContext(),
                                                    0,
                                                    tag.userId
                                                )
                                            )
                                        }
                                    } else {
                                        startActivityWithDefaultAnimation(
                                            NewOtherUserProfileActivity.getIntent(
                                                requireContext(),
                                                tag.userId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is SpontyActionState.UserImageClick -> {
                        if (state.commentInfo.user?.storyCount == 1) {
                            listofsponty.find { it.userId == state.commentInfo.userId }?.user?.storyCount = 0
                            spontyListAdapter.listOfSponty = listofsponty
                            toggleSelectedStory(
                                taggedSpontyContext,
                                storyListUtil,
                                state.commentInfo.userId
                            )
                        } else if (state.commentInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.commentInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0, state.commentInfo.userId
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(), state.commentInfo.userId
                                )
                            )
                        }
                    }
                    is SpontyActionState.VideoViewClick -> {
                        Outgoer.exoCacheManager.prepareCacheVideo(state.postVideoUrl.plus("?clientBandwidthHint=2.5"))
//                          requireActivity().startActivity(
//                              FullScreenImageActivity.getIntent(
//                                  requireContext(),
//                                  it.plus("?clientBandwidthHint=2.5")
//                              )
//                          )
                        startActivityWithDefaultAnimation(TempActivity.getIntent(requireContext(), state.postVideoUrl,state.postVideoThumbnailUrl))
                    }
                    is SpontyActionState.ImageClick -> {
                        requireActivity().startActivity(FullScreenActivity.getIntent(requireContext(), state.imageUrl))
                    }
                    is SpontyActionState.VenueClick -> {
                        startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, state.commentInfo.venueTags?.id ?: 0))
                    }
                    is SpontyActionState.CheckAction -> {
                        listofsponty.find { it.id == state.commentInfo.id }?.apply {
                            joinUsers?.let {
                                if (joinUsers.size > 0) {
                                    var spontyUserBottomsheet = SpontyUserBottomsheet.newInstance(joinUsers)
                                    spontyUserBottomsheet?.show(
                                        childFragmentManager,
                                        SpontyUserBottomsheet.TAG
                                    )
                                }
                            }
                        }
                    }
                    is SpontyActionState.DeleteSponty -> {
                        listofsponty.remove(state.commentInfo)
                        taggedReelsPhotosViewModel.removeSponty(state.commentInfo.id)
                        spontyListAdapter.listOfSponty = listofsponty
                    }
                    else -> {

                    }
                }
            }
        }

        spontyListAdapter.userId = loggedInUserCache.getUserId() ?: -1

        binding.spontyRecyclerView.apply {
            adapter = spontyListAdapter
        }
    }

    private fun listenToViewModel() {
        taggedReelsPhotosViewModel.venueTaggedState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueTaggedViewState.LoadingState -> {}
                is VenueTaggedViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueTaggedViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is VenueTaggedViewState.ListOfSpontyInfo -> {
                    taggedReelsPhotosViewModel.getTaggedViewChange(TaggedPostReelsViewRequest("sponty", venueId))
                    listofsponty.clear()
                    listofsponty.addAll(it.listofpost)
                    spontyListAdapter.listOfSponty = it.listofpost
                }
                is VenueTaggedViewState.AddSpontyJoin -> {
                    if (it.joinStatus == 0) {
                        listofsponty.get(index).spontyJoin = false
                    } else {
                        listofsponty.get(index).spontyJoin = true
                    }

                    spontyListAdapter.listOfSponty = listofsponty
                }
                is VenueTaggedViewState.AddRemoveSpontyLike -> {
                    val sponty = it.addSpontyLike
                    val spontyIds = it.spontyId

                    if (sponty != null) {
                        listofsponty.find { (sponty.spontyId) == it.id }?.apply {
                            spontyLike = !spontyLike
                            totalLikes = if (spontyLike) {
                                totalLikes?.let { it + 1 } ?: 0
                            } else {
                                totalLikes?.let { it - 1 } ?: 0
                            }
                        }
                    } else {
                        listofsponty.find { (spontyIds) == it.id }?.apply {
                            spontyLike = !spontyLike
                            totalLikes = if (spontyLike) {
                                totalLikes?.let { it + 1 } ?: 0
                            } else {
                                totalLikes?.let { it - 1 } ?: 0
                            }
                        }
                    }
                    spontyListAdapter.listOfSponty = listofsponty
                }

                else -> {}
            }
        }
    }
}