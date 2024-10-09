package com.outgoer.ui.story

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.base.BaseActivity
import com.outgoer.databinding.ActivityStoryInfoBinding
import com.outgoer.ui.story.view.CubeTransformer
import com.outgoer.ui.story.view.StoryListAdapter
import timber.log.Timber


class StoryInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityStoryInfoBinding
    private var list = ArrayList<StoryListResponse>()
    private var mCurrentPosition = -1

    companion object {
        const val LIST_OF_STORY = "LIST_OF_STORY"
        fun getIntent(context: Context, listOfStories: ArrayList<StoryListResponse>): Intent {
            val intent = Intent(context, StoryInfoActivity::class.java)
            intent.putExtra(LIST_OF_STORY, listOfStories)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {

        intent?.let {
            list = intent?.getParcelableArrayListExtra(LIST_OF_STORY) ?: arrayListOf()
        }
        val size = list.size
        binding.viewpager2INfo.setPageTransformer(CubeTransformer())
//        binding.viewpager2INfo.offscreenPageLimit = 1

        val storyListAdapter = StoryListAdapter({
            if (list.size - 1 == it) {
                Timber.i("Get Last Story Finish Call Back")
                onBackPressedDispatcher.onBackPressed()
            } else {
                mCurrentPosition = it
                binding.viewpager2INfo.setCurrentItems(binding.viewpager2INfo.currentItem + 1, 500L)
            }

        }, {
            mCurrentPosition = it
            binding.viewpager2INfo.setCurrentItems(binding.viewpager2INfo.currentItem - 1, 500L)
        }, size, list, this@StoryInfoActivity)
        binding.viewpager2INfo.adapter = storyListAdapter

        val selectedItem = list.find { it.isSelected }
        val index = list.indexOf(selectedItem)
        binding.viewpager2INfo.setCurrentItem(index,false)
    }

    private fun ViewPager2.setCurrentItems(
        item: Int,
        duration: Long,
        interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
        pagePxWidth: Int = width // Default value taken from getWidth() from ViewPager2 view
    ) {
        val pxToDrag: Int = pagePxWidth * (item - currentItem)
        val animator = ValueAnimator.ofInt(0, pxToDrag)
        var previousValue = 0
        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            val currentPxToDrag = (currentValue - previousValue).toFloat()
            fakeDragBy(-currentPxToDrag)
            previousValue = currentValue
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                beginFakeDrag()
            }

            override fun onAnimationEnd(p0: Animator) {
                endFakeDrag()
            }

            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationRepeat(p0: Animator) {}
        })
        animator.interpolator = interpolator
        animator.duration = duration
        animator.start()
    }
}