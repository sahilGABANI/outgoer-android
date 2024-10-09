package com.outgoer.ui.comment

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatSendMessageRequest
import com.outgoer.api.chat.model.MessageType
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.*
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.PostCommentBottomSheetBinding
import com.outgoer.ui.comment.view.PostCommentAdapter
import com.outgoer.ui.comment.viewmodel.CommentViewState
import com.outgoer.ui.comment.viewmodel.PostCommentViewModel
import com.outgoer.ui.commenttagpeople.view.CommentTagPeopleAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.story.view.EmojiAdapter
import com.petersamokhin.android.floatinghearts.HeartsView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class PostCommentBottomSheet(
    private val postInfo: PostInfo
) : BaseBottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<PostCommentViewModel>
    private lateinit var postCommentViewModel: PostCommentViewModel
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()
    private var _binding: PostCommentBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var postCommentAdapter: PostCommentAdapter
    private var parentId: Int? = null
    private var commentInfo: CommentInfo? = null
    private lateinit var commentTagPeopleAdapter: CommentTagPeopleAdapter
    private var initialListOfFollower: List<FollowUser> = listOf()

    private lateinit var emojiAdapter: EmojiAdapter

    private val dismissClickSubject: PublishSubject<DismissBottomSheet> = PublishSubject.create()
    val dismissClick: Observable<DismissBottomSheet> = dismissClickSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        postCommentViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.post_comment_bottom_sheet, container, false)
        _binding = PostCommentBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.messageEditTextView.requestFocus()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
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
                    dismissClickSubject.onNext(DismissBottomSheet.DismissClick)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun listenToViewEvents() {
        Glide.with(this)
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar)
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .into(binding.rivUserProfile)
        binding.ivVerified.isVisible = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profileVerified == 1

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

        //val popup = EmojiPopup.Builder.fromRootView(binding.postCommentLayout).build(binding.messageEditTextView)
        postCommentAdapter = PostCommentAdapter(requireContext()).apply {
            postCommentActionState.subscribeAndObserveOnMainThread {
                when (it) {
                    is PostCommentActionState.DisLike -> {
                        postCommentViewModel.removeLikeFromComment(it.commentInfo)
                    }

                    is PostCommentActionState.Like -> {
                        postCommentViewModel.addLikeToComment(it.commentInfo)
                    }

                    is PostCommentActionState.ReplyComment -> {
                        parentId = it.commentInfo.id
                        requireActivity().focusKeyboard(binding.messageEditTextView)
                        binding.messageEditTextView.hint = getString(
                            R.string.comment_hint,
                            it.commentInfo.commentUserInfo?.username ?: ""
                        )
                    }

                    is PostCommentActionState.ClickComment -> {

                        println("Clicked comment button")
                        val comment = it.commentInfo
                        if (loggedInUserId == it.commentInfo.userId) {
                            val bottomReportSheet = PostCommentMoreOptionBottomSheet(comment)
                            bottomReportSheet.bottomReportSheetClicks.subscribeAndObserveOnMainThread { state ->
                                when (state) {
                                    is PostCommentMoreOptionState.CancelComment -> {
                                        commentInfo = null
                                        bottomReportSheet.dismissBottomSheet()
                                    }
                                    is PostCommentMoreOptionState.DeleteComment -> {
                                        commentInfo = null
                                        bottomReportSheet.dismissBottomSheet()
                                        postCommentViewModel.deleteCommentOrReply(comment,it.replyComment)
                                    }
                                    is PostCommentMoreOptionState.EditComment -> {
                                        bottomReportSheet.dismissBottomSheet()
                                        commentInfo = comment
                                        postCommentViewModel.clickOnEditText(comment.comment ?: "")
                                    }
                                }
                            }.autoDispose()
                            bottomReportSheet.show(
                                childFragmentManager,
                                PostCommentMoreOptionBottomSheet::class.java.name
                            )
                        }
                    }

                    is PostCommentActionState.UserImageClick -> {
                        if (it.commentInfo.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == it.commentInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0,
                                        it.commentInfo.userId
                                    )
                                )
                            }

                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(),
                                    it.commentInfo.userId
                                )
                            )
                        }
                        dismissClickSubject.onNext(DismissBottomSheet.DismissClick)
                    }

                    is PostCommentActionState.TaggedUser -> {
                        val clickedText = it.clickedText
                        val tagsList = it.commentInfo.tags
                        if (!tagsList.isNullOrEmpty()) {
                            val tag = tagsList.firstOrNull { cInfo ->
                                cInfo.commentUserInfo?.username == clickedText
                            }
                            if (tag != null) {
                                if (loggedInUserId != tag.userId) {
                                    if (it.commentInfo.userType == MapVenueUserType.VENUE_OWNER.type) {
                                        if (loggedInUserCache.getUserId() == it.commentInfo.userId) {
                                            RxBus.publish(RxEvent.OpenVenueUserProfile)
                                        } else {
                                            startActivityWithDefaultAnimation(
                                                NewVenueDetailActivity.getIntent(
                                                    requireContext(), 0,
                                                    it.commentInfo.userId
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
                                    dismissClickSubject.onNext(DismissBottomSheet.DismissClick)
                                }
                            }
                        }
                    }
                }
            }.autoDispose()
        }

        binding.rvCommentList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postCommentAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                postCommentViewModel.loadMore(postInfo.id)
                            }
                        }
                    }
                }
            })
        }
        postCommentViewModel.getListOfPostComments(postInfo.id)

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            manageComment()
        }

        binding.messageEditTextView.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                manageComment()
            }.autoDispose()

        val captionText = if (!postInfo.caption.isNullOrEmpty()) {
            postInfo.caption
        } else {
            ""
        }

        commentTagPeopleAdapter = CommentTagPeopleAdapter(requireContext()).apply {
            commentTagPeopleClick.subscribeAndObserveOnMainThread { followUser ->
                val cursorPosition: Int = binding.messageEditTextView.selectionStart
                val descriptionString = binding.messageEditTextView.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                postCommentViewModel.searchTagUserClicked(
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
                            postCommentViewModel.getFollowersList(
                                loggedInUserId,
                                lastWord.replace("@", "")
                            )
                        } else {
                            binding.rlFollowerList.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()

//        binding.photoImageView.throttleClicks().subscribeAndObserveOnMainThread {
//            popup.toggle()
//
//            if(popup.isShowing){
//                binding.photoImageView.setImageResource(R.drawable.ic_happy)
//                binding.photoImageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.purple))
//            } else {
//                binding.photoImageView.setImageResource(R.drawable.ic_keyboard)
//            }
//        }.autoDispose()

        binding.messageEditTextView.clicks().subscribeAndObserveOnMainThread {
            val inputMethodManager = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            inputMethodManager?.showSoftInput(binding.messageEditTextView, InputMethodManager.SHOW_IMPLICIT)
        }.autoDispose()

        postCommentViewModel.getInitialFollowersList(loggedInUserId)

        KeyboardVisibilityEvent.setEventListener(requireActivity()) {
            if (!it && parentId != null) {
                parentId = null
                binding.messageEditTextView.hint = context?.resources?.getString(R.string.type_a_comment)
            }
        }
    }

    private fun listenToViewModel() {
        postCommentViewModel.commentViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is CommentViewState.LoadCommentInfo -> {
                    postCommentAdapter.listOfCommentInfo = it.listOfComment
                    if (it.listOfComment.isNotEmpty()) {
                        binding.rvCommentList.layoutManager?.let { layoutManager ->
                            if (it.scrollToTop) {
                                (layoutManager as LinearLayoutManager).scrollToPosition(0)
                            }
                        }
                    }
                    hideShowNoData(it.listOfComment)
                }
                is CommentViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is CommentViewState.DeleteMessage -> {
                    showLongToast(it.deleteMessage)
                }
                is CommentViewState.UpdateEditTextView -> {
                    binding.messageEditTextView.setText(it.commentText)
                    binding.messageEditTextView.setSelection(binding.messageEditTextView.text.toString().length)
                    requireActivity().focusKeyboard(binding.messageEditTextView)
                }
                is CommentViewState.InitialFollowerList -> {
                    initialListOfFollower = it.listOfFollowers
                }
                is CommentViewState.EditComment -> {
                    parentId = null
                    commentInfo = null
                }
                is CommentViewState.SuccessMessage -> {
                    parentId = null
                    commentInfo = null
                }
                is CommentViewState.FollowerList -> {
                    mentionTagPeopleViewVisibility(!it.listOfFollowers.isNullOrEmpty())
                    commentTagPeopleAdapter.listOfDataItems = it.listOfFollowers
                }
                is CommentViewState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.messageEditTextView.setText(it.descriptionString)
                    binding.messageEditTextView.setSelection(binding.messageEditTextView.text.toString().length)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowNoData(listOfComment: List<CommentInfo>) {
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
        if (commentInfo != null) {
            postCommentViewModel.updateCommentOrReply(
                commentInfo!!,
                binding.messageEditTextView.text.toString()
            )
            Timber.i("update comment")
        } else {
            parentId?.let {
                postCommentViewModel.addCommentReply(
                    postInfo.id,
                    binding.messageEditTextView.text.toString(),
                    it
                )
                Timber.i("reply comment")
            } ?: run {
                postCommentViewModel.addComment(
                    postInfo.id,
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