package com.outgoer.ui.sponty

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.ReportSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.Outgoer
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.startActivityWithFadeInAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentSpontyListBinding
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import com.outgoer.ui.newvenuedetail.FullScreenActivity
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.ui.sponty.comment.SpontyReplyBottomSheet
import com.outgoer.ui.sponty.view.SpontyListAdapter
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import com.outgoer.ui.temp.TempActivity
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SpontyListFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = SpontyListFragment()
    }

    private var _binding: FragmentSpontyListBinding? = null
    private val binding get() = _binding!!

    private lateinit var spontyListAdapter: SpontyListAdapter
    private var listofsponty: ArrayList<SpontyResponse> = arrayListOf()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SpontyViewModel>
    private lateinit var spontyViewModel: SpontyViewModel
    var index: Int = -1
    private lateinit var spontyListContext: Context

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        spontyViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSpontyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spontyListContext = view.context
        listenToViewEvents()
        listenToViewModel()

        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "Sponty") {
                lifecycleScope.launch {
                    delay(1000)
                    spontyViewModel.resetPagination(false)
                }
                binding.spontyRecyclerView.scrollToPosition(0)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    override fun onResume() {
        super.onResume()
        spontyViewModel.resetPagination(true)

        val icLogoPlaceholder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_chat_user_placeholder)
        Glide.with(requireContext())
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar)
            .placeholder(icLogoPlaceholder)
            .into(binding.ivProfile)
    }

    private fun listenToViewEvents() {
        val icLogoPlaceholder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_chat_user_placeholder)
        Glide.with(requireContext())
            .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar)
            .placeholder(icLogoPlaceholder)
            .into(binding.ivProfile)

        binding.addAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                startActivityWithFadeInAnimation(CreateSpontyActivity.getIntent(requireContext(), ""))
        }

        spontyListAdapter = SpontyListAdapter(requireContext()).apply {
            spontyActionState.subscribeAndObserveOnMainThread { state ->
                when(state) {
                    is SpontyActionState.ReportSponty -> {
                        val postMoreOptionBottomSheet: PostMoreOptionBottomSheet = PostMoreOptionBottomSheet.newInstanceWithData(true, true)
                        postMoreOptionBottomSheet.postMoreOptionClick.subscribeAndObserveOnMainThread {
                            when(it) {
                                is PostMoreOption.ReportClick -> {
                                    val reportOptionBottomSheet = ReportBottomSheet()
                                    reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                                        spontyViewModel.spontyReport(ReportSpontyRequest(state.commentInfo.id, reportId))
                                        reportOptionBottomSheet.dismiss()

                                        postMoreOptionBottomSheet.dismissBottomSheet()
                                    }.autoDispose()
                                    reportOptionBottomSheet.show(
                                        childFragmentManager, ReportBottomSheet::class.java.name
                                    )

                                }
                                is PostMoreOption.DismissClick -> {
                                    postMoreOptionBottomSheet.dismissBottomSheet()
                                }
                                else -> {}
                            }
                        }

                        postMoreOptionBottomSheet.show(childFragmentManager, PostMoreOptionBottomSheet.javaClass.name)
                    }
                    is SpontyActionState.LocationSpontyClick -> {
                        val navigationIntentUri =  Uri.parse("geo:0,0?q=" + Uri.encode(state.commentInfo.location))

                        val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        startActivity(mapIntent)
                    }
                    is SpontyActionState.CommentClick -> {
                        val spontyId = state.commentInfo.id
                        val spontyReplyBottomSheet = SpontyReplyBottomSheet.newInstance(state.commentInfo.id)
                        spontyReplyBottomSheet.commentActionState.subscribeAndObserveOnMainThread { res ->
                            listofsponty.find { item -> item.id == spontyId }?.apply {
                                totalComments = res
                            }

                            spontyListAdapter.listOfSponty = listofsponty
                        }
                        spontyReplyBottomSheet.show(childFragmentManager, "SpontyReplyBottomSheet")
                    }
                    is SpontyActionState.LikeDisLike ->{
                        state.commentInfo.id.let { it1 ->
                            spontyViewModel.addRemoveSpontyLike(SpontyActionRequest(it1))
                        }
                    }
                    is SpontyActionState.JoinUnJoinClick -> {
                        index = listofsponty.indexOf(state.spontyResponse)
                        spontyViewModel.addRemoveSponty(AllJoinSpontyRequest(spontyId = state.spontyResponse.id))
                    }
                    is SpontyActionState.TaggedUser -> {
                        val loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id
                        val clickedText = state.clickedText
                        val tagsList = state.commentInfo.spontyTags ?: arrayListOf()
                        tagsList.addAll(state.commentInfo.descriptionTags ?: arrayListOf())

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
                                            startActivityWithDefaultAnimation(
                                                NewVenueDetailActivity.getIntent(
                                                    requireContext(),
                                                    0,
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
                                }
                            }
                        }
                    }
                    is SpontyActionState.UserImageClick -> {
                        if (state.commentInfo.user?.storyCount == 1) {
                            listofsponty.find { it.userId == state.commentInfo.userId }?.user?.storyCount = 0
                            spontyListAdapter.listOfSponty = listofsponty
                            toggleSelectedStory(
                                spontyListContext,
                                storyListUtil,
                                state.commentInfo.userId
                            )
                        } else if (state.commentInfo.user?.userType == MapVenueUserType.VENUE_OWNER.type) {
                            if (loggedInUserCache.getUserId() == state.commentInfo.userId) {
                                RxBus.publish(RxEvent.OpenVenueUserProfile)
                            } else {
                                startActivityWithDefaultAnimation(
                                    NewVenueDetailActivity.getIntent(
                                        requireContext(), 0, state.commentInfo.userId
                                    )
                                )
                            }
                        } else {
                            startActivityWithDefaultAnimation(
                                NewOtherUserProfileActivity.getIntent(
                                    requireContext(), state.commentInfo.userId
                                )
                            )
                        }
                    }
                    is SpontyActionState.VideoViewClick -> {
                        Outgoer.exoCacheManager.prepareCacheVideo(state.postVideoUrl.plus("?clientBandwidthHint=2.5"))
                        startActivityWithDefaultAnimation(TempActivity.getIntent(requireContext(), state.postVideoUrl,state.postVideoThumbnailUrl))
                    }
                    is SpontyActionState.ImageClick -> {
                        requireActivity().startActivity(FullScreenActivity.getIntent(requireContext(), state.imageUrl))
                    }
                    is SpontyActionState.VenueClick -> {
//                        val navigationIntentUri =  Uri.parse("google.navigation:q=" + state.commentInfo.latitude + "," + state.commentInfo.longitude)
//
//                        val mapIntent = Intent(Intent.ACTION_VIEW, navigationIntentUri)
//                        mapIntent.setPackage("com.google.android.apps.maps")
//                        startActivity(mapIntent)

                        startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, state.commentInfo.venueTags?.id ?: 0))
                    }
                    is SpontyActionState.CheckAction -> {
                        startActivity(NewVenueDetailActivity.getIntent(requireContext(), 0, state.commentInfo.venueTags?.id ?: 0))
                    }
                    is SpontyActionState.DeleteSponty -> {
                        openDeletePopup(state)
                    }
                    else -> {

                    }
                }
            }
        }

        spontyListAdapter.userId = loggedInUserCache.getUserId() ?: -1

        binding.spontyRecyclerView.apply {
            adapter = spontyListAdapter
        }

        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            binding.swipeRefreshLayout.isRefreshing = false
            spontyViewModel.resetPagination(false)
        }.autoDispose()
    }
    private fun openDeletePopup(state: SpontyActionState.DeleteSponty) {
//        val builder = AlertDialog.Builder(context)
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete_sponty))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            listofsponty.remove(state.commentInfo)
            spontyViewModel.removeSponty(state.commentInfo.id)
            spontyListAdapter.listOfSponty = listofsponty
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun listenToViewModel() {
        spontyViewModel.spontyDataState.subscribeAndObserveOnMainThread {
            when (it) {
                is SpontyViewModel.SpontyDataState.LoadingState -> {

                    if(listofsponty.size == 0) {
                        if (it.isLoading) {
                            binding.progressbar.visibility = View.VISIBLE
                        } else {
                            binding.progressbar.visibility = View.GONE
                        }
                    }

                }
                is SpontyViewModel.SpontyDataState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("SpontyDataState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is SpontyViewModel.SpontyDataState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is SpontyViewModel.SpontyDataState.SuccessReportMessage -> {
                    showToast(it.successMessage)
                }
                is SpontyViewModel.SpontyDataState.ListofSponty -> {
                    binding.progressbar.visibility = View.GONE
                    listofsponty.clear()
                    listofsponty.addAll(it.spontyData)
                    spontyListAdapter.listOfSponty = it.spontyData
                }
                is SpontyViewModel.SpontyDataState.SpontyInfo -> {

                }
                is SpontyViewModel.SpontyDataState.SpecificSpontyInfo -> {

                }
                is SpontyViewModel.SpontyDataState.SpontyJoinInfo -> {

                }
                is SpontyViewModel.SpontyDataState.AddSpontyJoin -> {
                    listofsponty[index].spontyJoin = it.joinStatus != 0

//                    spontyListAdapter.listOfSponty = listofsponty
                }
                else -> {}
            }
        }
    }
}