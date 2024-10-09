package com.outgoer.ui.home.map.venueinfo

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.*
import com.outgoer.databinding.VenueInfoBottomSheetBinding
import com.outgoer.ui.othernearvenue.OtherNearVenueActivity
import com.outgoer.ui.venuedetail.VenueDetailActivity
import javax.inject.Inject

class VenueInfoBottomSheet(
    private val venueCategoryId: Int,
    private val venueMapInfo: VenueMapInfo,
    private val venueMapInfoList: List<VenueMapInfo>
) : BaseBottomSheetDialogFragment() {

    private var _binding: VenueInfoBottomSheetBinding? = null
    private val binding get() = _binding!!
    @Inject
    lateinit var loggedInUserCache : LoggedInUserCache
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.venue_info_bottom_sheet, container, false)
        _binding = VenueInfoBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        binding.tvVenueCategoryName.text = venueMapInfo.name
        binding.tvAddress.text = venueMapInfo.venueAddress
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
            binding.tvDistance.text = if (venueMapInfo.distance != null) {
                venueMapInfo.distance.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
            } else {
                "0 ".plus(getString(R.string.label_miles))
            }
        } else {
            binding.tvDistance.text = if (venueMapInfo.distance != null) {
                venueMapInfo.distance.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
            } else {
                "0 ".plus(getString(R.string.label_kms))
            }
        }

        Glide.with(this)
            .load(venueMapInfo.avatar)
            .placeholder(R.drawable.venue_placeholder)
            .error(R.drawable.venue_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivVenueImage)

        Glide.with(this)
            .load(venueMapInfo.category?.firstOrNull()?.thumbnailImage ?: "")
            .placeholder(R.drawable.venue_placeholder)
            .error(R.drawable.venue_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivVenueCategoryImage)

        if (venueMapInfoList.size > 1) {
            binding.tvSeeAll.visibility = View.VISIBLE
        } else {
            binding.tvSeeAll.visibility = View.INVISIBLE
        }

        binding.tvSeeAll.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(OtherNearVenueActivity.getIntent(requireContext(), venueCategoryId, venueMapInfo.id))
            dismissBottomSheet()
        }.autoDispose()

        binding.llVenueInfo.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(VenueDetailActivity.getIntent(requireContext(), venueMapInfo.category?.firstOrNull()?.id ?: 0, venueMapInfo.id))
            dismissBottomSheet()
        }.autoDispose()

        binding.ivDirection.throttleClicks().subscribeAndObserveOnMainThread {
            openGoogleMapWithProvidedLatLng(requireContext(), venueMapInfo.latitude, venueMapInfo.longitude)
            dismissBottomSheet()
        }.autoDispose()
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}