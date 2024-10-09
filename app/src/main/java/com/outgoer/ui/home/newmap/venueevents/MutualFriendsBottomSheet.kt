package com.outgoer.ui.home.newmap.venueevents

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.api.event.model.MutualFriends
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.MutualFriendsBottomsheetBinding
import com.outgoer.ui.home.newmap.venueevents.view.MutualFriendsAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MutualFriendsBottomSheet(val mutualFriendsList: ArrayList<MutualFriends>) :
    BaseBottomSheetDialogFragment() {

    private val addedHashTagsClicksSubject: PublishSubject<ArrayList<MutualFriends>> =
        PublishSubject.create()
    val addedHashTagsClicks: Observable<ArrayList<MutualFriends>> =
        addedHashTagsClicksSubject.hide()

    private var _binding: MutualFriendsBottomsheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var mutualFriendsAdapter: MutualFriendsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MutualFriendsBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        loadData()
    }

    private fun loadData() {
        mutualFriendsAdapter = MutualFriendsAdapter(requireContext()).apply {
            profileViewClick.subscribeAndObserveOnMainThread {
                if (MapVenueUserType.VENUE_OWNER.type.equals(it.userType)) {
                    startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, it.id))
                } else {
                    startActivity(
                        NewOtherUserProfileActivity.getIntent(
                            requireContext(),
                            it.userId
                        )
                    )
                }
            }
        }
        binding.mutualFriendsRecyclerView.apply {
            adapter = mutualFriendsAdapter
        }

        mutualFriendsAdapter.listOfMutualFriends = mutualFriendsList
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}