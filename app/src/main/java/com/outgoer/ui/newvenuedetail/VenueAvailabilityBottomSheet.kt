package com.outgoer.ui.newvenuedetail

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.databinding.BottomSheetVenueAvailabilityBinding
import com.outgoer.ui.newvenuedetail.view.VenueAvailabilityAdapter

class VenueAvailabilityBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetVenueAvailabilityBinding? = null
    private val binding get() = _binding!!

    private lateinit var venueAvailabilityAdapter: VenueAvailabilityAdapter

    companion object {
        private val VENUE_AVAILABILITY = "VENUE_AVAILABILITY"

        @JvmStatic
        fun newInstance(): VenueAvailabilityBottomSheet {
            return VenueAvailabilityBottomSheet()
        }

        @JvmStatic
        fun newInstanceWithData(listofavailability: ArrayList<VenueAvailabilityRequest>): VenueAvailabilityBottomSheet {
            var venueAvailabilityBottomSheet = VenueAvailabilityBottomSheet()

            val bundle = Bundle()
            bundle.putParcelableArrayList(VENUE_AVAILABILITY, listofavailability)

            venueAvailabilityBottomSheet.arguments = bundle

            return venueAvailabilityBottomSheet
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
        _binding = BottomSheetVenueAvailabilityBinding.inflate(inflater, container, false)
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
        binding.llSearch.visibility = View.GONE
        venueAvailabilityAdapter = VenueAvailabilityAdapter(requireContext())
        binding.dataRecyclerView.apply {
            adapter = venueAvailabilityAdapter
        }

        arguments?.let {
            val listofavailability = it.getParcelableArrayList<VenueAvailabilityRequest>(VENUE_AVAILABILITY)
            venueAvailabilityAdapter.listofAvailable = listofavailability
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}