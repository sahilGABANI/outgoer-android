package com.outgoer.ui.home.profile.newprofile

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
import com.outgoer.ui.home.profile.newprofile.view.NewMyPostAdapter
import com.outgoer.ui.home.profile.newprofile.view.SpannedGridLayoutManager
import com.outgoer.ui.home.profile.viewmodel.MyPostViewModel
import com.outgoer.ui.post.AddNewPostActivity
import com.outgoer.ui.post.AddNewPostInfoActivity
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.utils.SnackBarUtils
import timber.log.Timber
import javax.inject.Inject

class NewMyPostsFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = NewMyPostsFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MyPostViewModel>
    private lateinit var myPostViewModel: MyPostViewModel

    private var _binding: FragmentNewMyPostsBinding? = null
    private val binding get() = _binding!!

    private lateinit var newMyPostAdapter: NewMyPostAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        myPostViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        _binding = FragmentNewMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewEvents()
        listenToViewModel()
        myPostViewModel.resetMyPostPagination()
    }

    private fun listenToViewEvents() {

        binding.btnPost.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionsForCreateReels(false)
        }


        newMyPostAdapter = NewMyPostAdapter(requireContext()).apply {
            postViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(PostDetailActivity.getIntent(requireContext(), it.id))
            }.autoDispose()
        }

        val manager = SpannedGridLayoutManager(
            SpannedGridLayoutManager.GridSpanLookup { position -> // Conditions for 2x2 items
                if (position % 12 == 0 || position % 12 == 7) {
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
            adapter = newMyPostAdapter
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
                            myPostViewModel.loadMoreMyPost()
                        }
                    }

                }
            })
        }

        RxBus.listen(RxEvent.RefreshMyProfile::class.java).subscribeOnIoAndObserveOnMainThread({
            myPostViewModel.resetMyPostPagination()
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun checkPermissionsForCreateReels(isReel: Boolean) {
        XXPermissions.with(this)
            .permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.READ_MEDIA_VIDEO
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
        myPostViewModel.myPostViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is MyPostViewModel.MyPostViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("MyPostViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is MyPostViewModel.MyPostViewState.LoadingState -> {
                    binding.progress.isVisible = it.isLoading
                }
                is MyPostViewModel.MyPostViewState.GetAllMyPostList -> {
                    newMyPostAdapter.listOfDataItems = it.postInfoList
                    
                    println("Size :: " + it.postInfoList.size)

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