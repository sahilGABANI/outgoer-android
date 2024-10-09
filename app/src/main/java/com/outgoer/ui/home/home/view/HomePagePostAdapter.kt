package com.outgoer.ui.home.home.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.HomePagePostInfoState
import com.outgoer.api.post.model.HomePageStoryInfoState
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HomePagePostAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val homePagePostViewClickSubject: PublishSubject<HomePagePostInfoState> = PublishSubject.create()
    val homePagePostViewClick: Observable<HomePagePostInfoState> = homePagePostViewClickSubject.hide()

    private val storyViewClickSubject: PublishSubject<HomePageStoryInfoState> = PublishSubject.create()
    val storyViewClick: Observable<HomePageStoryInfoState> = storyViewClickSubject.hide()

    private val spontyActionStateSubject: PublishSubject<SpontyActionState> = PublishSubject.create()
    val spontyActionState: Observable<SpontyActionState> = spontyActionStateSubject.hide()

    private val venueDetailActionStateSubject: PublishSubject<VenueDetail> = PublishSubject.create()
    val venueDetailActionState: Observable<VenueDetail> = venueDetailActionStateSubject.hide()


    var deviceHeight: Int? = null

    private var adapterItems = listOf<AdapterItem>()

    var listOfStories: ArrayList<StoryListResponse>? = null
        set(listOfStories) {
            field = listOfStories
            updateAdapterItems()
        }

    var listOfDataItems: List<PostInfo>? = null
        set(listOfSpotlightVideoInfo) {
            field = listOfSpotlightVideoInfo
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        this.adapterItems = emptyList()
        val adapterItems = mutableListOf<AdapterItem>()

        listOfStories?.let {
            adapterItems.add(AdapterItem.HomePageStoryModule(it))
        }

        listOfDataItems?.forEach {
            if(it.objectType == null || it.objectType.equals("post")) {
                when (it.type) {
                    1 -> {
                        adapterItems.add(AdapterItem.HomePagePostViewItem(it))
                    }
                    2 -> {
                        adapterItems.add(AdapterItem.HomePageVideoPostViewItem(it))
                    }
                    3 -> {
                        adapterItems.add(AdapterItem.HomePageVideoAndImagePostViewItem(it))
                    }
                    else -> {
                        adapterItems.add(AdapterItem.HomePagePostViewItem(it))
                    }
                }
            } else if(it.objectType.equals("sponty")) {
                adapterItems.add(AdapterItem.HomeSpontyViewItem(it))
            } else if(it.objectType.equals("venue")) {
                adapterItems.add(AdapterItem.HomeVenueViewItem(it))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.StoryModuleViewItemType.ordinal -> {
                HomePagePostAdapterViewHolder(HomePageStoryView(context).apply {
                    storyViewClick.subscribeAndObserveOnMainThread {
                        storyViewClickSubject.onNext(it)
                    }
                })
            }
            ViewType.ImagePostViewItemType.ordinal -> {
                HomePagePostAdapterViewHolder(HomePagePostView(context).apply {
                    homePagePostViewClick.subscribe { homePagePostViewClickSubject.onNext(it) }
                })
            }
            ViewType.VideoPostViewItemType.ordinal -> {
                HomePagePostAdapterViewHolder(HomePageVideoPostView(context).apply {
                    homePagePostViewClick.subscribe { homePagePostViewClickSubject.onNext(it) }
                })
            }
            ViewType.VideoAndImagePostViewItemType.ordinal -> {
                HomePagePostAdapterViewHolder(HomePageVideoPostView(context).apply {
                    homePagePostViewClick.subscribe { homePagePostViewClickSubject.onNext(it) }
                })
            }
            ViewType.SpontyModuleViewItemType.ordinal -> {
                HomePagePostAdapterViewHolder(HomeSpontyView(context).apply {
                    spontyActionState.subscribe { spontyActionStateSubject.onNext(it) }
                })
            }
            ViewType.VenueViewItemType.ordinal -> {
                HomePagePostAdapterViewHolder(HomeVenuesView(context).apply {
                    venueDetailActionState.subscribe{ venueDetailActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        if (position in adapterItems.indices) {
            return adapterItems[position].type
        }
        return -1
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.HomePageStoryModule -> {
                (holder.itemView as HomePageStoryView).bind(adapterItem.storyInfo)
            }
            is AdapterItem.HomePagePostViewItem -> {
                (holder.itemView as HomePagePostView).bind(adapterItem.postInfo)
            }
            is AdapterItem.HomePageVideoPostViewItem -> {
                (holder.itemView as HomePageVideoPostView).bind(adapterItem.postInfo,position)
            }
            is AdapterItem.HomePageVideoAndImagePostViewItem -> {
                (holder.itemView as HomePageVideoPostView).bind(adapterItem.postInfo, position)
            }
            is AdapterItem.HomeSpontyViewItem -> {
                (holder.itemView as HomeSpontyView).bind(adapterItem.postInfo)
            }
            is AdapterItem.HomeVenueViewItem -> {
                (holder.itemView as HomeVenuesView).bind(adapterItem.postInfo)
            }
        }
    }

    private class HomePagePostAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class HomePageStoryModule(var storyInfo: ArrayList<StoryListResponse>) : AdapterItem(ViewType.StoryModuleViewItemType.ordinal)
        data class HomePagePostViewItem(var postInfo: PostInfo) : AdapterItem(ViewType.ImagePostViewItemType.ordinal)
        data class HomePageVideoPostViewItem(var postInfo: PostInfo) : AdapterItem(ViewType.VideoPostViewItemType.ordinal)
        data class HomePageVideoAndImagePostViewItem(var postInfo: PostInfo) : AdapterItem(ViewType.VideoAndImagePostViewItemType.ordinal)
        data class HomeSpontyViewItem(var postInfo: PostInfo) : AdapterItem(ViewType.SpontyModuleViewItemType.ordinal)
        data class HomeVenueViewItem(var postInfo: PostInfo) : AdapterItem(ViewType.VenueViewItemType.ordinal)
    }

    private enum class ViewType {
        StoryModuleViewItemType,
        ImagePostViewItemType,
        VideoPostViewItemType,
        VideoAndImagePostViewItemType,
        SpontyModuleViewItemType,
        VenueViewItemType
    }
}