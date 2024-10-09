package com.outgoer.ui.tag_venue

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentHomeBinding
import com.outgoer.ui.comment.PostCommentBottomSheet
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import com.outgoer.ui.home.home.view.HomePagePostAdapter
import com.outgoer.ui.like.LikesActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.posttags.PostTaggedPeopleBottomSheet
import com.outgoer.ui.tag_venue.viewmodel.TaggedReelsPhotosViewModel
import com.outgoer.ui.tag_venue.viewmodel.VenueTaggedViewState
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VenueTaggedPostFragment : BaseFragment() {
    companion object {
        private val VENUE_ID = "VENUE_ID"

        @JvmStatic
        fun newInstance(venueId: Int): VenueTaggedPostFragment {
            val venueTaggedReelFragment = VenueTaggedPostFragment()

            val bundle = Bundle()
            bundle.putInt(VENUE_ID, venueId)

            venueTaggedReelFragment.arguments = bundle

            return venueTaggedReelFragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<TaggedReelsPhotosViewModel>
    private lateinit var taggedReelsPhotosViewModel: TaggedReelsPhotosViewModel

    private var venueId: Int = 0

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var playPosition = -1

    private lateinit var homePagePostAdapter: HomePagePostAdapter
    private lateinit var venueContext: Context

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OutgoerApplication.component.inject(this)
        taggedReelsPhotosViewModel = getViewModelFromFactory(viewModelFactory)
        venueContext = view.context
        venueId = arguments?.let {
            it.getInt(VENUE_ID)
        } ?: 0

        listenToViewModel()
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        taggedReelsPhotosViewModel.resetPaginationVenuePostReelList(
            TaggedPostReelsRequest(
                "2",
                venueId
            )
        )

        homePagePostAdapter = HomePagePostAdapter(requireContext()).apply {
            homePagePostViewClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is HomePagePostInfoState.UserProfileClick -> {
                        if (state.postInfo.user?.storyCount == 1) {
                            listOfDataItems?.find { it.userId == state.postInfo.userId }?.user?.storyCount = 0
                            homePagePostAdapter.listOfDataItems = listOfDataItems
                            toggleSelectedStory(
                                venueContext,
                                storyListUtil,
                                state.postInfo.userId
                            )
                        } else if (state.postInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.postInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(),
                                        0,
                                        state.postInfo.user?.id ?: 0
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    state.postInfo.userId
                                )
                            )
                        }
                    }
                    is HomePagePostInfoState.MoreClick -> {
                        val bottomSheetFragment = PostMoreOptionBottomSheet()
                        bottomSheetFragment.postMoreOptionClick.subscribeAndObserveOnMainThread {
                            bottomSheetFragment.dismissBottomSheet()
                            taggedReelsPhotosViewModel.deletePost(state.postInfo.id, venueId)
                        }.autoDispose()
                        bottomSheetFragment.show(
                            childFragmentManager,
                            PostMoreOptionBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.TaggedPeopleClick -> {
                        val bottomSheetFragment = PostTaggedPeopleBottomSheet(state.postInfo)
                        bottomSheetFragment.show(
                            childFragmentManager,
                            PostTaggedPeopleBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.OpenTaggedPeopleClick -> {
                        startActivity(NewOtherUserProfileActivity.getIntent(requireContext(), state.postInfo.id))
                    }
                    is HomePagePostInfoState.AddPostLikeClick -> {
                        taggedReelsPhotosViewModel.addPostLike(state.postInfo)
                    }
                    is HomePagePostInfoState.RemovePostLikeClick -> {
                        taggedReelsPhotosViewModel.removeLikeFromPost(state.postInfo)
                    }
                    is HomePagePostInfoState.PostLikeCountClick -> {

                        var likesActivity = LikesActivity.newInstanceWithData(state.postInfo.id)
                        likesActivity.show(childFragmentManager, LikesActivity.javaClass.name)
                    }
                    is HomePagePostInfoState.CommentClick -> {
                        val reelsCommentBottomSheet = PostCommentBottomSheet(state.postInfo).apply {
                            dismissClick.subscribeAndObserveOnMainThread {
                                dismiss()
                            }.autoDispose()
                        }
                        reelsCommentBottomSheet.show(
                            childFragmentManager,
                            PostCommentBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.ShareClick -> {
                        ShareHelper.shareDeepLink(requireContext(), true, state.postInfo.id) {
                            ShareHelper.shareText(requireContext(), it)
                        }
                    }
                    is HomePagePostInfoState.AddBookmarkClick -> {
                        taggedReelsPhotosViewModel.addPostToBookmark(state.postInfo)
                    }
                    is HomePagePostInfoState.RemoveBookmarkClick -> {
                        taggedReelsPhotosViewModel.removePostToBookmark(state.postInfo)
                    }
                    is HomePagePostInfoState.VenueTaggedProfileClick -> {
                        if (state.postInfo.venueTags?.id ?: 0 > 0) {
                            startActivityWithDefaultAnimation(
                                NewVenueDetailActivity.getIntent(
                                    requireContext(),
                                    0,
                                    state.postInfo.venueTags?.id ?: 0
                                )
                            )
                        }
                    }
                    else -> {

                    }
                }
            }.autoDispose()
        }

        binding.rvHomePagePost.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = homePagePostAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    (layoutManager as LinearLayoutManager).apply {
                        Jzvd.goOnPlayOnPause()
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()

                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                taggedReelsPhotosViewModel.loadMoreVenuePostReelList(
                                    TaggedPostReelsRequest(
                                        "2",
                                        venueId
                                    )
                                )
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            taggedReelsPhotosViewModel.resetPaginationVenuePostReelList(
                TaggedPostReelsRequest(
                    "2",
                    venueId
                )
            )
        }.autoDispose()

    }

    override fun onResume() {
        super.onResume()

        taggedReelsPhotosViewModel.resetPaginationVenuePostReelList(
            TaggedPostReelsRequest(
                "2",
                venueId
            )
        )

    }


    private fun listenToViewModel() {
        taggedReelsPhotosViewModel.venueTaggedState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueTaggedViewState.ErrorMessage -> {
                    activity?.showLongToast(it.errorMessage)
                }
                is VenueTaggedViewState.LoadingState -> {
                }
                is VenueTaggedViewState.SuccessMessage -> {
                    activity?.showLongToast(it.successMessage)
                }
                is VenueTaggedViewState.ListOfPostInfo -> {
                    taggedReelsPhotosViewModel.getTaggedViewChange(TaggedPostReelsViewRequest("post", venueId))

                    binding.llNoData.visibility = if (it.listofpost.size > 0) View.GONE else View.VISIBLE
                    homePagePostAdapter.listOfDataItems = it.listofpost
                    hideShowNoData(it.listofpost)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(postInfoList: List<PostInfo>) {
        if (postInfoList.isNotEmpty()) {
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

    private fun autoPlayVideo() {
        val player: JzvdStdOutgoer? =
            binding.rvHomePagePost.getChildAt(playPosition).findViewById(R.id.outgoerVideoPlayer)
        player?.apply {
            Timber.tag("<><><><> POS ").e(playPosition.toString())
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
                JZMediaExoKotlin::class.java
            )
//            Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
            startVideoAfterPreloading()
        }
    }

    override fun onPause() {
        super.onPause()
        Observable.timer(200, TimeUnit.MILLISECONDS)
            .subscribeAndObserveOnMainThread {
                Jzvd.goOnPlayOnPause()
            }.autoDispose()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        _binding = null
        super.onDestroy()
    }
}