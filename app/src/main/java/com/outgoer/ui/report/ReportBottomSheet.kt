package com.outgoer.ui.report

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.databinding.BottomSheetReportBinding
import com.outgoer.ui.report.view.ReportReasonAdapter
import com.outgoer.ui.report.viewmodel.ReportReasonState
import com.outgoer.ui.report.viewmodel.ReportReasonViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ReportBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetReportBinding? = null
    private val binding get() = _binding!!

    private val reasonClickSubject: PublishSubject<Int> = PublishSubject.create()
    val reasonClick: Observable<Int> = reasonClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ReportReasonViewModel>
    private lateinit var reportReasonViewModel: ReportReasonViewModel

    private lateinit var reportReasonAdapter: ReportReasonAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        reportReasonViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.login_bottom_sheet_background)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        reportReasonViewModel.reportReasonState.subscribeAndObserveOnMainThread {
            when(it) {
                is ReportReasonState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ReportReasonState.GetReason -> {
                    reportReasonAdapter.listOfDataItem = it.data

                }
                is ReportReasonState.LoadingState -> {
                    binding.rvReportReason.isVisible = !it.isLoading
                    binding.progressbar.isVisible = it.isLoading

                }
                is ReportReasonState.SuccessMessage -> {

                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        reportReasonViewModel.getReportReason()

        reportReasonAdapter = ReportReasonAdapter(requireContext())
        reportReasonAdapter.apply {
            this.reportReasonClick.subscribeAndObserveOnMainThread {
                it.id?.let {id ->
                    reasonClickSubject.onNext(id)
                }
            }.autoDispose()
        }

        binding.rvReportReason.adapter = reportReasonAdapter
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }


}