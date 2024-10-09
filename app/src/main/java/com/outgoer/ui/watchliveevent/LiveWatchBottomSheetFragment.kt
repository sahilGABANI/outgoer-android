package com.outgoer.ui.watchliveevent

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.outgoer.R
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomSheetLiveWatchingUserBinding
import com.outgoer.ui.watchliveevent.view.WatchUserListAdapter
import com.outgoer.ui.watchliveevent.viewmodel.LiveWatchingUserViewModel
import javax.inject.Inject

class LiveWatchBottomSheetFragment(
    private val liveId: Int
) : BaseBottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveWatchingUserViewModel>
    private lateinit var liveWatchingUserViewModel: LiveWatchingUserViewModel

    private var _binding: BottomSheetLiveWatchingUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var watchUserListAdapter: WatchUserListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        liveWatchingUserViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetLiveWatchingUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvent()

        dialog?.apply {

            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun listenToViewModel() {
        liveWatchingUserViewModel.liveStreamingStates.subscribeAndObserveOnMainThread { state ->
            when (state) {
                is LiveWatchingUserViewModel.LiveStreamingViewState.ErrorMessage -> {
                    showToast(state.errorMessage)
                }
                is LiveWatchingUserViewModel.LiveStreamingViewState.LoadingState -> {

                }
                is LiveWatchingUserViewModel.LiveStreamingViewState.LiveJoinUser -> {
                    watchUserListAdapter.listOfDataItems = state.listUserJoin
                    if (state.listUserJoin.isNotEmpty()) {
                        binding.llNoData.visibility = View.GONE
                    } else {
                        binding.llNoData.visibility = View.VISIBLE
                    }
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }.autoDispose()

        watchUserListAdapter = WatchUserListAdapter(requireContext())
        watchUserListAdapter.apply {
            watchUsersViewClicks.subscribeAndObserveOnMainThread {
            }
        }
        binding.rvUserList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = watchUserListAdapter
        }
        liveWatchingUserViewModel.liveJoinUserEvent(liveId)
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}