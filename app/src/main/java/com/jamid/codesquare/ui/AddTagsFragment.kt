package com.jamid.codesquare.ui

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.InterestItemAdapter
import com.jamid.codesquare.adapter.recyclerview.SearchResultsAdapter
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.data.SearchQuery
import com.jamid.codesquare.databinding.FragmentAddTagBinding
import com.jamid.codesquare.listeners.AddTagsListener
import com.jamid.codesquare.listeners.InterestItemClickListener
import com.jamid.codesquare.listeners.SearchItemClickListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class AddTagsFragment: RoundedBottomSheetDialogFragment(), SearchItemClickListener, InterestItemClickListener {

    private lateinit var binding: FragmentAddTagBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var currentSearchList: List<SearchQuery> = emptyList()
    private var addTagsListener: AddTagsListener? = null
    private var selectedTempList = mutableListOf<String>()
    private var title: String = "Add tags"
    private var draggable: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddTagBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = dialog!!
        val frame = dialog.window!!.decorView.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(frame)
        val totalHeight = getWindowHeight()
        binding.root.updateLayoutParams<ViewGroup.LayoutParams> {
            height = totalHeight
        }

        behavior.isDraggable = draggable

        val interestsAdapter = InterestItemAdapter(this)

        binding.postTags.apply {
            layoutManager = FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP)
            adapter = interestsAdapter
        }

        val offset = totalHeight * 0.15

        binding.addTagsHeader.text = title

        behavior.maxHeight = totalHeight - offset.toInt()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.postTagsCloseBtn.setOnClickListener {
            onBackPressed()
        }
        
        val searchResultsAdapter = SearchResultsAdapter(this).apply {
            shouldShowRightIcon = false
        }
        
        binding.postTagSearchRecycler.apply {
            adapter = searchResultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.isNotEmpty()) {
                    currentSearchList = it.distinctBy { it1 ->
                        it1.queryString
                    }

                    binding.postTagSearchRecycler.show()

                    searchResultsAdapter.submitList(currentSearchList)
                } else {
                    searchResultsAdapter.submitList(emptyList())
                }
            } else {

                binding.postTagSearchRecycler.hide()

                searchResultsAdapter.submitList(emptyList())
            }
        }

        val query = Firebase.firestore.collection(INTERESTS)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPagedInterestItems(query).collectLatest {
                interestsAdapter.submitData(it)
            }
        }

        // for end icon click
        binding.postTagSearchInput.editText?.doAfterTextChanged { text ->
            if (!text.isNullOrBlank()) {

                viewModel.searchInterests(text.toString())
                binding.postTagSearchInput.setEndIconDrawable(R.drawable.ic_round_add_24)

                binding.postTagSearchInput.setEndIconOnClickListener {
                    val tag = text.toString()

                    // insert in database
                    insertInterestItem(tag)
                }

            } else {
                viewModel.setSearchData(emptyList())
                
                binding.postTagSearchInput.setEndIconDrawable(R.drawable.ic_round_search_24)
                
                binding.postTagSearchInput.setEndIconOnClickListener {

                }
            }
        }

        /*val alphabets = getString(R.string.alphabets)
        FireUtility.getRandomInterests {
            if (it.isSuccessful) {
                val snapshot = it.result
                for (l in alphabets) {
                    @Suppress("UNCHECKED_CAST") val tags = snapshot[l.toString()] as List<String>
                    preloadTags(tags)
                }
            } else {
                Log.d(TAG, it.exception?.localizedMessage.orEmpty())
            }
        }*/

        binding.postTagsAddBtn.setOnClickListener {
            saveChanges()
            dismiss()
        }

    }

    private fun onBackPressed() {
        if (selectedTempList.isNotEmpty()) {

            val word = if (title.contains("tag")) {
                "tags"
            } else {
                "interests"
            }

            val frag = MessageDialogFragment.builder("You have selected some $word!")
                .setTitle("Add $word")
                .setPositiveButton("Save changes") { a, _ ->
                    a.dismiss()
                    saveChanges()
                    this.dismiss()
                }.setNegativeButton("Discard changes") { a, _ ->
                    a.dismiss()
                    this.dismiss()
                }.build()

            frag.show(requireActivity().supportFragmentManager, MessageDialogFragment.TAG)
            /*MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add $word")
                .setMessage("You have selected some $word!")
                .setPositiveButton("Save changes") { a, _ ->
                    a.dismiss()
                    saveChanges()
                    this.dismiss()
                }.setNegativeButton("Discard changes") { a, _ ->
                    a.dismiss()
                    this.dismiss()
                }.show()*/
        } else {
            dismiss()
        }
    }

    private fun saveChanges() {
        val tags = mutableListOf<String>()

        for (c in binding.postTags.children) {
            val chip = c as Chip
            if (chip.isChecked) {
                tags.add(chip.text.toString())
            }
        }

        addTagsListener?.onTagsSelected(tags)

    }

   /* private fun preloadTags(tags: List<String>){
        val newTags = tags.shuffled()

        for (i in newTags) {
            binding.postTags.addTag(i)
        }
    }

    private fun FlexboxLayout.addTag(s: String, checked: Boolean = false) {
        s.trim()
        val chip = View.inflate(requireContext(), R.layout.choice_chip, null) as Chip
        chip.text = s
        chip.isCheckable = true
        chip.isChecked = checked
        chip.isCheckedIconVisible = false
        chip.isCloseIconVisible = false
        chip.tag = s

        fun onAdded() {

            tagsHolder.add(s)

            chip.updateLayoutParams<FlexboxLayout.LayoutParams> {
                marginEnd = resources.getDimension(R.dimen.generic_len).toInt()
                marginStart = resources.getDimension(R.dimen.zero).toInt()
            }
        }

        if (currentSearchList.find { it.queryString == s } == null) {
            this.addView(chip, 0)
            onAdded()
        } else {
            val oldChip = this.findViewWithTag<Chip>(s)
            if (oldChip != null) {
                oldChip.performClick()
                tagsHolder.remove(s)
                return
            } else {
                this.addView(chip, 0)
                onAdded()
            }
        }

        chip.setOnClickListener {
            this.removeView(chip)
            tagsHolder.remove(s)
            if (chip.isChecked) {
                this.addView(chip, 0)
                onAdded()
            }
        }
        
    }*/

    override fun onSearchItemClick(searchQuery: SearchQuery) {
        val tag = searchQuery.queryString
        insertInterestItem(tag)
        binding.postTagSearchInput.editText?.text?.clear()
    }

    private fun insertInterestItem(tag: String) {
        // insert in database
        val id = tag.trim().lowercase().replace(" ", "_")
        val now = System.currentTimeMillis()
        val interestItem = InterestItem(
            id,
            id,
            tag,
            now,
            emptyList(),
            now,
            0.001,
            true
        )

        FireUtility.insertInterestItem(interestItem) {
            binding.postTagSearchInput.editText?.text?.clear()

            if (it.isSuccessful) {
                viewModel.insertInterestItem(interestItem)
            } else {
                Log.e(TAG, "onViewCreated: ${it.exception?.localizedMessage}")
            }
        }
    }

    override fun onRecentSearchClick(searchQuery: SearchQuery) {
        
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {
        
    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {
        
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setSearchData(emptyList())
    }
    
    companion object {

        const val TAG = "AddTagFragment"

        class Builder {

            private val addTagsFragment = AddTagsFragment()

            fun setListener(mListener: AddTagsListener): Builder {
                addTagsFragment.addTagsListener = mListener
                return this
            }

            fun setTitle(title: String): Builder {
                addTagsFragment.title = title
                return this
            }

            fun setIsDraggable(isDraggable: Boolean): Builder {
                addTagsFragment.draggable = isDraggable
                return this
            }

            /*fun setPositiveButton(label: String, a: (DialogFragment, View) -> Unit): Builder {
                messageDialogFragment.positiveLabel = label
                messageDialogFragment.onPositiveClickListener = object : MessageDialogInterface.OnClickListener {
                    override fun onClick(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun setNegativeButton(label: String, a: (DialogFragment, View) -> Unit): Builder {
                messageDialogFragment.negativeLabel = label
                messageDialogFragment.onNegativeClickListener = object : MessageDialogInterface.OnClickListener {
                    override fun onClick(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun setCustomView(@LayoutRes id: Int, a: (DialogFragment, View) -> Unit): Builder {
                messageDialogFragment.layoutId = id
                messageDialogFragment.onInflateListener = object: MessageDialogInterface.OnInflateListener {
                    override fun onInflate(d: DialogFragment, v: View) {
                        a(d, v)
                    }
                }
                return this
            }

            fun shouldShowProgress(a: Boolean): Builder {
                messageDialogFragment.shouldShowProgress = a
                return this
            }

            fun setIsHideable(isHideable: Boolean): Builder {
                messageDialogFragment.isHideable = isHideable
                return this
            }

            fun setIsDraggable(isDraggable: Boolean): Builder {
                messageDialogFragment.isDraggable = isDraggable
                return this
            }

            fun setScrimVisibility(isVisible: Boolean): Builder {
                 messageDialogFragment.isScrimVisible = isVisible
                 return this
            }*/

            fun build(): AddTagsFragment {
                return addTagsFragment
            }

        }

        fun builder(): Builder {
            return Builder()
        }

        fun newInstance(): AddTagsFragment {
            return AddTagsFragment()
        }

    }

    override fun onInterestClick(interestItem: InterestItem) {
        if (interestItem.isChecked) {
            selectedTempList.remove(interestItem.content)
            viewModel.uncheckInterestItem(interestItem)
        } else {
            selectedTempList.add(interestItem.content)
            viewModel.checkInterestItem(interestItem)
        }
    }

}