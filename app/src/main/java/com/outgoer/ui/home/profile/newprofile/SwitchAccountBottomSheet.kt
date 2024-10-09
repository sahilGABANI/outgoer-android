package com.outgoer.ui.home.profile.newprofile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.AddUsernameEmail
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.api.profile.model.DeviceAccountRequest
import com.outgoer.api.profile.model.SwitchDeviceAccountRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithFadeInAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.SwitchAccountBottomSheetBinding
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.profile.newprofile.view.SwitchAccountAdapter
import com.outgoer.ui.home.profile.viewmodel.ProfileViewModel
import com.outgoer.ui.onboarding.OnBoardingActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.UUID
import javax.inject.Inject


class SwitchAccountBottomSheet: BaseBottomSheetDialogFragment() {

    private val switchAccountSubject: PublishSubject<String> = PublishSubject.create()
    val switchAccount: Observable<String> = switchAccountSubject.hide()

    companion object {
        @JvmStatic
        fun newInstance(): SwitchAccountBottomSheet {
            var switchAccountBottomSheet = SwitchAccountBottomSheet()

            return switchAccountBottomSheet
        }
    }


    private var _binding: SwitchAccountBottomSheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    private lateinit var profileViewModel: ProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var switchAccountAdapter: SwitchAccountAdapter

    private var listOfSavedAccount: ArrayList<PeopleForTag> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.switch_account_bottom_sheet, container, false)
        _binding = SwitchAccountBottomSheetBinding.bind(view)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToViewEvent()
        listenToViewModel()

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.isDraggable= false
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }

    }

    private fun listenToViewModel() {
        profileViewModel.profileViewStates.subscribeAndObserveOnMainThread {
            when(it) {
                is ProfileViewModel.ProfileViewState.LoadingState -> {
                    binding.progressbar.isVisible = it.isLoading
                }
                is ProfileViewModel.ProfileViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ProfileViewModel.ProfileViewState.GetSavedAccount -> {
                    listOfSavedAccount = ArrayList(it.savedAccountList.reversed())

                    listOfSavedAccount.find { it.id.equals(loggedInUserCache.getUserId()) }?.isSelected = true
                    switchAccountAdapter.listOfDataItems = listOfSavedAccount
                }
                is ProfileViewModel.ProfileViewState.SwitchedUserProfileData -> {
                    switchAccountSubject.onNext("item")
                    dismissBottomSheet()
                }
                else -> {}
            }
        }
    }

    private fun listenToViewEvent() {
        var androidId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        if (androidId.isNullOrEmpty()) {
            androidId = loggedInUserCache.getLoggedInUser()?.loggedInUserToken
        }
        profileViewModel.getDeviceAccount(DeviceAccountRequest(androidId))


        switchAccountAdapter = SwitchAccountAdapter(requireContext()).apply {
            tagPeopleClick.subscribeAndObserveOnMainThread {
                var itemIndex = listOfSavedAccount.indexOf(it)
                listOfSavedAccount.get(itemIndex).isSelected = !listOfSavedAccount.get(itemIndex).isSelected
                switchAccountAdapter.listOfDataItems = listOfSavedAccount
                profileViewModel.switchAccount(SwitchDeviceAccountRequest(androidId, it.id))
            }
        }

        binding.savedAccountRecyclerView.apply {
            adapter = switchAccountAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        binding.addOutgoerAccountLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            startActivity(OnBoardingActivity.getIntent(requireContext()))
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }


    fun dismissBottomSheet() {
        dismiss()
    }

}