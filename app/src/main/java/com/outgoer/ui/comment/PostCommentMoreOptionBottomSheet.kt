package com.outgoer.ui.comment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.api.post.model.CommentInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomCommentSheetBinding
import com.outgoer.ui.comment.viewmodel.PostCommentViewModel
import com.outgoer.ui.comment.viewmodel.CommentViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class PostCommentMoreOptionBottomSheet(
    private val commentInfo: CommentInfo
) : BaseBottomSheetDialogFragment() {

    private val bottomReportSheetClicksSubject: PublishSubject<PostCommentMoreOptionState> = PublishSubject.create()
    val bottomReportSheetClicks: Observable<PostCommentMoreOptionState> = bottomReportSheetClicksSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<PostCommentViewModel>
    private lateinit var postCommentViewModel: PostCommentViewModel

    private var _binding: BottomCommentSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        postCommentViewModel = getViewModelFromFactory(viewModelFactory)
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
        postCommentViewModel.commentViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is CommentViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is CommentViewState.EditComment -> {
                    dismissBottomSheet()
                }
                is CommentViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToEvent() {
        binding.tvEdit.throttleClicks().subscribeAndObserveOnMainThread {
            bottomReportSheetClicksSubject.onNext(PostCommentMoreOptionState.EditComment(commentInfo))
        }.autoDispose()

        binding.tvDelete.throttleClicks().subscribeAndObserveOnMainThread {

            openDeletePopup()
            dismissBottomSheet()
        }.autoDispose()

        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            bottomReportSheetClicksSubject.onNext(PostCommentMoreOptionState.CancelComment)
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }

    private fun openDeletePopup() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
        builder.setTitle(getString(R.string.label_delete_))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete_comment))
        builder.setPositiveButton(getString(R.string.delete)) { dialogInterface, which ->
            bottomReportSheetClicksSubject.onNext(PostCommentMoreOptionState.DeleteComment)
            dialogInterface.dismiss()
        }
        builder.setNeutralButton(getString(R.string.label_cancel)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}

sealed class PostCommentMoreOptionState {
    data class EditComment(val commentInfo: CommentInfo) : PostCommentMoreOptionState()
    object DeleteComment : PostCommentMoreOptionState()
    object CancelComment : PostCommentMoreOptionState()
}