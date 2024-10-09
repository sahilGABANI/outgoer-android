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
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentNewMyReelBinding
import com.outgoer.ui.deepar.DeeparEffectsActivity
import com.outgoer.ui.home.create.CreateNewReelInfoActivity
import com.outgoer.ui.home.newReels.hashtag.PlayReelsByHashtagActivity
import com.outgoer.ui.home.profile.newprofile.view.NewMyReelsAdapter
import com.outgoer.ui.home.profile.newprofile.view.SpannedGridLayoutManager
import com.outgoer.ui.post.PostCameraActivity
import com.outgoer.ui.reelsdetail.viewmodel.ReelsDetailViewModel
import com.outgoer.utils.SnackBarUtils
import timber.log.Timber
import javax.inject.Inject

class NewMyReelFragment : BaseFragment() {
    companion object {

        var INTENT_USER_ID = "INTENT_USER_ID"

        @JvmStatic
        fun newInstance() = NewMyReelFragment()


        @JvmStatic
        fun newInstanceWithData(userId: Int): NewMyReelFragment {
            val newMyReelFragment = NewMyReelFragment()
            val args = Bundle()
            args.putInt(INTENT_USER_ID, userId)
            newMyReelFragment.arguments = args
            return newMyReelFragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsDetailViewModel>
    private lateinit var reelsDetailViewModel: ReelsDetailViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentNewMyReelBinding? = null
    private val binding get() = _binding!!

    private lateinit var newMyReelsAdapter: NewMyReelsAdapter

    private var userId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewMyReelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        reelsDetailViewModel = getViewModelFromFactory(viewModelFactory)


        listenToViewEvents()
        loadIntent()
        listenToViewModel()
        reelsDetailViewModel.pullToRefresh(userId)

    }

    private fun loadIntent() {
        if (arguments == null) {
            userId = loggedInUserCache.getUserId() ?: -1
        } else {
            arguments?.let {
                userId = it.getInt(INTENT_USER_ID)
            }
        }
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
    private fun listenToViewEvents() {

        binding.btnPost.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermissionsForCreateReels(true)
        }

        newMyReelsAdapter = NewMyReelsAdapter(requireContext()).apply {
            postViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    PlayReelsByHashtagActivity.getIntent(
                        requireContext(),
                        newMyReelsAdapter.listOfDataItems as ArrayList<ReelInfo>, it
                    )
                )
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
            adapter = newMyReelsAdapter

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
                            reelsDetailViewModel.loadMore(userId)
                        }
                    }
                }
            })
        }

        RxBus.listen(RxEvent.RefreshMyProfile::class.java).subscribeOnIoAndObserveOnMainThread({
            reelsDetailViewModel.pullToRefresh(userId)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun listenToViewModel() {
        reelsDetailViewModel.reelsDetailViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsDetailViewModel.ReelsDetailViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("ReelsDetailViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is ReelsDetailViewModel.ReelsDetailViewState.LoadingState -> {
                    binding.progress.isVisible = it.isLoading
                }
                is ReelsDetailViewModel.ReelsDetailViewState.GetAllReelsInfo -> {
                    newMyReelsAdapter.listOfDataItems = it.listOfReelsInfo
                    if(it.listOfReelsInfo.size == 1)
                        binding.rvMyPost.layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
                    hideShowNoData(it.listOfReelsInfo)
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(postInfoList: List<ReelInfo>) {
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