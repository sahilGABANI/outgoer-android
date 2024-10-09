package com.outgoer.ui.home.home.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.HomePageStoryInfoState
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.StoryListItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class StoryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val storyViewClickSubject: PublishSubject<HomePageStoryInfoState> = PublishSubject.create()
    val storyViewClick: Observable<HomePageStoryInfoState> = storyViewClickSubject.hide()

    private var binding: StoryListItemBinding? = null
    private lateinit var storyResponse: StoryListResponse

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)

        val view = View.inflate(context, R.layout.story_list_item, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = StoryListItemBinding.bind(view)
        binding?.apply {
            userProfileRoundedImageView.throttleClicks().subscribeAndObserveOnMainThread {
                storyViewClickSubject.onNext(HomePageStoryInfoState.UserProfileClick(storyResponse))
            }

            createStoryLayout.throttleClicks().subscribeAndObserveOnMainThread {
                storyViewClickSubject.onNext(HomePageStoryInfoState.AddStoryResponseInfo(""))
            }

            storyItem.throttleClicks().subscribeAndObserveOnMainThread {
                storyViewClickSubject.onNext(HomePageStoryInfoState.StoryResponseData(storyResponse))
            }

            storyItem.setOnLongClickListener {
                if(loggedInUserCache.getUserId()?.equals(storyResponse.stories[0].userId) == true)
                    storyViewClickSubject.onNext(HomePageStoryInfoState.AddStoryResponseInfo("1"))

                true
            }
        }
    }

    fun bindAddInfo(info: String) {
        binding?.apply {
            createStoryLayout.visibility = View.VISIBLE
            storyItem.visibility = View.GONE

            Glide.with(context)
                .load(loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .fitCenter()
                .into(userProfileImageView)
        }
    }

    fun bind(storyListResponse: StoryListResponse) {
        this.storyResponse = storyListResponse

        binding?.apply {
            createStoryLayout.visibility = View.GONE
            storyItem.visibility = View.VISIBLE


            storyListResponse.stories.lastOrNull()?.mentions?.let {

                if(storyListResponse.stories.lastOrNull()?.mentions?.size ?: 0 > 0) {
                    mentionAppCompatTextView.text = "${storyListResponse.stories.lastOrNull()?.mentions?.get(0)?.user?.username ?: storyListResponse.stories.lastOrNull()?.mentions?.get(0)?.user?.name}"

                    mentionAppCompatTextView.isSelected = true
                    Glide.with(context)
                        .load(storyListResponse.stories.lastOrNull()?.mentions?.get(0)?.user?.avatar)
                        .placeholder(R.drawable.venue_placeholder)
                        .into(venueProfileRoundedImageView)
                } else {
                    venueLinearLayout.visibility = View.GONE
                    venueProfileRoundedImageView.visibility = View.GONE
                }
            } ?: {
                venueLinearLayout.visibility = View.GONE
                venueProfileRoundedImageView.visibility = View.GONE
            }

            Glide.with(context)
                .load(storyListResponse.lastThumbnail)
                .placeholder(R.drawable.venue_placeholder)
                .into(lastStoryRoundedImageView)


            Glide.with(context)
                .load(storyListResponse.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(userProfileRoundedImageView)

            Timber.tag("StoryUserName").d("storyListResponse.username: ${storyListResponse.username}")
            Timber.tag("StoryUserName").d("storyListResponse.name: ${storyListResponse.name}")
            userNameAppCompatTextView.text = storyListResponse.username
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}