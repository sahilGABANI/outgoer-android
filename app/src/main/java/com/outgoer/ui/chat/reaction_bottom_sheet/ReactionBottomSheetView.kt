package com.outgoer.ui.chat.reaction_bottom_sheet

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.chat.model.Reaction
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.ReactionBottomSheetViewBinding
import com.outgoer.ui.chat.reaction_bottom_sheet.view.ReactedUsersAdapter
import com.outgoer.ui.chat.viewmodel.ChatMessageViewModel
import com.outgoer.ui.chat.viewmodel.ChatMessageViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ReactionBottomSheetView : BaseBottomSheetDialogFragment() {

    companion object {
        private const val MESSAGE_ID = "MESSAGE_ID"
        private const val LOGGED_IN_USER_ID = "LOGGED_IN_USER_ID"

        @JvmStatic
        fun newInstance(messageId: Int, loggedInUserId: Int): ReactionBottomSheetView {
            val reactionBottomSheetView = ReactionBottomSheetView()
            val bundle = Bundle()
            bundle.putInt(MESSAGE_ID, messageId)
            bundle.putInt(LOGGED_IN_USER_ID, loggedInUserId)
            reactionBottomSheetView.arguments = bundle
            return reactionBottomSheetView
        }
    }

    private var _binding: ReactionBottomSheetViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var reactedUsersAdapter: ReactedUsersAdapter
    private lateinit var reactionContext: Context
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChatMessageViewModel>
    private lateinit var chatMessageViewModel: ChatMessageViewModel
    private var messageId: Int = -1
    private var loggedInUserId: Int = -1

    private val reactionClickSubject: PublishSubject<Reaction> = PublishSubject.create()
    val reactionClick: Observable<Reaction> = reactionClickSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        chatMessageViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.reaction_bottom_sheet_view, container, false)
        _binding = ReactionBottomSheetViewBinding.bind(view)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reactionContext = view.context
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.login_bottom_sheet_background)

        arguments?.let {
            messageId = it.getInt(MESSAGE_ID,0)
            loggedInUserId = it.getInt(LOGGED_IN_USER_ID,0)
            if (messageId != -1) chatMessageViewModel.resetReactionPagination(messageId)
        }
        listenToViewEvent()
        listenToViewModel()

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
    }

    private fun listenToViewModel() {
        chatMessageViewModel.messageViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ChatMessageViewState.LoadingReactionState -> {
                    if (it.isLoading) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.userInfoRecyclerView.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.userInfoRecyclerView.visibility = View.VISIBLE
                    }
                }
                is ChatMessageViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e(it.errorMessage)
                }
                is ChatMessageViewState.ChatReactionList -> {
                    reactedUsersAdapter.listOfDataItems = it.listOfChatReactionInfo
                }
                else -> {}
            }
        }
    }

    private fun listenToViewEvent() {
        reactedUsersAdapter = ReactedUsersAdapter(requireContext(), loggedInUserId).apply {
            reactionClick.subscribeAndObserveOnMainThread {
                reactionClickSubject.onNext(it)
                dismiss()
            }.autoDispose()
        }

        binding.userInfoRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = reactedUsersAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                chatMessageViewModel.loadReactionMore(messageId)
                            }
                        }
                    }
                }
            })
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    //TO-DO
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
}