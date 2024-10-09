package com.outgoer.ui.story

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.sponty.model.SpontyJoins
import com.outgoer.api.story.model.MentionUser
import com.outgoer.api.story.model.ViewStoryRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.StoryUserBottomsheetBinding
import com.outgoer.ui.create_story.viewmodel.StoryViewModel
import com.outgoer.ui.create_story.viewmodel.StoryViewState
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.story.view.StoryUserViewAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class StoryUserBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        val TAG: String = "SpontyUserBottomsheet"
        val SPONTY_ID: String = "SPONTY_ID"

        @JvmStatic
        fun newInstance(spontyId: Int): StoryUserBottomSheet {
            val spontyUserBottomsheet = StoryUserBottomSheet()
            val bundle = Bundle()
            bundle.putInt(SPONTY_ID, spontyId)
            spontyUserBottomsheet.arguments = bundle

            return spontyUserBottomsheet
        }
    }

    private var _binding: StoryUserBottomsheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var storyUserViewAdapter: StoryUserViewAdapter

    private var storyId: Int = 0
    private var listofspontyusers: ArrayList<SpontyJoins> = arrayListOf()

    private val storyActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val storyActionState: Observable<String> = storyActionStateSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<StoryViewModel>
    private lateinit var storyViewModel: StoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        storyViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StoryUserBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.halfExpandedRatio = 0.6f
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {
        storyUserViewAdapter = StoryUserViewAdapter(requireContext()).apply {
            storyUserActionState.subscribeAndObserveOnMainThread {
                startActivity(NewOtherUserProfileActivity.getIntent(requireContext(), it.id ?: 0))
            }
        }

        binding.userRecyclerView.apply {
            adapter = storyUserViewAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                storyViewModel.loadMore(ViewStoryRequest(storyId))
                            }
                        }
                    }
                }
            })
        }

        arguments?.let {
            storyId = it.getInt(SPONTY_ID)

            storyViewModel.resetPagination(ViewStoryRequest(storyId))
        }
    }

    private fun listenToViewModel() {
        storyViewModel.storyViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is StoryViewState.ErrorMessage -> {
                    requireActivity().showLongToast(it.errorMessage)
                }

                is StoryViewState.ViewListUserInfo -> {
                    binding.totalViewsAppCompatTextView.text = "${it.listOfMentionUser.size} Viewers"
                    storyUserViewAdapter.listOfViewedUser = it.listOfMentionUser
                    if (it.listOfMentionUser.size ==0) {
                        binding.userRecyclerView.isVisible = false
                        binding.llEmptyState.isVisible = true
                    } else {
                        binding.userRecyclerView.isVisible = true
                        binding.llEmptyState.isVisible = false
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
                    dismissBottomSheet()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    fun dismissBottomSheet() {
        storyActionStateSubject.onNext("1")
        dismiss()
    }

    override fun dismiss() {
        super.dismiss()
    }
}