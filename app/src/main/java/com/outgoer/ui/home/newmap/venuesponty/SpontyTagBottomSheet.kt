package com.outgoer.ui.home.newmap.venuesponty

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.sponty.model.AllJoinSpontyRequest
import com.outgoer.api.sponty.model.SpontyActionRequest
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.SpontyListItemBinding
import com.outgoer.ui.sponty.comment.SpontyReplyBottomSheet
import com.outgoer.ui.sponty.viewmodel.SpontyViewModel
import javax.inject.Inject

class SpontyTagBottomSheet(
    private val spontyResponse: SpontyResponse
) : BaseBottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<SpontyViewModel>
    private lateinit var spontyViewModel: SpontyViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: SpontyListItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        spontyViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sponty_list_item, container, false)
        _binding = SpontyListItemBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewEvents() {
        Glide.with(requireContext())
            .load(spontyResponse.user?.avatar)
            .centerCrop()
            .into(binding.ivProfile)

        if (spontyResponse.spontyJoin) {
            binding.unjoinMaterialButton.visibility = View.VISIBLE
            binding.joinMaterialButton.visibility = View.GONE
        } else {
            binding.unjoinMaterialButton.visibility = View.GONE
            binding.joinMaterialButton.visibility = View.VISIBLE
        }

        binding.usernameAppCompatTextView.text = spontyResponse.user?.username
        binding.timeAppCompatTextView.text = spontyResponse.humanReadableTime
        binding.aboutAppCompatTextView.text = spontyResponse.caption
        binding.locationAppCompatTextView.text = spontyResponse.location
        binding.tvCommentCount.text = spontyResponse.totalComments.toString()
        binding.tvLikeCount.text = spontyResponse.totalLikes.toString()
        updateSpontyLike()

        binding.joinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            spontyViewModel.addRemoveSponty(AllJoinSpontyRequest(spontyId = spontyResponse.id))
        }

        binding.unjoinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            spontyViewModel.addRemoveSponty(AllJoinSpontyRequest(spontyId = spontyResponse.id))
        }

        binding.commentLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            var spontyId = spontyResponse.id
            var spontyReplyBottomSheet = SpontyReplyBottomSheet.newInstance(spontyId)
            spontyReplyBottomSheet.commentActionState.subscribeAndObserveOnMainThread { res ->
                spontyResponse.totalComments = res
            }
            spontyReplyBottomSheet.show(childFragmentManager, "SpontyReplyBottomSheet")
        }

        binding.likeLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
            spontyViewModel.addRemoveSpontyLike(SpontyActionRequest(spontyResponse.id))
        }

        setJoinedUserInfo()

    }

    private fun listenToViewModel() {
        spontyViewModel.spontyDataState.subscribeAndObserveOnMainThread {
            when (it) {
                is SpontyViewModel.SpontyDataState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is SpontyViewModel.SpontyDataState.AddRemoveSpontyLike -> {
                    spontyResponse.spontyLike = !spontyResponse.spontyLike
//                    spontyResponse.totalLikes = spontyResponse.totalLikes?.let { it + 1 } ?: 0
//                    binding.tvLikeCount.text = spontyResponse.totalLikes.toString()

                    updateSpontyLike()
                }
                is SpontyViewModel.SpontyDataState.AddSpontyJoin -> {
                    if (it.joinStatus == 0) {
                        spontyResponse.spontyJoin = false
                    } else {
                        spontyResponse.spontyJoin = true
                    }

                    binding.checkJoinMaterialButton.visibility = View.GONE
                    if (spontyResponse.spontyJoin) {
                        binding.unjoinMaterialButton.visibility = View.VISIBLE
                        binding.joinMaterialButton.visibility = View.GONE
                    } else {
                        binding.unjoinMaterialButton.visibility = View.GONE
                        binding.joinMaterialButton.visibility = View.VISIBLE
                    }
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun updateSpontyLike() {
        if(spontyResponse.spontyLike) {
            binding.likeAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_red_like, null))
        } else {
            binding.likeAppCompatImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_white_like, null))
        }
    }

    private fun setJoinedUserInfo() {
        val userId = loggedInUserCache.getUserId() ?: 0

        if (userId != -1 && userId.equals(spontyResponse.user?.id)) {

            when (spontyResponse.joinUsers?.size ?: 0) {
                0 -> {
                    binding.checkJoinMaterialButton.visibility = View.GONE
                }
                1 -> {
                    binding.checkJoinMaterialButton.visibility = View.VISIBLE
                    binding.firstRoundedImageView.visibility = View.VISIBLE
                    binding.secondRoundedImageView.visibility = View.GONE
                    binding.moreFrameLayout.visibility = View.GONE

                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(0)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.firstRoundedImageView)
                }
                2 -> {
                    binding.checkJoinMaterialButton.visibility = View.VISIBLE
                    binding.firstRoundedImageView.visibility = View.VISIBLE
                    binding.secondRoundedImageView.visibility = View.VISIBLE
                    binding.moreFrameLayout.visibility = View.GONE
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(0)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.firstRoundedImageView)
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(1)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.secondRoundedImageView)
                }
                3 -> {
                    binding.checkJoinMaterialButton.visibility = View.VISIBLE
                    binding.firstRoundedImageView.visibility = View.VISIBLE
                    binding.secondRoundedImageView.visibility = View.VISIBLE
                    binding.moreFrameLayout.visibility = View.VISIBLE
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(0)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.firstRoundedImageView)
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(1)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.secondRoundedImageView)
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(2)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.thirdRoundedImageView)

                }
                else -> {
                    binding.checkJoinMaterialButton.visibility = View.VISIBLE
                    binding.firstRoundedImageView.visibility = View.VISIBLE
                    binding.secondRoundedImageView.visibility = View.VISIBLE
                    binding.moreFrameLayout.visibility = View.VISIBLE
                    binding.thirdRoundedImageView.visibility = View.VISIBLE
                    binding.maxRoundedImageView.visibility = View.VISIBLE

                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(0)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.firstRoundedImageView)
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(1)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.secondRoundedImageView)
                    Glide.with(requireContext())
                        .load(spontyResponse.joinUsers?.get(2)?.avatar)
                        .placeholder(
                            resources.getDrawable(
                                R.drawable.ic_chat_user_placeholder,
                                null
                            )
                        )
                        .into(binding.thirdRoundedImageView)

                    binding.maxRoundedImageView.text =
                        (spontyResponse.joinUsers?.size ?: 0 - 3).toString().plus("+")


                }
            }
        } else {
            binding.joinMaterialButton.visibility = if(!userId.equals(spontyResponse.user?.id)) View.VISIBLE else View.GONE
            binding.checkJoinMaterialButton.visibility = View.GONE
        }
    }


    fun dismissBottomSheet() {
        dismiss()
    }
}