package com.outgoer.ui.home.home.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.HomePageStoryInfoState
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.StoryListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class HomePageStoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val storyViewClickSubject: PublishSubject<HomePageStoryInfoState> = PublishSubject.create()
    val storyViewClick: Observable<HomePageStoryInfoState> = storyViewClickSubject.hide()

    private var binding: StoryListBinding? = null
    private lateinit var storyResponse: ArrayList<StoryListResponse>
    private lateinit var storyAdapter: StoryAdapter
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.story_list, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = StoryListBinding.bind(view)
        binding?.apply {

        }
    }

    fun bind(storyListResponse: ArrayList<StoryListResponse>) {
        this.storyResponse = storyListResponse

        binding?.apply {
            storyAdapter = StoryAdapter(context,loggedInUserCache.getUserId()).apply {
                storyViewClick.subscribeAndObserveOnMainThread {
                    storyViewClickSubject.onNext(it)
                }
            }

            storyRecyclerView.apply {
                adapter = storyAdapter
            }

            storyAdapter.listOfStories = storyResponse
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}