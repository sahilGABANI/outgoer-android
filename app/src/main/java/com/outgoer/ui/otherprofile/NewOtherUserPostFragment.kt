package com.outgoer.ui.otherprofile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.post.model.PostInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentNewMyPostsBinding
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.home.create.CreateNewReelInfoActivity
import com.outgoer.ui.home.profile.newprofile.view.SpannedGridLayoutManager
import com.outgoer.ui.otherprofile.view.NewOtherUserPostAdapter
import com.outgoer.ui.otherprofile.viewmodel.OtherUserPostViewModel
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import timber.log.Timber
import javax.inject.Inject

class NewOtherUserPostFragment() : BaseFragment() {

    companion object {

        var INTENT_USER_ID = "INTENT_USER_ID"

        @JvmStatic
        fun newInstanceWithData(userId: Int): NewOtherUserPostFragment {
            var newOtherUserPostFragment = NewOtherUserPostFragment()

            val args = Bundle()
            args.putInt(INTENT_USER_ID, userId)

            newOtherUserPostFragment.arguments = args

            return newOtherUserPostFragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<OtherUserPostViewModel>
    private lateinit var otherUserPostViewModel: OtherUserPostViewModel

    private var _binding: FragmentNewMyPostsBinding? = null
    private val binding get() = _binding!!

    private var userId: Int = -1

    private lateinit var otherUserPostAdapter: NewOtherUserPostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        otherUserPostViewModel = getViewModelFromFactory(viewModelFactory)

        arguments?.let {
            userId = it.getInt(INTENT_USER_ID)
        }

        listenToViewEvents()
        listenToViewModel()
        otherUserPostViewModel.resetUserPostPagination(userId)

    }

    private fun listenToViewEvents() {
        binding.btnPost.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionsForCreateReels(false)
        }

        otherUserPostAdapter = NewOtherUserPostAdapter(requireContext())
        otherUserPostAdapter.apply {
            postViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    PostDetailActivity.getIntent(
                        requireContext(),
                        it.id
                    )
                )
            }.autoDispose()
        }
        val manager = SpannedGridLayoutManager(
            SpannedGridLayoutManager.GridSpanLookup { position -> // Conditions for 2x2 items
                if (position == 0 || (position % 12 == 0 || position % 12 == 7)) {
                    SpannedGridLayoutManager.SpanInfo(
                        2,
                        2
                    )
                } else {
                    SpannedGridLayoutManager.SpanInfo(
                        1,
                        1
                    )
                }
            },
            3,  // number of columns
            1f // how big is default item
        )
        binding.rvMyPost.apply {
            layoutManager = manager
//            layoutManager = SpannedGridLayoutManager(3,1f)

            adapter = otherUserPostAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, state: Int) {
                    super.onScrollStateChanged(recyclerView, state)

                    if (state == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager ?: return
                        var lastVisibleItemPosition = 0
                        if (layoutManager is GridLayoutManager) {
                            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        }
                        val adjAdapterItemCount = layoutManager.itemCount
                        if (layoutManager.childCount > 0 && adjAdapterItemCount >= layoutManager.childCount) {
                            otherUserPostViewModel.loadMoreUserPost(userId)
                        }
                    }
                }
            })
        }

        RxBus.listen(RxEvent.RefreshOtherUserProfile::class.java)
            .subscribeOnIoAndObserveOnMainThread({
                otherUserPostViewModel.resetUserPostPagination(userId)
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun checkPermissionsForCreateReels(isReel: Boolean) {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO,
                    Permission.READ_MEDIA_IMAGES
                )
            )
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        startActivity(DeeparEffectsActivity.getIntent(requireContext()))
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }


    private fun listenToViewModel() {
        otherUserPostViewModel.otherUserPostViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is OtherUserPostViewModel.OtherUserPostViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is OtherUserPostViewModel.OtherUserPostViewState.LoadingState -> {
                    binding.progress.isVisible = it.isLoading
                }
                is OtherUserPostViewModel.OtherUserPostViewState.GetAllPostList -> {
                    otherUserPostAdapter.listOfDataItems = it.postInfoList

                    if(it.postInfoList.size == 1)
                        binding.rvMyPost.layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)

                    hideShowNoData(it.postInfoList)
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

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PostCameraActivity.RC_CAPTURE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val filePath = data.getStringExtra(PostCameraActivity.INTENT_EXTRA_FILE_PATH)
                    if (!filePath.isNullOrEmpty()) {
                        startActivityWithDefaultAnimation(
                            CreateNewReelInfoActivity.launchActivity(
                                requireContext(),
                                filePath
                            )
                        )
                    }
                }
            }
        }
    }
}