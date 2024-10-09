package com.outgoer.ui.livestreamuser.liveuserinfo

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.BottomSheetLiveUserInfoBinding
import com.outgoer.ui.livestreamuser.liveuserinfo.viewmodel.LiveUserInfoViewModel
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class LiveUserInfoBottomSheet(
    private var isHost: Boolean = false,
    private var userId: Int = 0,
    private var isCoHost: Boolean = false,
) : BaseBottomSheetDialogFragment() {

    private val removeUserSubject: PublishSubject<Unit> = PublishSubject.create()
    val removeUser: Observable<Unit> = removeUserSubject.hide()

    private var _binding: BottomSheetLiveUserInfoBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LiveUserInfoViewModel>
    private lateinit var liveUserInfoViewModel: LiveUserInfoViewModel

    private lateinit var outgoerUser: OutgoerUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        liveUserInfoViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetLiveUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewModel()
        listenToViewEvent()

        liveUserInfoViewModel.getUserProfile(userId)
    }

    private fun listenToViewModel() {
        liveUserInfoViewModel.liveUserInfoViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is LiveUserInfoViewModel.LiveUserInfoViewState.LoadingState -> {
                    manageViewVisibility(it.isLoading)
                }
                is LiveUserInfoViewModel.LiveUserInfoViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is LiveUserInfoViewModel.LiveUserInfoViewState.SuccessMessage -> {

                }
                is LiveUserInfoViewModel.LiveUserInfoViewState.LoadUserProfileDetail -> {
                    setUserDetails(it.outgoerUser)
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.ivProfile.throttleClicks().subscribeAndObserveOnMainThread {
            if (!isCoHost) {
                startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(requireContext(), userId))
                dismissBottomSheet()
            }
        }.autoDispose()

        binding.btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
            binding.btnFollow.visibility = View.GONE
            binding.btnFollowing.visibility = View.VISIBLE

            outgoerUser.followStatus = 1
            outgoerUser.totalFollowers = outgoerUser.totalFollowers?.let { it + 1 } ?: 0

            updateFollowStatus()

            liveUserInfoViewModel.followUnfollow(userId)
        }

        binding.btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnFollowing.visibility = View.GONE

            outgoerUser.followStatus = 0
            outgoerUser.totalFollowers = outgoerUser.totalFollowers?.let { it - 1 } ?: 0

            updateFollowStatus()

            liveUserInfoViewModel.followUnfollow(userId)
        }

        binding.btnRemove.throttleClicks().subscribeAndObserveOnMainThread {
            removeUserSubject.onNext(Unit)
        }.autoDispose()
    }

    private fun setUserDetails(outgoerUser: OutgoerUser) {
        this.outgoerUser = outgoerUser

        binding.apply {

            Glide.with(requireContext())
                .load(outgoerUser.avatar ?: "")
                .centerCrop()
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivProfile)

            tvUsername.text = outgoerUser.username ?: ""
            tvDescription.text = outgoerUser.about ?: ""

            if (isHost) {
                btnRemove.visibility = View.VISIBLE
            } else {
                btnRemove.visibility = View.GONE
            }

            updateFollowStatus()
        }
    }

    private fun updateFollowStatus() {
        binding.apply {
            tvFollowersCount.text = "${outgoerUser.totalFollowers?.prettyCount() ?: 0}"
            tvFollowingCount.text = "${outgoerUser.totalFollowing?.prettyCount() ?: 0}"

            val followStatus = outgoerUser.followStatus ?: 0
            if (followStatus == 1) {
                btnFollow.visibility = View.GONE
                btnFollowing.visibility = View.VISIBLE
            } else {
                btnFollow.visibility = View.VISIBLE
                btnFollowing.visibility = View.GONE
            }
        }
    }

    private fun manageViewVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.llContent.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.llContent.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}