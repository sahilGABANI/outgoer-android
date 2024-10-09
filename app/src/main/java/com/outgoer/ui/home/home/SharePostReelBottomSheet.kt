package com.outgoer.ui.home.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.SharePostReelsRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.SharePostReelBottomsheetBinding
import com.outgoer.ui.tag.view.TagPeopleAdapter
import com.outgoer.ui.tag.viewmodel.AddTagViewModel
import com.outgoer.utils.ShareHelper
import com.outgoer.utils.UiUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SharePostReelBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        const val SHARE_URL = "SHARE_URL"
        const val POST_ID = "POST_ID"
        const val POST_TYPE = "POST_TYPE"
        const val MSG_FORWARD = "MSG_FORWARD"

        @JvmStatic
        fun newInstance(isMSGForward: Boolean): SharePostReelBottomSheet {
            val sharePostReelBottomSheet = SharePostReelBottomSheet()
            val bundle = Bundle()
            bundle.putBoolean(MSG_FORWARD, isMSGForward)
            sharePostReelBottomSheet.arguments = bundle
            return sharePostReelBottomSheet
        }

        @JvmStatic
        fun newInstance(shareUrl: String, postId: Int, postType: String): SharePostReelBottomSheet {
            val sharePostReelBottomSheet = SharePostReelBottomSheet()
            val bundle = Bundle()
            bundle.putString(SHARE_URL, shareUrl)
            bundle.putInt(POST_ID, postId)
            bundle.putString(POST_TYPE, postType)
            sharePostReelBottomSheet.arguments = bundle
            return sharePostReelBottomSheet
        }
    }

    private val shareOptionClickSubject: PublishSubject<String> = PublishSubject.create()
    val shareOptionClick: Observable<String> = shareOptionClickSubject.hide()

    private val forwardClickSubject: PublishSubject<ArrayList<Int>> = PublishSubject.create()
    val forwardClick: Observable<ArrayList<Int>> = forwardClickSubject.hide()

    private var _binding: SharePostReelBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var shareUrl: String = ""
    private var postId: Int = -1
    private var postType: String = ""

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddTagViewModel>
    private lateinit var addTagViewModel: AddTagViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var tagPeopleAdapter: TagPeopleAdapter
    private var peopleForTagArrayList = ArrayList<FollowUser>()
    private var taggedPeopleHashMap = HashMap<Int, String?>()
    private var isMSGForward: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        addTagViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.share_post_reel_bottomsheet, container, false)
        _binding = SharePostReelBottomsheetBinding.bind(view)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.login_bottom_sheet_background)

        arguments?.let {
            isMSGForward = it.getBoolean(MSG_FORWARD)
            shareUrl = it.getString(SHARE_URL, "")
            postType = it.getString(POST_TYPE, "")
            postId = it.getInt(POST_ID)
        }

        listenToViewEvent()
        listenToViewModel()

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.isDraggable= false
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        binding.closeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            dismissBottomSheet()
        }
        binding.addToStoryRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread { }
        binding.shareRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            ShareHelper.shareText(requireContext(), shareUrl)
        }
        binding.copyLinkRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread {
            val clipboard: ClipboardManager? =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("Copied Text", shareUrl)
            clipboard?.setPrimaryClip(clip)


            requireActivity().showToast("Text copied!!")
        }
        binding.messageRelativeLayout.throttleClicks().subscribeAndObserveOnMainThread { }

        binding.sendsAppCompatTextView.throttleClicks().subscribeAndObserveOnMainThread {
            val userIds = arrayListOf<Int>()
            peopleForTagArrayList.filter { it.isSelected }.let {
                it.forEach { people ->
                    userIds.add(people.id)
                }
            }
            if (isMSGForward) {
                forwardClickSubject.onNext(userIds)
            } else {
                addTagViewModel.getShareReelsPostToChat(SharePostReelsRequest(id = postId, userIds = userIds, type = postType))
            }
        }
    }

    private fun listenToViewEvent() {
        if (isMSGForward) {
            binding.actionFrameLayout.visibility = View.GONE
            binding.actionView.visibility = View.GONE
            binding.actionLinearLayout.visibility = View.GONE
        } else {
            binding.actionFrameLayout.visibility = View.VISIBLE
            binding.actionView.visibility = View.VISIBLE
            binding.actionLinearLayout.visibility = View.VISIBLE
        }
        tagPeopleAdapter = TagPeopleAdapter(requireContext()).apply {
            tagFollowClick.subscribeAndObserveOnMainThread {
                if (it.isSelected) {
                    if (taggedPeopleHashMap.containsKey(it.id)) {
                        taggedPeopleHashMap.remove(it.id)
                    }
                } else {
                    if (!taggedPeopleHashMap.containsKey(it.id)) {
                        taggedPeopleHashMap[it.id] = it.username
                    }
                }
                val mPos = peopleForTagArrayList.indexOf(it)
                if (mPos != -1) {
                    peopleForTagArrayList[mPos].isSelected = !it.isSelected
                }
                tagPeopleAdapter.listOfFollowUser = peopleForTagArrayList

                peopleForTagArrayList.filter { select -> select.isSelected }.let { list ->
                    if (isMSGForward) {
                        binding.actionFrameLayout.visibility = View.VISIBLE
                        binding.sendsAppCompatTextView.isVisible = list.isNotEmpty()
                    } else {
                        binding.sendsAppCompatTextView.isVisible = list.isNotEmpty()
                        binding.actionLinearLayout.isVisible = list.isEmpty()
                    }
                }
            }.autoDispose()
        }

        binding.userInfoRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = tagPeopleAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                addTagViewModel.loadMoreFollowersList(loggedInUserCache.getUserId() ?: 0)
                            }
                        }
                    }
                }
            })
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(requireContext())
            }.autoDispose()

        binding.etSearch.textChanges()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("Search String %s", it.toString())
                if (it.length > 1) {
                    addTagViewModel.searchFollowersList(loggedInUserCache.getUserId() ?: 0, it.toString())
                } else if (it.isEmpty()) {
                    addTagViewModel.searchFollowersList(loggedInUserCache.getUserId() ?: 0, "")
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    shareOptionClickSubject.onNext(shareUrl)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    fun dismissBottomSheet() {
        dismiss()
    }

    private fun listenToViewModel() {
        addTagViewModel.addTagState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddTagViewModel.AddTagViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                 is AddTagViewModel.AddTagViewState.ShareReelsPostToChat -> {

                     shareOptionClickSubject.onNext("1")
                     showLongToast("${postType} shared successfully")
                     dismissBottomSheet()
                 }

                is AddTagViewModel.AddTagViewState.FollowerList -> {
                    val peopleForTagList = it.listOfFollowers
                    if(it.listOfFollowers.isNotEmpty()) {
                        addTagViewModel.loadMoreFollowersList(loggedInUserCache.getUserId() ?: 0)
                    }
                    for (i in peopleForTagList.indices) {
                        if (taggedPeopleHashMap.containsKey(peopleForTagList[i].id)) {
                            peopleForTagList[i].isSelected = true
                        }
                    }
                    peopleForTagArrayList.clear()
                    peopleForTagArrayList.addAll(peopleForTagList)
                    tagPeopleAdapter.listOfFollowUser = peopleForTagArrayList
                    if(peopleForTagList.isEmpty()) {
                        binding.noPeopleAppCompatTextView.visibility = View.GONE
                    } else {
                        binding.noPeopleAppCompatTextView.visibility = View.GONE
                    }
                }

                else -> {}
            }
        }.autoDispose()
    }
}