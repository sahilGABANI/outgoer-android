package com.outgoer.ui.reels.comment

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.api.reels.model.ReelCommentInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomCommentSheetBinding
import com.outgoer.ui.home.newReels.comment.viewmodel.ReelsCommentViewModel
import com.outgoer.ui.home.newReels.comment.viewmodel.ReelsCommentViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ReelCommentMoreOptionBottomSheet(
    private val reelCommentInfo: ReelCommentInfo
) : BaseBottomSheetDialogFragment() {

    private val bottomReportSheetClicksSubject: PublishSubject<ReelsCommentMoreOptionState> = PublishSubject.create()
    val bottomReportSheetClicks: Observable<ReelsCommentMoreOptionState> = bottomReportSheetClicksSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReelsCommentViewModel>
    private lateinit var reelsCommentViewModel: ReelsCommentViewModel

    private var _binding: BottomCommentSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        reelsCommentViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_comment_sheet, container, false)
        _binding = BottomCommentSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewMode()
        listenToEvent()
    }

    private fun listenToViewMode() {
        reelsCommentViewModel.reelsCommentViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is ReelsCommentViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ReelsCommentViewState.EditComment -> {
                    dismissBottomSheet()
                }
                is ReelsCommentViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToEvent() {
        binding.tvEdit.throttleClicks().subscribeAndObserveOnMainThread {
            bottomReportSheetClicksSubject.onNext(
                ReelsCommentMoreOptionState.EditComment(
                    reelCommentInfo
                )
            )
        }.autoDispose()

        binding.tvDelete.throttleClicks().subscribeAndObserveOnMainThread {
            bottomReportSheetClicksSubject.onNext(ReelsCommentMoreOptionState.DeleteComment)
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            bottomReportSheetClicksSubject.onNext(ReelsCommentMoreOptionState.CancelComment)
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}

sealed class ReelsCommentMoreOptionState {
    data class EditComment(val reelCommentInfo: ReelCommentInfo) : ReelsCommentMoreOptionState()
    object DeleteComment : ReelsCommentMoreOptionState()
    object CancelComment : ReelsCommentMoreOptionState()
}