package com.outgoer.ui.home.home.view

import android.content.Context
import android.view.View
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.HomeSpontyItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


class HomeSpontyView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val spontyActionStateSubject: PublishSubject<SpontyActionState> = PublishSubject.create()
    val spontyActionState: Observable<SpontyActionState> = spontyActionStateSubject.hide()

    private var binding: HomeSpontyItemBinding? = null
    private lateinit var postInfo: PostInfo
    private lateinit var spontyListAdapter: HomeSpontyAdapter

    var minHeight1 = 0
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.home_sponty_item, this)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = HomeSpontyItemBinding.bind(view)
        binding?.apply {
            spontyRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(@NonNull recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val newHeight = recyclerView.measuredHeight

                    println("NewHeight: " + newHeight)
                    println("minHeight: " + minHeight1)
                    if (0 != newHeight && minHeight1 < newHeight) {
                        // keep track the height and prevent recycler view optimizing by resizing
                        minHeight1 = newHeight
                        recyclerView.minimumHeight = minHeight1
                    }
                }
            })
        }
    }

    fun bind(postInfoResponse: PostInfo) {
        this.postInfo = postInfoResponse

        spontyListAdapter = HomeSpontyAdapter(context).apply {
            spontyActionState.subscribeAndObserveOnMainThread {
                spontyActionStateSubject.onNext(it)
            }
        }

        binding?.apply {
            spontyRecyclerView.adapter = spontyListAdapter
            spontyListAdapter.listOfSponty = postInfoResponse.sponties
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}