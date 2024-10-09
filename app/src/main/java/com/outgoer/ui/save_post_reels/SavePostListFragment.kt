package com.outgoer.ui.save_post_reels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.MediaObjectType
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentSavePostListBinding
import com.outgoer.ui.group.audio.AudioRecordBottomSheet
import com.outgoer.ui.home.search.top.view.SearchTopAdapter
import com.outgoer.ui.home.search.top.viewmodel.SearchTopViewModel
import com.outgoer.ui.home.search.top.viewmodel.SearchTopViewState
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.save_post_reels.view.SavedReelAdapter
import com.outgoer.ui.save_post_reels.viewmodel.SavedPostReelState
import com.outgoer.ui.save_post_reels.viewmodel.SavedPostReelViewModel
import javax.inject.Inject

class SavePostListFragment : BaseFragment() {


    companion object {
        const val TYPE = "TYPE"
        fun newInstance(type: String): SavePostListFragment {
            val savePostListFragment = SavePostListFragment()
            val bundle = Bundle()
            bundle.putString(TYPE, type)
            savePostListFragment.arguments = bundle
            return savePostListFragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SavedPostReelViewModel>
    private lateinit var savedPostReelViewModel: SavedPostReelViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentSavePostListBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchTopAdapter: SearchTopAdapter
    private lateinit var savedReelAdapter: SavedReelAdapter
    private var type = MediaObjectType.POST.type

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSavePostListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val loggedInUserId = loggedInUserCache.getUserId()
        if (loggedInUserId != null) {
            savedPostReelViewModel.resetBookMark(loggedInUserId, type)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        savedPostReelViewModel = getViewModelFromFactory(viewModelFactory)
        type = arguments?.getString(TYPE, "") ?: MediaObjectType.POST.type
        val loggedInUserId = loggedInUserCache.getUserId()
        if (loggedInUserId != null) {
            savedPostReelViewModel.resetBookMark(loggedInUserId, type)
        }
        listenToViewEvents()
        listenToViewModel()
    }


    private fun listenToViewEvents() {
        searchTopAdapter = SearchTopAdapter(requireContext())
        searchTopAdapter.searchTopClick.subscribeAndObserveOnMainThread { myTagBookmarkInfo ->
            myTagBookmarkInfo.objectType?.let {
                if (it == MediaObjectType.POST.type) {
                    startActivityWithDefaultAnimation(
                        PostDetailActivity.getIntent(
                            requireContext(), myTagBookmarkInfo.id
                        )
                    )
                } else if (it == MediaObjectType.Reel.type) {
                    startActivityWithDefaultAnimation(ReelsDetailActivity.getIntent(requireContext(), myTagBookmarkInfo.id))
                }
            }
        }.autoDispose()

        savedReelAdapter = SavedReelAdapter(requireContext())
        savedReelAdapter.searchTopClick.subscribeAndObserveOnMainThread { myTagBookmarkInfo ->
            myTagBookmarkInfo.objectType?.let {
                if (it == MediaObjectType.POST.type) {
                    startActivityWithDefaultAnimation(
                        PostDetailActivity.getIntent(
                            requireContext(), myTagBookmarkInfo.id
                        )
                    )
                } else if (it == MediaObjectType.Reel.type) {
                    startActivityWithDefaultAnimation(ReelsDetailActivity.getIntent(requireContext(), myTagBookmarkInfo.id))
                }
            }
        }.autoDispose()

        if (type == MediaObjectType.POST.type) {
            binding.rvTopSearch.apply {
                layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
                adapter = searchTopAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (dy > 0) {
                            (layoutManager as GridLayoutManager).apply {
                                val visibleItemCount = childCount
                                val totalItemCount = itemCount
                                val pastVisibleItems = findFirstVisibleItemPosition()
                                if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                    val loggedInUserId = loggedInUserCache.getUserId()
                                    if (loggedInUserId != null) {
                                        savedPostReelViewModel.loadBookMark(loggedInUserId, type)
                                    }
                                }
                            }
                        }
                    }
                })
            }
        } else {
            binding.rvTopSearch.apply {
                layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
                adapter = savedReelAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (dy > 0) {
                            (layoutManager as GridLayoutManager).apply {
                                val visibleItemCount = childCount
                                val totalItemCount = itemCount
                                val pastVisibleItems = findFirstVisibleItemPosition()
                                if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                    val loggedInUserId = loggedInUserCache.getUserId()
                                    if (loggedInUserId != null) {
                                        savedPostReelViewModel.loadBookMark(loggedInUserId, type)
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            val loggedInUserId = loggedInUserCache.getUserId()
            if (loggedInUserId != null) {
                savedPostReelViewModel.resetBookMark(loggedInUserId, type)
            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        savedPostReelViewModel.savedPostReelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is SavedPostReelState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is SavedPostReelState.GetAllPostList -> {
                    if (type == MediaObjectType.POST.type) {
                        it.postInfoList.forEach {
                            it.isSavePost = true
                        }
                        searchTopAdapter.listOfDataItems = it.postInfoList
                        hideShowNoData(it.postInfoList)
                    } else {
                        savedReelAdapter.listOfDataItems = it.postInfoList
                        hideShowNoData(it.postInfoList)
                    }
                }
                is SavedPostReelState.LoadingState -> {
                    if (it.isLoading) {
                        binding.progressbar.visibility = View.VISIBLE
                    } else {
                        binding.progressbar.visibility = View.GONE
                    }

                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(tagBookmarkInfoList: List<MyTagBookmarkInfo>) {
        if (tagBookmarkInfoList.isNotEmpty()) {
            if (type == MediaObjectType.POST.type) {
                binding.llNoPostData.visibility = View.GONE
            } else {
                binding.llNoReelsData.visibility = View.GONE
            }
        } else {
            if (type == MediaObjectType.POST.type) {
                binding.llNoPostData.visibility = View.VISIBLE
            } else {
                binding.llNoReelsData.visibility = View.VISIBLE
            }
        }
    }


}