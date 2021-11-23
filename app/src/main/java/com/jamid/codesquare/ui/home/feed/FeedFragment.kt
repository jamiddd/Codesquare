package com.jamid.codesquare.ui.home.feed

import android.annotation.SuppressLint
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.ProjectAdapter
import com.jamid.codesquare.adapter.recyclerview.ProjectViewHolder
import com.jamid.codesquare.convertDpToPx
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.ui.PagerListFragment
import com.jamid.codesquare.updateLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class FeedFragment: PagerListFragment<Project, ProjectViewHolder>() {

    init {
        shouldHideRecyclerView = true
    }

    @SuppressLint("InflateParams")
    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val query = Firebase.firestore.collection("projects")

        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            getItems { viewModel.getFeedItems(query) }
        }

        recyclerView?.itemAnimator = null

        val tagsContainerView = layoutInflater.inflate(R.layout.tags_container, null, false)

        binding.pagerRoot.addView(tagsContainerView)
        val params = tagsContainerView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = binding.pagerRoot.id
        params.topToTop = binding.pagerRoot.id
        params.endToEnd = binding.pagerRoot.id
        tagsContainerView.layoutParams = params

        tagsContainerView.updateLayout(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        recyclerView?.setPadding(0, convertDpToPx(48), 0, convertDpToPx(8))

        val container = tagsContainerView.findViewById<ChipGroup>(R.id.tags_holder)
        container.findViewById<Chip>(R.id.random)?.setOnClickListener {
            if (currentUser != null) {
                getItems {
                    viewModel.getFeedItems(query)
                }
            }
        }

        viewModel.currentUser.observe(viewLifecycleOwner) {
            if (it != null) {
                container.removeViews(1, container.childCount - 1)
                if (it.interests.isEmpty()) {
                    container.addUpdateInterestButton()
                } else {
                    for (interest in it.interests) {
                        container.addTag(interest)
                    }
                }

                if (isEmpty.value == true) {
                    pagingAdapter.refresh()
                }

            }
        }

        /*isEmpty.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        pagingAdapter.refresh()
                    }
                }
            }
        }*/

    }

    private fun ChipGroup.addUpdateInterestButton() {
        val chip = Chip(requireContext())
        chip.text = "Update interests"
        chip.isCheckable = false
        chip.isCloseIconVisible = false

        chip.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_editProfileFragment, null)
        }

        addView(chip)

    }

    private fun ChipGroup.addTag(tag: String) {
        tag.trim()
        val chip = Chip(requireContext())
        chip.text = tag
        chip.isCheckable = true
        chip.isCloseIconVisible = false
//        chip.checkedIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_700))

        chip.setOnClickListener {

            val query = Firebase.firestore.collection("projects")
                .whereArrayContains("tags", tag)

            getItems {
                viewModel.getFeedItems(query, tag)
            }
        }

        addView(chip)

    }

    companion object {
        @JvmStatic
        fun newInstance() = FeedFragment()
    }

    override fun getAdapter(): PagingDataAdapter<Project, ProjectViewHolder> {
        return ProjectAdapter()
    }

}