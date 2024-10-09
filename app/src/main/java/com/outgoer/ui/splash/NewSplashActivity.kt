package com.outgoer.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import com.google.gson.Gson
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityNewSplashBinding
import com.outgoer.service.NotificationService
import com.outgoer.ui.create_story.AddToStoryActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.onboarding.OnBoardingActivity
import com.outgoer.ui.story.StoryInfoActivity
import com.outgoer.ui.story.StoryViewActivity
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@SuppressLint("CustomSplashScreen")
class NewSplashActivity : BaseActivity() {

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityNewSplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        OutgoerApplication.component.inject(this)
        Timber.tag("NewSplashActivity").i("NewSplashActivity is Open")
        binding = ActivityNewSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("NewSplashActivity").i("NewSplashActivity is Open")
        Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
            val loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser
            if (loggedInUser != null) {
                Timber.tag("<><> Access Token").e(loggedInUserCache.getLoginUserToken())
                when {
                    loggedInUser.emailVerified != 1 -> {
                        //startActivityWithDefaultAnimation(NewLoginActivity.getIntent(this))
                        finish()
                    }
                    else -> {
                        manageNavigation()
                    }
                }
            } else {
                startActivityWithDefaultAnimation(OnBoardingActivity.getIntent(this))
                finish()
            }
        }.autoDispose()
    }

    private fun manageNavigation() {
        when {
            intent?.hasExtra(NotificationService.NOTIFICATION_TYPE) == true -> {
                val intent = when (val notificationType = intent?.getStringExtra(NotificationService.NOTIFICATION_TYPE)) {
                    NotificationService.N_TYPE_CHAT -> {
                        val payload = intent?.getStringExtra(NotificationService.PAYLOAD)
                        if (payload != null) {
                            val chatConversationInfo = Gson().fromJson(payload, ChatConversationInfo::class.java)
                            if (chatConversationInfo != null) {
                                HomeActivity.launchFromChatNotification(this, chatConversationInfo)
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    }
                    NotificationService.N_TYPE_FOLLOW -> {
                        val payload = intent?.getStringExtra(NotificationService.PAYLOAD)
                        if (payload != null) {
                            val followObj = JSONObject(payload)
                            if (followObj.has(NotificationService.EXTRA_PARAM_FOLLOW_BY)) {
                                val otherUserId = followObj.optInt(NotificationService.EXTRA_PARAM_FOLLOW_BY, -1)
                                if (otherUserId != -1) {
                                    HomeActivity.launchFromFollowNotification(this, otherUserId)
                                } else {
                                    Intent(this, HomeActivity::class.java)
                                }
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    }
                    NotificationService.N_TYPE_POST_LIKED,
                    NotificationService.N_TYPE_POST_COMMENT, NotificationService.N_TYPE_POST_COMMENT_LIKED, NotificationService.N_TYPE_POST_COMMENT_REPLY,
                    NotificationService.N_TYPE_POST_TAG_POST -> {
                        val payload = intent?.getStringExtra(NotificationService.PAYLOAD)
                        if (payload != null) {
                            val postObj = JSONObject(payload)
                            if (postObj.has(NotificationService.EXTRA_PARAM_POST_ID)) {
                                val postId = postObj.optInt(NotificationService.EXTRA_PARAM_POST_ID, -1)
                                if (postId != -1) {
                                    if (notificationType == NotificationService.N_TYPE_POST_COMMENT ||
                                        notificationType == NotificationService.N_TYPE_POST_COMMENT_LIKED ||
                                        notificationType == NotificationService.N_TYPE_POST_COMMENT_REPLY
                                    ) {
                                        HomeActivity.launchFromPostNotification(this, postId, showComments = true)
                                    } else if (notificationType == NotificationService.N_TYPE_POST_TAG_POST) {
                                        HomeActivity.launchFromPostNotification(this, postId, showTaggedPeople = true)
                                    } else {
                                        HomeActivity.launchFromPostNotification(this, postId)
                                    }
                                } else {
                                    Intent(this, HomeActivity::class.java)
                                }
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    }
                    NotificationService.N_TYPE_REEL_LIKED,
                    NotificationService.N_TYPE_REEL_COMMENT, NotificationService.N_TYPE_REEL_COMMENT_LIKED, NotificationService.N_TYPE_REEL_COMMENT_REPLY,
                    NotificationService.N_TYPE_REEL_TAG_REEL -> {
                        val payload = intent?.getStringExtra(NotificationService.PAYLOAD)
                        if (payload != null) {
                            val postObj = JSONObject(payload)
                            if (postObj.has(NotificationService.EXTRA_PARAM_REEL_ID)) {
                                val reelId = postObj.optInt(NotificationService.EXTRA_PARAM_REEL_ID, -1)
                                if (reelId != -1) {
                                    if (notificationType == NotificationService.N_TYPE_REEL_COMMENT ||
                                        notificationType == NotificationService.N_TYPE_REEL_COMMENT_LIKED ||
                                        notificationType == NotificationService.N_TYPE_REEL_COMMENT_REPLY
                                    ) {
                                        HomeActivity.launchFromReelNotification(this, reelId, showComments = true)
                                    } else if (notificationType == NotificationService.N_TYPE_REEL_TAG_REEL) {
                                        HomeActivity.launchFromReelNotification(this, reelId, showTaggedPeople = true)
                                    } else {
                                        HomeActivity.launchFromReelNotification(this, reelId)
                                    }
                                } else {
                                    Intent(this, HomeActivity::class.java)
                                }
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    }
                    NotificationService.N_TYPE_LIVE_EVENT, NotificationService.N_TYPE_HOST_INVITE -> {
                        val payload = intent?.getStringExtra(NotificationService.PAYLOAD)
                        if (payload != null) {
                            val liveEventInfo = Gson().fromJson(payload, LiveEventInfo::class.java)
                            if (liveEventInfo != null) {
                                HomeActivity.launchFromLiveNotification(this, liveEventInfo)
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    }
                    else -> {
                        Intent(this, HomeActivity::class.java)
                    }
                }
                startActivityWithFadeInAnimation(intent)
                finish()
            }
            else -> {
                startActivityWithFadeInAnimation(HomeActivity.getIntent(this))
                finish()
            }
        }
    }
}