package com.outgoer.ui.home.newReels.comment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.DismissBottomSheet
import com.outgoer.api.reels.model.ReelCommentInfo
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsCommentPageState
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.NewReelsCommentBottomSheetBinding
import com.outgoer.ui.comment.viewmodel.CommentViewState
import com.outgoer.ui.commenttagpeople.view.CommentTagPeopleAdapter
import com.outgoer.ui.home.newReels.comment.view.NewReelsCommentAdapter
import com.outgoer.ui.home.newReels.comment.viewmodel.ReelsCommentViewModel
import com.outgoer.ui.home.newReels.comment.viewmodel.ReelsCommentViewState
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.reels.comment.ReelCommentMoreOptionBottomSheet
import com.outgoer.ui.reels.comment.ReelsCommentMoreOptionState
import com.outgoer.ui.story.view.EmojiAdapter
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class NewReelsCommentBottomSheet(
    private val reelInfo: ReelInfo,
) : BaseBottomSheetDialogFragment() {

    private val reelsCommentIncrementStateSubject: PublishSubject<ReelInfo> =
        PublishSubject.create()
    val reelsCommentIncrementViewState: Observable<ReelInfo> =
        reelsCommentIncrementStateSubject.hide()


    private val dismissClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val dismissClick: Observable<ReelInfo> = dismissClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsCommentViewModel>
    private lateinit var reelsCommentViewModel: ReelsCommentViewModel
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()
    private var _binding: NewReelsCommentBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var newReelsCommentAdapter: NewReelsCommentAdapter
    private var parentId: Int? = null
    private var reelCommentInfo: ReelCommentInfo? = null
    private lateinit var commentTagPeopleAdapter: CommentTagPeopleAdapter
    private var initialListOfFollower: List<FollowUser> = listOf()

    private lateinit var emojiAdapter: EmojiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        reelsCommentViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.new_reels_comment_bottom_sheet, container, false)
        _binding = NewReelsCommentBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.messageEditTextView.requestFocus()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

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
                    requireActivity().hideKeyboard(binding.messageEditTextView)
                    dismissClickSubject.onNext(reelInfo)
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listenToViewEvents() {

        val listOfEmoji = arrayListOf(0x1F602, 0x1F49C, 0x1F622, 0x1F621, 0x1F62E, 0x1F44A, 0x1F525)
        emojiAdapter = EmojiAdapter(requireContext()).apply {
            emojiActionState.subscribeAndObserveOnMainThread {
                binding.messageEditTextView.setText(getEmojiByUnicode(it))
            }
        }

        binding.emojiViewRecyclerView.apply {
            adapter = emojiAdapter
        }

        emojiAdapter.listOfEmoji = listOfEmoji

        Glide.with(this)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar)
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .into(binding.rivUserProfile)

        binding.ivVerified.isVisible = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profileVerified == 1

        newReelsCommentAdapter = NewReelsCommentAdapter(requireContext()).apply {
            reelsCommentPageState.subscribeAndObserveOnMainThread {
                when (it) {
                    is ReelsCommentPageState.DisLike -> {
                        reelsCommentViewModel.removeLikeFromComment(it.reelCommentInfo)
                    }
                    is ReelsCommentPageState.Like -> {
                        reelsCommentViewModel.addLikeToComment(it.reelCommentInfo)
                    }
                    is ReelsCommentPageState.ReplyComment -> {
                        parentId = it.reelCommentInfo.id
                        requireActivity().focusKeyboard(binding.messageEditTextView)
                        binding.messageEditTextView.hint = getString(
                            R.string.comment_hint,
                            it.reelCommentInfo.commentUserInfo?.username ?: ""
                        )
                    }
                    is ReelsCommentPageState.ClickComment -> {
                        val userId = loggedInUserCache.getUserId()
                        val comment = it.reelCommentInfo
                        if (userId == it.reelCommentInfo.userId) {
                            val bottomReportSheet = ReelCommentMoreOptionBottomSheet(comment)
                            bottomReportSheet.bottomReportSheetClicks.subscribeAndObserveOnMainThread { state ->
                                when (state) {
                                    is ReelsCommentMoreOptionState.CancelComment -> {
                                        bottomReportSheet.dismissBottomSheet()
                                    }
                                    is ReelsCommentMoreOptionState.DeleteComment -> {
                                        bottomReportSheet.dismissBottomSheet()
//                                        reelsCommentViewModel.deleteCommentOrReply(comment)
//                                            val builder = AlertDialog.Builder(context)
                                        val builder = AlertDialog.Builder(
                                            ContextThemeWrapper(
                                                context,
                                                R.style.AlertDialogCustom
                                            )
                                        )
                                        builder.setTitle(getString(R.string.label_delete_))
                                        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete))
                                        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, _ ->
                                            reelsCommentViewModel.deleteCommentOrReply(comment)
                                            dialogInterface.dismiss()
                                        }
                                        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
                                            dialogInterface.dismiss()
                                        }
                                        val alertDialog: AlertDialog = builder.create()
                                        alertDialog.setCancelable(false)
                                        alertDialog.show()
                                    }
                                    is ReelsCommentMoreOptionState.EditComment -> {
                                        bottomReportSheet.dismissBottomSheet()
                                        reelCommentInfo = comment
                                        reelsCommentViewModel.clickOnEditText(comment.comment ?: "")
                                    }
                                    else -> {}
                                }
                            }.autoDispose()
                            bottomReportSheet.show(
                                childFragmentManager,
                                ReelCommentMoreOptionBottomSheet::class.java.name
                            )
                        }
                    }
                    is ReelsCommentPageState.UserImageClick -> {
//                        val userId = loggedInUserCache.getUserId()
                        if (it.reelCommentInfo.commentUserInfo?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == it.reelCommentInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0,
                                        it.reelCommentInfo.userId
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    it.reelCommentInfo.userId
                                )
                            )
                        }
                        dismissClickSubject.onNext(reelInfo)
                    }
                    is ReelsCommentPageState.TaggedUser -> {
                        val clickedText = it.clickedText
                        val tagsList = it.reelCommentInfo.tags
                        if (!tagsList.isNullOrEmpty()) {
                            val tag = tagsList.firstOrNull { cInfo ->
                                cInfo.commentUserInfo?.username == clickedText
                            }
                            if (tag != null) {
                                if (tag.commentUserInfo?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                    if (loggedInUserCache.getUserId() == it.reelCommentInfo.userId) {
                                        RxBus.publish(RxEvent.OpenVenueUserProfile)
                                    } else {
                                        startActivityWithDefaultAnimation(
                                            NewVenueDetailActivity.getIntent(
                                                requireContext(), 0,
                                                tag.userId
                                            )
                                        )
                                    }
                                } else {
                                    startActivityWithDefaultAnimation(
                                        NewOtherUserProfileActivity.getIntent(
                                            requireContext(),
                                            tag.userId
                                        )
                                    )
                                }
                                dismissClickSubject.onNext(reelInfo)
                            }
                        }
                    }
                }
            }.autoDispose()
        }

        binding.rvCommentList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newReelsCommentAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                reelsCommentViewModel.loadMore(reelInfo.id)
                            }
                        }
                    }
                }
            })
        }

        reelsCommentViewModel.getAllReelComments(reelInfo.id)

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            manageComment()
        }

        binding.messageEditTextView.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                manageComment()
            }.autoDispose()

        val captionText = if (!reelInfo.caption.isNullOrEmpty()) {
            reelInfo.caption
        } else {
            ""
        }
/*
        val userNameWitCaptionText = SpannableStringBuilder()
            .bold { append(reelInfo.user?.username ?: "") }
            .append(" ")
            .append(captionText)

        binding.etUsernameWithComment.text = userNameWitCaptionText

        binding.etUsernameWithComment.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_SCROLL ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        Glide.with(requireContext())
            .load(reelInfo.user?.avatar ?: "")
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .into(binding.rivUserProfile)
*/

        commentTagPeopleAdapter = CommentTagPeopleAdapter(requireContext()).apply {
            commentTagPeopleClick.subscribeAndObserveOnMainThread { followUser ->
                val cursorPosition: Int = binding.messageEditTextView.selectionStart
                val descriptionString = binding.messageEditTextView.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                reelsCommentViewModel.searchTagUserClicked(
                    binding.messageEditTextView.text.toString(),
                    subString,
                    followUser
                )
            }.autoDispose()
        }

        binding.rlFollowerList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentTagPeopleAdapter
        }

        binding.messageEditTextView.textChanges()
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.sendImageView.isVisible = false
                    binding.rlFollowerList.visibility = View.GONE
                } else {
                    binding.sendImageView.isVisible = true
                    val lastChar = it.last().toString()
                    if (lastChar.contains("@")) {
                        commentTagPeopleAdapter.listOfDataItems = initialListOfFollower
                        binding.rlFollowerList.visibility = View.VISIBLE
                    } else {
                        val wordList = it.split(" ")
                        val lastWord = wordList.last()
                        if (lastWord.contains("@")) {
                            reelsCommentViewModel.getFollowersList(
                                loggedInUserId,
                                lastWord.replace("@", "")
                            )
                        } else {
                            binding.rlFollowerList.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()

        reelsCommentViewModel.getInitialFollowersList(loggedInUserId)

        KeyboardVisibilityEvent.setEventListener(requireActivity()) {
            if (!it && parentId != null) {
                parentId = null
                binding.messageEditTextView.hint = getString(R.string.type_a_comment)
            }
        }
    }

    private fun listenToViewModel() {
        reelsCommentViewModel.reelsCommentViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsCommentViewState.LoadCommentInfo -> {
                    newReelsCommentAdapter.listOfReelsComment = it.listOfComment
                    if (it.listOfComment.isNotEmpty()) {
                        binding.rvCommentList.layoutManager?.let { layoutManager ->
                            if (it.scrollToTop) {
                                (layoutManager as LinearLayoutManager).scrollToPosition(0)
                            }
                        }
                    }
                    hideShowNoData(it.listOfComment)
                }
                is ReelsCommentViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ReelsCommentViewState.DeleteMessage -> {
                    showLongToast(it.deleteMessage)
                }
                is ReelsCommentViewState.UpdateEditTextView -> {
                    binding.messageEditTextView.setText(it.commentText)
                    binding.messageEditTextView.setSelection(binding.messageEditTextView.text.toString().length)
                    requireActivity().focusKeyboard(binding.messageEditTextView)
                }
                is ReelsCommentViewState.UpdateCommentReelInfo -> {
                    it.reelInfo?.let { it1 -> reelsCommentIncrementStateSubject.onNext(it1) }
                }
                is ReelsCommentViewState.EditComment -> {
                    parentId = null
                    reelCommentInfo = null
                }
                is ReelsCommentViewState.SuccessMessage -> {
                    parentId = null
                    reelCommentInfo = null
                }
                is ReelsCommentViewState.InitialFollowerList -> {
                    initialListOfFollower = it.listOfFollowers
                }
                is ReelsCommentViewState.FollowerList -> {
                    mentionTagPeopleViewVisibility(!it.listOfFollowers.isNullOrEmpty())
                    commentTagPeopleAdapter.listOfDataItems = it.listOfFollowers
                }
                is ReelsCommentViewState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.messageEditTextView.setText(it.descriptionString)
                    binding.messageEditTextView.setSelection(binding.messageEditTextView.text.toString().length)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(listOfComment: List<ReelCommentInfo>) {
        if (listOfComment.isEmpty()) {
            binding.llNoData.visibility = View.VISIBLE
        } else {
            binding.llNoData.visibility = View.GONE
        }
    }

    private fun manageComment() {
        if (binding.messageEditTextView.text.isNullOrEmpty()) {
            return
        }
        requireActivity().hideKeyboard(binding.messageEditTextView)
        if (reelCommentInfo != null) {
            reelsCommentViewModel.updateCommentOrReply(
                reelCommentInfo!!,
                binding.messageEditTextView.text.toString()
            )
            Timber.i("update comment")
        } else {
            parentId?.let {
                reelsCommentViewModel.addCommentReply(
                    reelInfo.id,
                    binding.messageEditTextView.text.toString(),
                    it
                )
                Timber.i("reply comment")
            } ?: run {
                reelsCommentViewModel.addComment(
                    reelInfo,
                    binding.messageEditTextView.text.toString()
                )
                Timber.i("add comment")
            }
        }
        binding.messageEditTextView.setText("")
    }

    private fun mentionTagPeopleViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.rlFollowerList.visibility == View.GONE) {
            binding.rlFollowerList.visibility = View.VISIBLE
        } else if (!isVisibility && binding.rlFollowerList.visibility == View.VISIBLE) {
            binding.rlFollowerList.visibility = View.GONE
        }
    }
}