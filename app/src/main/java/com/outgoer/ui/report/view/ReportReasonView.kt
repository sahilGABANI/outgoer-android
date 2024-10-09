package com.outgoer.ui.report.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.post.model.ReportReason
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewReportReasonBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportReasonView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reportReasonClickSubject: PublishSubject<ReportReason> = PublishSubject.create()
    val reportReasonClick: Observable<ReportReason> = reportReasonClickSubject.hide()

    private lateinit var binding: ViewReportReasonBinding
    private lateinit var reportReason: ReportReason

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_report_reason, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewReportReasonBinding.bind(view)

        binding.apply {
            tvReason.throttleClicks().subscribeAndObserveOnMainThread {
                reportReasonClickSubject.onNext(reportReason)
            }.autoDispose()

        }
    }

    fun bind(reportReason: ReportReason) {
        this.reportReason = reportReason
        binding.apply {
            tvReason.text = reportReason.title.toString()

        }
    }

}