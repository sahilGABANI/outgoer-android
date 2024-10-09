package com.outgoer.ui.sponty.comment

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.SpontyCommentActionState
import com.outgoer.api.sponty.model.*
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.SpontyReplyBottomsheetBinding
import com.outgoer.ui.comment.PostCommentMoreOptionBottomSheet
import com.outgoer.ui.commenttagpeople.view.CommentTagPeopleAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.sponty.view.SpontyReplyListAdapter
import com.outgoer.ui.sponty.view.SpontyUserAdapter
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.outgoer.ui.story.view.EmojiAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

class SpontyReplyBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        val TAG: String = "SpontyUserBottomSheet"
        val SPONTY_INFO: String = "SPONTY_INFO"

        @JvmStatic
        fun newInstance(spontyId: Int): SpontyReplyBottomSheet {
            val spontyReplyBottomSheet = SpontyReplyBottomSheet()

            val bundle = Bundle()
            bundle.putInt(SPONTY_INFO, spontyId)
            spontyReplyBottomSheet.arguments = bundle

            return spontyReplyBottomSheet
        }
    }

    private val commentActionStateSubject: PublishSubject<Int> = PublishSubject.create()
    val commentActionState: Observable<Int> = commentActionStateSubject.hide()


    private var _binding: SpontyReplyBottomsheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var spontyUserAdapter: SpontyUserAdapter
    private var spontyResponse: ArrayList<SpontyResponse> = arrayListOf()
    private var spontyId: Int = -1

    private var parentId: Int? = null
    private var loggedInUserId by Delegates.notNull<Int>()
    private var commentInfo: SpontyCommentResponse? = null

    private var listofspontyusers: ArrayList<SpontyJoins> = arrayListOf()
    private lateinit var spontyReplyListAdapter: SpontyReplyListAdapter

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SpontyViewModel>
    private lateinit var spontyViewModel: SpontyViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var commentTagPeopleAdapter: CommentTagPeopleAdapter
    private var initialListOfFollower: List<FollowUser> = listOf()
    private lateinit var emojiAdapter: EmojiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)
        spontyViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SpontyReplyBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }

        listenToViewEvents()

        initUI()
        listenToViewModel()
    }

    private fun initUI() {

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

        commentTagPeopleAdapter = CommentTagPeopleAdapter(requireContext()).apply {
            commentTagPeopleClick.subscribeAndObserveOnMainThread { followUser ->
                val cursorPosition: Int = binding.messageEditTextView.selectionStart
                val descriptionString = binding.messageEditTextView.text.toString()
                val subString = descriptionString.subSequence(0, cursorPosition).toString()
                spontyViewModel.searchTagUserClicked(
                    binding.messageEditTextView.text.toString(), subString, followUser
                )
            }.autoDispose()
        }

        binding.rlFollowerList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentTagPeopleAdapter
        }
        binding.messageEditTextView.textChanges()
            // .debounce(400, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeAndObserveOnMainThread {
                if (it.isEmpty()) {
                    binding.rlFollowerList.visibility = View.GONE
                    binding.sendImageView.isVisible = false
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
                            spontyViewModel.getFollowersList(
                                loggedInUserId, lastWord.replace("@", "")
                            )
                        } else {
                            binding.rlFollowerList.visibility = View.GONE
                        }
                    }
                }
            }.autoDispose()

        if (requireActivity() != null) {
            KeyboardVisibilityEvent.setEventListener(requireActivity()) {
                if (!it && parentId != null) {
                    parentId = null
                    binding.messageEditTextView.hint = getString(R.string.type_a_comment)
                }
            }
        }

        spontyViewModel.getInitialFollowersList(loggedInUserId)
    }

    private fun manageComment() {
        if (binding.messageEditTextView.text.isNullOrEmpty()) {
            return
        }
        requireActivity().hideKeyboard(binding.messageEditTextView)
        if (commentInfo != null) {
            spontyViewModel.addSpontyUpdateComments(
                binding.messageEditTextView.text.toString(),
                commentInfo!!,
            )
            Timber.i("update comment")
        } else {
            parentId?.let {
                spontyViewModel.addSpontyReplyComments(
                    spontyId, binding.messageEditTextView.text.toString(), it
                )
                Timber.i("reply comment")
            } ?: run {
                spontyViewModel.addSpontyComments(
                    AddSpontyCommentRequest(
                        spontyId, binding.messageEditTextView.text.toString()
                    )
                )
                Timber.i("add comment")
            }
        }
        binding.messageEditTextView.setText("")
    }

    private fun listenToViewModel() {
        spontyViewModel.spontyDataState.subscribeAndObserveOnMainThread {
            when (it) {
                is SpontyViewModel.SpontyDataState.CommentLoadingState -> {
                    //binding.progressbar.visibility = if(it.isLoading) View.VISIBLE else View.GONE
                }
                is SpontyViewModel.SpontyDataState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is SpontyViewModel.SpontyDataState.SuccessMessage -> {
                    parentId = null
                    commentInfo = null
                }
                is SpontyViewModel.SpontyDataState.EditComment -> {
                    parentId = null
                    commentInfo = null
                }
                is SpontyViewModel.SpontyDataState.UpdateEditTextView -> {
                    binding.messageEditTextView.setText(it.comments)
                    binding.messageEditTextView.setSelection(binding.messageEditTextView.text.toString().length)
                    requireActivity().focusKeyboard(binding.messageEditTextView)
                }
                is SpontyViewModel.SpontyDataState.InitialFollowerList -> {
                    initialListOfFollower = it.listOfFollowers
                }
                is SpontyViewModel.SpontyDataState.FollowerList -> {
                    mentionTagPeopleViewVisibility(!it.listOfFollowers.isNullOrEmpty())
                    commentTagPeopleAdapter.listOfDataItems = it.listOfFollowers
                }
                is SpontyViewModel.SpontyDataState.UpdateDescriptionText -> {
                    mentionTagPeopleViewVisibility(false)
                    binding.messageEditTextView.setText(it.descriptionString)
                    binding.messageEditTextView.setSelection(binding.messageEditTextView.text.toString().length)
                }
                is SpontyViewModel.SpontyDataState.GetAllComments -> {
                    spontyReplyListAdapter.listOfSponty = it.listOfSpontyLikes
                    hideShowNoData(it.listOfSpontyLikes)
                }
                is SpontyViewModel.SpontyDataState.AddComments -> {
                    val listofcomments: ArrayList<SpontyCommentResponse> = spontyReplyListAdapter.listOfSponty as ArrayList<SpontyCommentResponse>
                    listofcomments.add(it.comments)

                    spontyReplyListAdapter.listOfSponty = listofcomments
                    hideShowNoData(listofcomments)
                    binding.messageEditTextView.text?.clear()

                    commentActionStateSubject.onNext(spontyReplyListAdapter.listOfSponty?.size ?: 0)
                }
                is SpontyViewModel.SpontyDataState.SuccessCommentMessage -> {
                    val commentId = it.commentId
                    val listofcomments: ArrayList<SpontyCommentResponse> = spontyReplyListAdapter.listOfSponty as ArrayList<SpontyCommentResponse>
                    listofcomments.removeIf { it.id == commentId }
                    spontyReplyListAdapter.listOfSponty = listofcomments

                    commentActionStateSubject.onNext(spontyReplyListAdapter.listOfSponty?.size ?: 0)
                }

                else -> {}
            }
        }
    }

    private fun mentionTagPeopleViewVisibility(isVisibility: Boolean) {
        if (isVisibility && binding.rlFollowerList.visibility == View.GONE) {
            binding.rlFollowerList.visibility = View.VISIBLE
        } else if (!isVisibility && binding.rlFollowerList.visibility == View.VISIBLE) {
            binding.rlFollowerList.visibility = View.GONE
        }
    }

    private fun hideShowNoData(listOfComment: List<SpontyCommentResponse>) {
        if (listOfComment.isEmpty()) {
            binding.llNoData.visibility = View.VISIBLE
        } else {
            binding.llNoData.visibility = View.GONE
        }
    }


    private fun listenToViewEvents() {
        arguments?.let {
            spontyId = it.getInt(SPONTY_INFO) ?: 0

            spontyViewModel.getAllSpontyComments(SpontyActionRequest(spontyId))

        }

        Glide.with(this).load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar).placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder).into(binding.rivUserProfile)

        binding.ivVerified.isVisible = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profileVerified == 1

        spontyReplyListAdapter = SpontyReplyListAdapter(requireContext()).apply {
            spontyCommentActionState.subscribeAndObserveOnMainThread { state ->
                when (state) {
                    is SpontyCommentActionState.ClickComment -> {
                        val comment = state.commentInfo
                        if (loggedInUserId == state.commentInfo.userId) {
                            val bottomReportSheet = SpontyCommentMoreOptionBottomSheet(comment)
                            bottomReportSheet.bottomReportSheetClicks.subscribeAndObserveOnMainThread { state ->
                                when (state) {
                                    is SpontyCommentMoreOptionState.CancelComment -> {
                                        commentInfo = null
                                        bottomReportSheet.dismissBottomSheet()
                                    }
                                    is SpontyCommentMoreOptionState.DeleteComment -> {
                                        commentInfo = null
                                        bottomReportSheet.dismissBottomSheet()
                                        spontyViewModel.removeComments(comment.id,comment)
                                    }
                                    is SpontyCommentMoreOptionState.EditComment -> {
                                        bottomReportSheet.dismissBottomSheet()
                                        commentInfo = comment
                                        spontyViewModel.clickOnEditText(comment.comment ?: "")
                                    }
                                }
                            }.autoDispose()
                            bottomReportSheet.show(
                                childFragmentManager, PostCommentMoreOptionBottomSheet::class.java.name
                            )
                        }
                    }
                    is SpontyCommentActionState.DisLike -> {
                        spontyViewModel.addSpontyCommentsLike(SpontyCommentActionRequest(commentId = state.commentInfo.id))
                    }
                    is SpontyCommentActionState.Like -> {
                        spontyViewModel.addSpontyCommentsLike(SpontyCommentActionRequest(commentId = state.commentInfo.id))
                    }
                    is SpontyCommentActionState.ReplyComment -> {
                        parentId = state.commentInfo.id
                        requireActivity().focusKeyboard(binding.messageEditTextView)
                        binding.messageEditTextView.hint = getString(
                            R.string.comment_hint, state.commentInfo.user?.username ?: ""
                        )
                    }
                    is SpontyCommentActionState.TaggedUser -> {
                        val loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                        val clickedText = state.clickedText
                        val tagsList = state.commentInfo.tags
                        if (!tagsList.isNullOrEmpty()) {
                            val tag = tagsList.firstOrNull { cInfo ->
                                cInfo.user?.username == clickedText
                            }
                            if (tag != null) {
                                if (loggedInUserId != tag.userId) {
                                    if (tag.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                                        if (loggedInUserCache.getUserId() == tag.userId) {
                                            RxBus.publish(RxEvent.OpenVenueUserProfile)
                                        } else {
                                            startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(), 0, tag.userId ?: 0))
                                        }
                                    } else {
                                        startActivityWithDefaultAnimation(
                                            NewOtherUserProfileActivity.getIntent(
                                                requireContext(), tag.userId
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is SpontyCommentActionState.UserImageClick -> {
                        requireActivity().hideKeyboard()
                        if (state.commentInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.commentInfo.user.id) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0, state.commentInfo.user.id ?: 0
                                    )
                                )
                            }
                        } else {
                            startActivity(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(), state.commentInfo.userId
                                )
                            )
                        }
                    }
                }
            }.autoDispose()
//            removeActionState.subscribeAndObserveOnMainThread {
//                spontyViewModel.removeComments(it.id)
//            }
        }

        binding.replyRecyclerView.apply {
            adapter = spontyReplyListAdapter
        }

        binding.sendImageView.throttleClicks().subscribeAndObserveOnMainThread {
            manageComment()
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


    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION") super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}