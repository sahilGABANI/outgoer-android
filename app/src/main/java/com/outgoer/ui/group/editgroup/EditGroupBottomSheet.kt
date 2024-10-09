package com.outgoer.ui.group.editgroup

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.group.model.ManageGroupRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.EditGroupBottomsheetBinding
import com.outgoer.ui.group.viewmodel.GroupViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class EditGroupBottomSheet : BaseBottomSheetDialogFragment() {

    private val removeClickedStateSubject: PublishSubject<String> = PublishSubject.create()
    val removeClicked: Observable<String> = removeClickedStateSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<GroupViewModel>
    private lateinit var groupViewModel: GroupViewModel
    private var chatConversationInfo: ChatConversationInfo? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        val TAG: String = "EditGroupBottomSheet"
        val CHAT_USER_INFO = "CHAT_USER_INFO"
        val CHAT_USER_ID = "CHAT_USER_ID"

        @JvmStatic
        fun newInstance(): EditGroupBottomSheet {
            return EditGroupBottomSheet()
        }

        @JvmStatic
        fun newInstanceWithData(chatConversationInfo: ChatConversationInfo, userId: Int): EditGroupBottomSheet {
            var editGroupBottomSheet = EditGroupBottomSheet()

            var bundle = Bundle()
            bundle.putParcelable(CHAT_USER_INFO, chatConversationInfo)
            bundle.putInt(CHAT_USER_ID, userId)

            editGroupBottomSheet.arguments = bundle

            return editGroupBottomSheet
        }
    }

    private var _binding: EditGroupBottomsheetBinding? = null
    private val binding get() = _binding!!
    private var userId: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        groupViewModel = getViewModelFromFactory(viewModelFactory)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditGroupBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        arguments?.let {
            chatConversationInfo = it.getParcelable<ChatConversationInfo>(CHAT_USER_INFO)
            userId = it.getInt(CHAT_USER_ID, 0)

            chatConversationInfo?.users?.find { it.userId == userId }?.apply {
                binding.removeAppCompatTextView.setText(resources.getString(R.string.label_remove_).plus(" ").plus(username).plus(resources.getString(R.string.from_this_group)))
            }
        }

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {

        binding.makeAdminAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            groupViewModel.setGroupAdmin(ManageGroupRequest(chatConversationInfo?.id, userId))
        }

        binding.removeAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            groupViewModel.removeGroupUser(ManageGroupRequest(chatConversationInfo?.id, userId))
        }

        binding.cancelAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }

    }

    private fun listenToViewModel() {
        groupViewModel.groupState.subscribeAndObserveOnMainThread {
            when (it) {
                is GroupViewModel.GroupViewState.ErrorMessage -> {
                    showLongToast(it.toString())
                }
                is GroupViewModel.GroupViewState.LoadingState -> {

                }
                is GroupViewModel.GroupViewState.SuccessMessage -> {

                }
                is GroupViewModel.GroupViewState.AdminSuccessMessage -> {
                    removeClickedStateSubject.onNext(resources.getString(R.string.edit))
                    dismissBottomSheet()
                }
                is GroupViewModel.GroupViewState.RemoveUserSuccessMessage -> {
                    removeClickedStateSubject.onNext(resources.getString(R.string.label_remove))
                    dismissBottomSheet()
                }

                else -> {}
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

    fun dismissBottomSheet() {
        dismiss()
    }
}