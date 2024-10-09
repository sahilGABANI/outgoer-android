package com.outgoer.ui.reeltags

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelTaggedPeopleState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.TaggedPeopleBottomSheetBinding
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reeltags.view.ReelTaggedPeopleAdapter
import com.outgoer.ui.reeltags.viewmodel.ReelTaggedPeopleViewModel
import com.outgoer.ui.reeltags.viewmodel.ReelTaggedPeopleViewState
import javax.inject.Inject

class ReelTaggedPeopleBottomSheet(
    private val reelInfo: ReelInfo
) : BaseBottomSheetDialogFragment() {

    private var _binding: TaggedPeopleBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var reelTaggedPeopleAdapter: ReelTaggedPeopleAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelTaggedPeopleViewModel>
    private lateinit var reelTaggedPeopleViewModel: ReelTaggedPeopleViewModel

    @Inject
    lateinit var loggedInUserCache :LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        reelTaggedPeopleViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tagged_people_bottom_sheet, container, false)
        _binding = TaggedPeopleBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewModel()
        listenToViewEvents()

        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun listenToViewModel() {
        reelTaggedPeopleViewModel.reelTaggedPeopleViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelTaggedPeopleViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ReelTaggedPeopleViewState.LoadingState -> {

                }
                is ReelTaggedPeopleViewState.SuccessMessage -> {

                }
                is ReelTaggedPeopleViewState.GetTaggedPeopleList -> {
                    reelTaggedPeopleAdapter.listOfDataItem = it.listOfTaggedPeople
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        reelTaggedPeopleAdapter = ReelTaggedPeopleAdapter(requireContext())
        reelTaggedPeopleAdapter.apply {
            reelTaggedPeopleClick.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is ReelTaggedPeopleState.UserProfileClick -> {
                        if(state.reelsTagsItem.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if(loggedInUserCache.getUserId() == state.reelsTagsItem.userId ) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            }else {
                                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(),0,state.reelsTagsItem.userId ?: 0))
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    state.reelsTagsItem.userId ?: 0
                                )
                            )
                        }
                    }
                    is ReelTaggedPeopleState.Follow -> {
                        reelTaggedPeopleViewModel.followUnfollow(state.reelsTagsItem)
                    }
                    is ReelTaggedPeopleState.Unfollow -> {
                        reelTaggedPeopleViewModel.followUnfollow(state.reelsTagsItem)
                    }
                }
            }.autoDispose()
        }

        binding.rvTaggedPeopleList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = reelTaggedPeopleAdapter
        }

        reelTaggedPeopleViewModel.getReelTaggedPeople(reelInfo.id)
    }
}