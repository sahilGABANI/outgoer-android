package com.outgoer.ui.home.create

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding3.widget.editorActions
import com.outgoer.R
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.extension.hideKeyboard
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.AddHashtagBottomSheetBinding
import com.outgoer.ui.home.create.view.AddedHashtagsAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class AddHashtagBottomSheet(val hashtagArrayList:ArrayList<String>) : BaseBottomSheetDialogFragment() {

    private val addedHashTagsClicksSubject: PublishSubject<ArrayList<String>> = PublishSubject.create()
    val addedHashTagsClicks: Observable<ArrayList<String>> = addedHashTagsClicksSubject.hide()

    private var _binding: AddHashtagBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var addedHashtagAdapter: AddedHashtagsAdapter
    private var addedHashtagArrayList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AddHashtagBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        loadData()
        listenToViewEvents()

    }

    private fun loadData(){
        if(!hashtagArrayList.isNullOrEmpty()) {
            addedHashtagArrayList = hashtagArrayList
        }
    }


    private fun listenToViewEvents() {

        addedHashtagAdapter = AddedHashtagsAdapter(requireContext()).apply {
            removeItemClick.subscribeAndObserveOnMainThread {
                addedHashtagArrayList.remove(it)
                addedHashtagAdapter.listOfDataItems = addedHashtagArrayList
            }
        }
        binding.rvAddedHashtag.adapter = addedHashtagAdapter

        addedHashtagAdapter.listOfDataItems = addedHashtagArrayList
        addedHashtagAdapter.isHashtagRemove = true
        binding.btnSubmit.throttleClicks().subscribeAndObserveOnMainThread {
            addedHashTagsClicksSubject.onNext(addedHashtagArrayList)
            dismissBottomSheet()
        }

        binding.btnAdd.throttleClicks().subscribeAndObserveOnMainThread {
            manageAddedHashTag()
        }

        binding.etHashtag.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEND }
            .subscribeAndObserveOnMainThread {
                manageAddedHashTag()
            }.autoDispose()

    }

    private fun manageAddedHashTag() {
        if (binding.etHashtag.text.isNullOrEmpty()) {
            dismissBottomSheet()
        } else {
//            requireActivity().hideKeyboard(binding.etHashtag)
            addedHashtagArrayList.add(binding.etHashtag.text.toString())
            addedHashtagAdapter.notifyDataSetChanged()
            addedHashtagAdapter.listOfDataItems = addedHashtagArrayList
            binding.etHashtag.setText("")
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}