package com.outgoer.ui.like

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.post.model.PostLikesUser
import com.outgoer.api.post.model.PostLikesUserPageState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityLikesBinding
import com.outgoer.databinding.AddHashtagBottomSheetBinding
import com.outgoer.ui.group.editgroup.EditGroupBottomSheet
import com.outgoer.ui.like.view.PostLikesAdapter
import com.outgoer.ui.like.viewmodel.PostLikeViewModel
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LikesActivity : BaseBottomSheetDialogFragment() {

    companion object {
        private const val INTENT_EXTRA_POST_ID = "INTENT_EXTRA_POST_ID"
        @JvmStatic
        fun newInstanceWithData(postId: Int): LikesActivity {
            var likesBottomSheet = LikesActivity()

            var bundle = Bundle()
            bundle.putInt(INTENT_EXTRA_POST_ID, postId)

            likesBottomSheet.arguments = bundle

            return likesBottomSheet
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<PostLikeViewModel>
    private lateinit var postLikeViewModel: PostLikeViewModel

    @Inject
    lateinit var loggedInUserCache : LoggedInUserCache

    private var _binding: ActivityLikesBinding? = null
    private val binding get() = _binding!!

    private lateinit var postLikesAdapter: PostLikesAdapter
    private var postLikesUserArrayList = ArrayList<PostLikesUser>()

    private var postId = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityLikesBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        postLikeViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = (view.parent as View)

        dialog?.apply {
            setupFullHeight(bottomSheet)

            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        loadDataFromIntent()
    }


    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    private fun loadDataFromIntent() {
        arguments?.let {
            if (it.getInt(INTENT_EXTRA_POST_ID, -1) > 0) {
                val postId = it.getInt(INTENT_EXTRA_POST_ID, -1)
                if (postId != -1) {
                    this.postId = postId
                    listenToViewEvents()
                    listenToViewModel()
                }
            }
        }
    }

    private fun listenToViewEvents() {
        postLikesUserArrayList = ArrayList()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()

        postLikesAdapter = PostLikesAdapter(requireContext()).apply {
            postLikesViewClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is PostLikesUserPageState.UserProfileClick -> {
                        if(state.postLikesUser.user.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if(loggedInUserCache.getUserId() == state.postLikesUser.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            }else {
                                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(),0,state.postLikesUser.userId ?: 0))
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    state.postLikesUser.user.id
                                )
                            )
                        }
                    }
                    is PostLikesUserPageState.FollowUserClick -> {
                        postLikeViewModel.sendFollowRequest(state.postLikesUser)
                    }
                }
            }.autoDispose()
        }

        binding.rvPostLikesList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = postLikesAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                postLikeViewModel.loadMorePostLikeUser(postId)
                            }
                        }
                    }
                }
            })
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(requireContext())
            }.autoDispose()

        binding.etSearch.textChanges()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("Search String %s", it.toString())
                if (it.length > 1) {
                    postLikeViewModel.searchPostLikeUser(postId, it.toString())
                } else if (it.isEmpty()) {
                    postLikeViewModel.searchPostLikeUser(postId, "")
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun listenToViewModel() {
        postLikeViewModel.postLikeViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is PostLikeViewModel.PostLikeViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is PostLikeViewModel.PostLikeViewState.PostLikesUserList -> {
                    postLikesAdapter.listOfDataItems = it.postLikesUserList

                    binding.noDataLinearLayout.visibility = if(it.postLikesUserList.size > 0) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }

                else -> {}
            }
        }.autoDispose()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
}