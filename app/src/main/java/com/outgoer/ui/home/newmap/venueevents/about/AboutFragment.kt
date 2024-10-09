package com.outgoer.ui.home.newmap.venueevents.about

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.outgoer.R
import com.outgoer.api.event.model.EventData
import com.outgoer.base.BaseFragment
import com.outgoer.databinding.FragmentAboutBinding
import com.outgoer.databinding.FragmentVenueEventsBinding
import com.outgoer.ui.home.newmap.venueevents.VenueEventsFragment
import com.outgoer.ui.home.profile.newprofile.NewMyReelFragment

class AboutFragment : BaseFragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    companion object {
        @JvmStatic
        fun newInstance() = AboutFragment()

        private val EVENT_INFO = "EVENT_INFO"
        @JvmStatic
        fun newInstanceWithData(eventData: EventData): AboutFragment {
            var aboutFragment = AboutFragment()

            val args = Bundle()
            args.putParcelable(EVENT_INFO, eventData)

            aboutFragment.arguments = args

            return aboutFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            binding.aboutAppCompatTextView.text = it.getParcelable<EventData>(EVENT_INFO)?.description
        }
    }

    override fun onResume() {
        super.onResume()
//        binding.root.requestLayout()
    }
}