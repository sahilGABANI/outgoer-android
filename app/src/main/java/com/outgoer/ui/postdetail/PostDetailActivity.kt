package com.outgoer.ui.postdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityPostDetailBinding
import com.outgoer.ui.comment.PostCommentBottomSheet
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import com.outgoer.ui.home.home.SharePostReelBottomSheet
import com.outgoer.ui.home.home.view.HomePagePostAdapter
import com.outgoer.ui.like.LikesActivity
import com.outgoer.ui.newvenuedetail.FullScreenActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewModel
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewModel.Companion.postDetailState
import com.outgoer.ui.postdetail.viewmodel.PostDetailViewState
import com.outgoer.ui.posttags.PostTaggedPeopleBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.ui.temp.TempActivity
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class PostDetailActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_POST_ID = "INTENT_EXTRA_POST_ID"
        private const val POST_SHOW_COMMENTS = "POST_SHOW_COMMENTS"
        private const val POST_SHOW_TAGGED_PEOPLE = "POST_SHOW_TAGGED_PEOPLE"

        fun getIntent(context: Context, postId: Int, showComments: Boolean = false, showTaggedPeople: Boolean = false): Intent {
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_POST_ID, postId)
            intent.putExtra(POST_SHOW_COMMENTS, showComments)
            intent.putExtra(POST_SHOW_TAGGED_PEOPLE, showTaggedPeople)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<PostDetailViewModel>
    private lateinit var postDetailViewModel: PostDetailViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityPostDetailBinding

    private lateinit var homePagePostAdapter: HomePagePostAdapter
    private var listOfPostInfo: ArrayList<PostInfo> = arrayListOf()
    private var postId: Int = -1
    private var showComments: Boolean = false
    private var showTaggedPeople: Boolean = false

    var height: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        postDetailViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            val postId = it.getIntExtra(INTENT_EXTRA_POST_ID, -1)
            if (postId != -1) {
                this.postId = postId
                this.showComments = it.getBooleanExtra(POST_SHOW_COMMENTS, false)
                this.showTaggedPeople = it.getBooleanExtra(POST_SHOW_TAGGED_PEOPLE, false)
                listenToViewEvents()
                listenToViewModel()
                postDetailViewModel.getPostById(postId)
            } else {
                onBackPressed()
            }
        } ?: onBackPressed()
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION") this.windowManager.defaultDisplay.getMetrics(displayMetrics)

        height = if (displayMetrics.heightPixels >= 1000) {
            displayMetrics.heightPixels - 500
        } else {
            displayMetrics.heightPixels - 400
        }

        homePagePostAdapter = HomePagePostAdapter(this).apply {
            homePagePostViewClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is HomePagePostInfoState.UserProfileClick -> {
                        if (state.postInfo.user?.storyCount == 1) {
                            listOfDataItems?.find { it.userId == state.postInfo.userId }?.user?.storyCount = 0
                            homePagePostAdapter.listOfDataItems = listOfDataItems
                            toggleSelectedStory(
                                this@PostDetailActivity,
                                storyListUtil,
                                state.postInfo.userId
                            )
                        } else if (state.postInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.postInfo.user.id) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        this@PostDetailActivity, 0, state.postInfo.venueTags?.id ?: 0
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    this@PostDetailActivity, state.postInfo.userId
                                )
                            )
                        }
                    }
                    is HomePagePostInfoState.MoreClick -> {
                        Jzvd.goOnPlayOnPause()
                        val showReport = state.postInfo.userId != loggedInUserCache.getUserId()
                        val bottomSheetFragment = PostMoreOptionBottomSheet.newInstance(showReport)
                        bottomSheetFragment.isCancelable = false
                        bottomSheetFragment.postMoreOptionClick.subscribeAndObserveOnMainThread {
                            when (it) {
                                PostMoreOption.BlockClick -> {}
                                PostMoreOption.DeleteClick -> {
                                    Jzvd.goOnPlayOnResume()
                                    bottomSheetFragment.dismissBottomSheet()
                                    postDetailViewModel.deletePost(state.postInfo.id)
                                }
                                PostMoreOption.ReportClick -> {
                                    bottomSheetFragment.dismissBottomSheet()
                                    val reportOptionBottomSheet = ReportBottomSheet()
                                    reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                                        reportOptionBottomSheet.dismiss()
                                        Jzvd.goOnPlayOnResume()
                                        postDetailViewModel.reportPost(state.postInfo.id, reportId)
                                    }.autoDispose()
                                    reportOptionBottomSheet.show(
                                        supportFragmentManager, ReportBottomSheet::class.java.name
                                    )
                                }
                                PostMoreOption.DismissClick -> {
                                    Jzvd.goOnPlayOnResume()
                                    bottomSheetFragment.dismissBottomSheet()
                                }
                            }

                        }.autoDispose()
                        bottomSheetFragment.show(
                            supportFragmentManager, PostMoreOptionBottomSheet::class.java.name
                        )
                    }
                    is HomePagePostInfoState.TaggedPeopleClick -> {
                        openTaggedPeopleBottomSheet(state.postInfo)
                    }
                    is HomePagePostInfoState.OpenTaggedPeopleClick -> {
                        startActivity(NewOtherUserProfileActivity.getIntent(this@PostDetailActivity, state.postInfo.id))
                    }
                    is HomePagePostInfoState.AddPostLikeClick -> {
                        postDetailViewModel.addPostLike(state.postInfo)
                    }
                    is HomePagePostInfoState.RemovePostLikeClick -> {
                        postDetailViewModel.removeLikeFromPost(state.postInfo)
                    }
                    is HomePagePostInfoState.PostLikeCountClick -> {
                        var likesActivity = LikesActivity.newInstanceWithData(state.postInfo.id)
                        likesActivity.show(supportFragmentManager, LikesActivity.javaClass.name)
                    }
                    is HomePagePostInfoState.CommentClick -> {
                        openCommentBottomSheet(state.postInfo)
                    }
                    is HomePagePostInfoState.ShareClick -> {
                        ShareHelper.shareDeepLink(
                            this@PostDetailActivity, true, state.postInfo.id
                        ) {
//                            ShareHelper.shareText(this@PostDetailActivity, it)
                            var sharePostReelBottomSheet = SharePostReelBottomSheet.newInstance(it, state.postInfo.id, "post")
                            sharePostReelBottomSheet.show(supportFragmentManager, SharePostReelBottomSheet.javaClass.name)
                        }
                    }
                    is HomePagePostInfoState.AddBookmarkClick -> {
                        postDetailViewModel.addPostToBookmark(state.postInfo)
                    }
                    is HomePagePostInfoState.RemoveBookmarkClick -> {
                        postDetailViewModel.removePostToBookmark(state.postInfo)
                    }
                    is HomePagePostInfoState.HashtagItemClicks -> {

                    }
                    is HomePagePostInfoState.PhotoViewClick -> {
                        startActivityWithDefaultAnimation(
                            FullScreenActivity.getIntent(
                                this@PostDetailActivity, state.postImageUrl
                            )
                        )
                    }
                    is HomePagePostInfoState.VideoViewClick -> {
                        Timber.tag("mediaVideoViewClick").e(state.postVideoUrl)
                        startActivityWithDefaultAnimation(TempActivity.getIntent(this@PostDetailActivity, state.postVideoUrl,state.postVideoThumbnailUrl))
                    }
                    is HomePagePostInfoState.ChangesVideoPosition -> {
                        Timber.tag("ChangesVideoPosition").e(state.postVideoUrl.toString())
                        Jzvd.goOnPlayOnPause()
                        Observable.timer(500, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            autoPlayVideo()
                        }
                    }
                    else -> {}
                }
            }.autoDispose()
        }
        homePagePostAdapter.deviceHeight = height
        binding.rvHomePagePost.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity, RecyclerView.VERTICAL, false)
            adapter = homePagePostAdapter
        }
    }

    private fun listenToViewModel() {
        postDetailState.subscribeAndObserveOnMainThread {
            when (it) {
                is PostDetailViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is PostDetailViewState.LoadingState -> {
                }
                is PostDetailViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    onBackPressed()
                }
                is PostDetailViewState.GetPostInfo -> {
                    listOfPostInfo.clear()
                    listOfPostInfo.add(it.postInfo)
                    homePagePostAdapter.listOfDataItems = listOfPostInfo
                    if (showComments) {
                        openCommentBottomSheet(it.postInfo)
                    } else if (showTaggedPeople) {
                        openTaggedPeopleBottomSheet(it.postInfo)
                    }
                    Observable.timer(100, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                        autoPlayVideo()
                    }.autoDispose()
                }
                PostDetailViewState.ReloadData -> {
                    loadDataFromIntent()
                }
            }
        }.autoDispose()
    }

    private fun openCommentBottomSheet(postInfo: PostInfo) {
        val bottomSheet = PostCommentBottomSheet(postInfo).apply {
            dismissClick.subscribeAndObserveOnMainThread {
                dismiss()
            }.autoDispose()
        }
        bottomSheet.show(supportFragmentManager, PostCommentBottomSheet::class.java.name)
    }


    private fun autoPlayVideo() {
        if (binding.rvHomePagePost.getChildAt(0) == null) {
            return
        }

        if (binding.rvHomePagePost.getChildAt(0) != null && binding.rvHomePagePost.getChildAt(0)
                .findViewById<ViewPager2>(R.id.viewPager2) != null && binding.rvHomePagePost.getChildAt(0).findViewById<ViewPager2>(R.id.viewPager2)
                .getChildAt(0).findViewById<JzvdStdOutgoer>(R.id.outgoerVideoPlayer) != null
        ) {
            val player: JzvdStdOutgoer? = binding.rvHomePagePost.getChildAt(0).findViewById<ViewPager2>(R.id.viewPager2).getChildAt(0)
                .findViewById(R.id.outgoerVideoPlayer)
            player?.apply {
                Timber.tag("HomeFragment").i("videoUrl :${this.videoUrl}")
                Timber.tag("HomeFragment").i("state :${this.state}")
                player.posterImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                val jzDataSource = JZDataSource(this.videoUrl)
                this.setUp(
                    jzDataSource, Jzvd.SCREEN_TINY, JZMediaExoKotlin::class.java
                )
                Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP)
                startVideoAfterPreloading()
            }
            player?.hideScreenProgress()
        } else {
            println("Any Condition Failed")
        }
    }

    private fun openTaggedPeopleBottomSheet(postInfo: PostInfo) {
        val bottomSheet = PostTaggedPeopleBottomSheet(postInfo)
        bottomSheet.show(supportFragmentManager, PostTaggedPeopleBottomSheet::class.java.name)
    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }

    override fun onResume() {
        Jzvd.goOnPlayOnResume()
        super.onResume()
    }
}