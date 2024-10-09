package com.outgoer.ui.home.search.top

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.api.post.model.MediaObjectType
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.FragmentSearchTopBinding
import com.outgoer.ui.home.search.top.view.SearchTopAdapter
import com.outgoer.ui.home.search.top.viewmodel.SearchTopViewModel
import com.outgoer.ui.home.search.top.viewmodel.SearchTopViewState
import com.outgoer.ui.postdetail.PostDetailActivity
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.utils.SnackBarUtils
import timber.log.Timber
import javax.inject.Inject

class SearchTopFragment : BaseFragment() {

    companion object {
        fun getInstance(): Fragment {
            return SearchTopFragment()
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SearchTopViewModel>
    private lateinit var searchTopViewModel: SearchTopViewModel

    private var _binding: FragmentSearchTopBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchTopAdapter: SearchTopAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchTopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        searchTopViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewEvents()
        listenToViewModel()
        searchTopViewModel.resetTopPostReelPagination(true)
    }

    private fun listenToViewEvents() {

        searchTopAdapter = SearchTopAdapter(requireContext())
        searchTopAdapter.searchTopClick.subscribeAndObserveOnMainThread { myTagBookmarkInfo ->
            myTagBookmarkInfo.objectType?.let {
                if (it == MediaObjectType.POST.type) {
                    startActivityWithDefaultAnimation(
                        PostDetailActivity.getIntent(
                            requireContext(),
                            myTagBookmarkInfo.id
                        )
                    )
                } else if (it == MediaObjectType.Reel.type) {
                    startActivityWithDefaultAnimation(ReelsDetailActivity.getIntent(requireContext(), myTagBookmarkInfo.id))
                }
            }
        }.autoDispose()

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
                                searchTopViewModel.loadMoreTopPostReel()
                            }
                        }
                    }
                }
            })
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            searchTopViewModel.resetTopPostReelPagination(false)
        }.autoDispose()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listenToViewModel() {
        searchTopViewModel.searchTopViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is SearchTopViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("SearchTopViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is SearchTopViewState.SearchTopList -> {
                    searchTopAdapter.listOfDataItems = it.listOfSearchTopData
                    hideShowNoData(it.listOfSearchTopData)
                }
                is SearchTopViewState.LoadingState -> {
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
            binding.llNoData.visibility = View.GONE
        } else {
            binding.llNoData.visibility = View.VISIBLE
        }
    }

}