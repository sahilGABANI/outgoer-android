package com.outgoer.ui.newvenuedetail

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BlockUserVenueBottomsheetBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BlockUserVenueBottomSheet: BaseBottomSheetDialogFragment() {

    private var _binding: BlockUserVenueBottomsheetBinding? = null
    private val binding get() = _binding!!

    private val blockOptionClickSubject: PublishSubject<String> = PublishSubject.create()
    val blockOptionClick: Observable<String> = blockOptionClickSubject.hide()
    companion object {
        private val VENUE_AVATAR = "VENUE_AVATAR"
        private val VENUE_NAME = "VENUE_NAME"
        @JvmStatic
        fun newInstanceWithData(profileAvatar: String, venueName: String): BlockUserVenueBottomSheet {
            var blockUserVenueBottomSheet = BlockUserVenueBottomSheet()

            val bundle = Bundle()
            bundle.putString(VENUE_NAME, venueName)
            bundle.putString(VENUE_AVATAR, profileAvatar)

            blockUserVenueBottomSheet.arguments = bundle

            return blockUserVenueBottomSheet
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BlockUserVenueBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        arguments?.let {
            val avatar = it.getString(VENUE_AVATAR)
            val venueName = it.getString(VENUE_NAME)

            Glide.with(requireActivity())
                .load(avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(binding.venueProfileRoundedImageView)

            binding.tvVenueName.text = venueName
        }


        binding.blockMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            blockOptionClickSubject.onNext("Done")
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}