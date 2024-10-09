package com.outgoer.ui.tag

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddTagToPostBinding
import com.outgoer.ui.tag.view.TagPeopleAdapter
import com.outgoer.ui.tag.viewmodel.AddTagViewModel
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddTagToPostActivity : BaseActivity() {

    companion object {
        const val INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP = "INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP"
        fun launchActivity(context: Context, taggedPeopleHashMap: HashMap<Int, String?>): Intent {
            val intent = Intent(context, AddTagToPostActivity::class.java)
            intent.putExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP, taggedPeopleHashMap)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddTagViewModel>
    private lateinit var addTagViewModel: AddTagViewModel

    private lateinit var binding: ActivityAddTagToPostBinding

    private lateinit var tagPeopleAdapter: TagPeopleAdapter
    private var peopleForTagArrayList = ArrayList<PeopleForTag>()

    private var taggedPeopleHashMap = HashMap<Int, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OutgoerApplication.component.inject(this)
        addTagViewModel = getViewModelFromFactory(viewModelFactory)

        binding = ActivityAddTagToPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDataFromIntent()
        listenToViewModel()
        listenToViewEvents()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            if (it.hasExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)) {
                val taggedPeopleHashMap = it.getSerializableExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP)
                if (taggedPeopleHashMap != null) {
                    this.taggedPeopleHashMap = taggedPeopleHashMap as HashMap<Int, String?>
                }
            }
        }
    }

    private fun listenToViewEvents() {
        peopleForTagArrayList = ArrayList()

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivDone.throttleClicks().subscribeAndObserveOnMainThread {

            if(taggedPeopleHashMap.isEmpty()) {
                showToast("Please select people")
            } else {
                setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_TAGGED_PEOPLE_HASHMAP, taggedPeopleHashMap))
                finish()
            }
        }.autoDispose()

        tagPeopleAdapter = TagPeopleAdapter(this).apply {
            tagPeopleClick.subscribeAndObserveOnMainThread {
                if (it.isSelected) {
                    if (taggedPeopleHashMap.containsKey(it.id)) {
                        taggedPeopleHashMap.remove(it.id)
                    }
                } else {
                    if (!taggedPeopleHashMap.containsKey(it.id)) {
                        taggedPeopleHashMap[it.id] = it.username
                    }
                }
                val mPos = peopleForTagArrayList.indexOf(it)
                if (mPos != -1) {
                    peopleForTagArrayList[mPos].isSelected = !it.isSelected
                }
                tagPeopleAdapter.listOfDataItems = peopleForTagArrayList

                hideShowDoneButton()
            }.autoDispose()
        }

        binding.rvTagPeopleList.apply {
            layoutManager = LinearLayoutManager(this@AddTagToPostActivity, RecyclerView.VERTICAL, false)
            adapter = tagPeopleAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        (layoutManager as LinearLayoutManager).apply {
                            val visibleItemCount = childCount
                            val totalItemCount = itemCount
                            val pastVisibleItems = findFirstVisibleItemPosition()
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                addTagViewModel.loadMoreTagPeople()
                            }
                        }
                    }
                }
            })
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
            }.autoDispose()

        binding.etSearch.textChanges()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("Search String %s", it.toString())
                if (it.length > 1) {
                    addTagViewModel.searchTagPeople(it.toString())
                } else if (it.isEmpty()) {
                    addTagViewModel.searchTagPeople("")
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun listenToViewModel() {
        addTagViewModel.addTagState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddTagViewModel.AddTagViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("AddTagViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(findViewById(android.R.id.content))
                    } else {
                        showLongToast(it.errorMessage)
                    }
                }
                is AddTagViewModel.AddTagViewState.ListOfPeopleForTag -> {
                    val peopleForTagList = it.ListOfPeopleForTag
                    for (i in peopleForTagList.indices) {
                        if (taggedPeopleHashMap.containsKey(peopleForTagList[i].id)) {
                            peopleForTagList[i].isSelected = true
                        }
                    }
                    peopleForTagArrayList.clear()
                    peopleForTagArrayList.addAll(peopleForTagList)
                    tagPeopleAdapter.listOfDataItems = peopleForTagArrayList

                    if(peopleForTagArrayList.isNullOrEmpty()) {
                        binding.llNoData.visibility = View.VISIBLE
//                        binding.ivDone.visibility = View.GONE
//                    } else {
                        binding.llNoData.visibility = View.GONE
//                        binding.ivDone.visibility = View.VISIBLE
                    }

                    hideShowDoneButton()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun hideShowDoneButton() {
        if (taggedPeopleHashMap.isEmpty()) {
            binding.ivDone.visibility = View.INVISIBLE
        } else {
            binding.ivDone.visibility = View.VISIBLE
        }
    }
}