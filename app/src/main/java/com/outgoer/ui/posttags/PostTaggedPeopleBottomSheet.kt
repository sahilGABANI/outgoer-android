package com.outgoer.ui.posttags

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
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.post.model.PostTaggedPeopleState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.TaggedPeopleBottomSheetBinding
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.posttags.view.PostTaggedPeopleAdapter
import com.outgoer.ui.posttags.viewmodel.PostTaggedPeopleViewModel
import com.outgoer.ui.posttags.viewmodel.PostTaggedPeopleViewState
import javax.inject.Inject

class PostTaggedPeopleBottomSheet(
    private val postInfo: PostInfo
) : BaseBottomSheetDialogFragment() {

    private var _binding: TaggedPeopleBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var postTaggedPeopleAdapter: PostTaggedPeopleAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<PostTaggedPeopleViewModel>
    private lateinit var postTaggedPeopleViewModel: PostTaggedPeopleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        postTaggedPeopleViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tagged_people_bottom_sheet, container, false)
        _binding = TaggedPeopleBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvents()

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun listenToViewModel() {
        postTaggedPeopleViewModel.postTaggedPeopleViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is PostTaggedPeopleViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is PostTaggedPeopleViewState.LoadingState -> {

                }
                is PostTaggedPeopleViewState.SuccessMessage -> {

                }
                is PostTaggedPeopleViewState.GetTaggedPeopleList -> {
                    postTaggedPeopleAdapter.listOfDataItem = it.listOfTaggedPeople
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        postTaggedPeopleAdapter = PostTaggedPeopleAdapter(requireContext())
        postTaggedPeopleAdapter.apply {
            postTaggedPeopleClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is PostTaggedPeopleState.UserProfileClick -> {
                        if (state.postTagsItem.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            startActivityWithDefaultAnimation(
                                NewVenueDetailActivity.getIntent(
                                    requireContext(),
                                    0,
                                    state.postTagsItem.user?.id ?: 0
                                )
                            )
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    state.postTagsItem.userId ?: 0
                                )
                            )
                        }
                    }
                    is PostTaggedPeopleState.Follow -> {
                        postTaggedPeopleViewModel.followUnfollow(state.postTagsItem)
                    }
                    is PostTaggedPeopleState.Unfollow -> {
                        postTaggedPeopleViewModel.followUnfollow(state.postTagsItem)
                    }
                }
            }.autoDispose()
        }

        binding.rvTaggedPeopleList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = postTaggedPeopleAdapter
        }

        postTaggedPeopleViewModel.getPostTaggedPeople(postInfo.id)
    }
}