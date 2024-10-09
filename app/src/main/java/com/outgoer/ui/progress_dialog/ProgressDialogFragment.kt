package com.outgoer.ui.progress_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.outgoer.R
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.DialogProgressOfUploadBinding
import com.outgoer.ui.post.viewmodel.AddNewPostViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ProgressDialogFragment : BaseDialogFragment() {

    private val progressStateSubject: PublishSubject<String> = PublishSubject.create()
    val progressState: Observable<String> = progressStateSubject.hide()

    companion object {

        fun newInstance(): ProgressDialogFragment {

            val fragment = ProgressDialogFragment()
            return fragment
        }
    }

    private var _binding: DialogProgressOfUploadBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddNewPostViewModel>
    private lateinit var addNewPostViewModel: AddNewPostViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogProgressOfUploadBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        addNewPostViewModel = getViewModelFromFactory(viewModelFactory)
        loadDataFromIntent()
        listenToViewEvent()
        listenToViewModel()
    }

    private fun loadDataFromIntent() {
        addNewPostViewModel.getLiveDataInfo()

    }

    private fun listenToViewModel() {
        addNewPostViewModel.addNewPostState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddNewPostViewModel.AddNewPostViewState.ProgressDisplay -> {
                    binding.progress.progress = it.progressInfo.toInt()

                    if(it.progressInfo.equals(100.0))
                        progressStateSubject.onNext("done")
                }
                is AddNewPostViewModel.AddNewPostViewState.UploadVideoCloudFlareSuccess -> {
                    progressStateSubject.onNext("done")
                    dismiss()
                }
                is AddNewPostViewModel.AddNewPostViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.tvCancel.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().runOnUiThread(Runnable {
                addNewPostViewModel.stopClient()
            })
            progressStateSubject.onNext("done")
        }.autoDispose()

    }
}