package com.outgoer.ui.activateaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.outgoer.R
import com.outgoer.api.authentication.model.AccountActivationRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityActivateAccountBinding
import com.outgoer.ui.activateaccount.viewmodel.ActivateAccountViewModel
import javax.inject.Inject

class ActivateAccountActivity : BaseActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, ActivateAccountActivity::class.java)
        }
    }

    @Inject 
    internal lateinit var viewModelFactory: ViewModelFactory<ActivateAccountViewModel>
    private lateinit var activateAccountViewModel: ActivateAccountViewModel

    private lateinit var binding: ActivityActivateAccountBinding

    private val emailPattern = Patterns.EMAIL_ADDRESS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        activateAccountViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityActivateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        activateAccountViewModel.activateAccountState.subscribeAndObserveOnMainThread {
            when (it) {
                is ActivateAccountViewModel.ActivateAccountViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ActivateAccountViewModel.ActivateAccountViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is ActivateAccountViewModel.ActivateAccountViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                    binding.etEmailId.setText("")
                    binding.etReason.setText("")
                }
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.btnSubmit.throttleClicks().subscribeAndObserveOnMainThread {
            if (isValidate()) {
                hideKeyboard()
                val emailId = binding.etEmailId.text.toString()
                val reason = binding.etReason.text.toString()
                activateAccountViewModel.activateAccount(
                    AccountActivationRequest(
                        email = emailId,
                        reason = reason
                    )
                )
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSubmit.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnSubmit.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun isValidate(): Boolean {
        var isValidate = true
        if (binding.etEmailId.text.isNullOrEmpty()) {
            showToast(getString(R.string.empty_email))
            isValidate = false
        } else if (!emailPattern.matcher(binding.etEmailId.text.toString()).matches()) {
            showToast(getString(R.string.invalid_email))
            isValidate = false
        }
        return isValidate
    }
}