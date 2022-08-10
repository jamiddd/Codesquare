package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.View
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.POST
import com.jamid.codesquare.POSTS
import com.jamid.codesquare.SUB_TITLE
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.Post2
import java.util.*
// something simple
class TagFragment: DefaultPagingFragment<Post2, SuperPostViewHolder>() {

    private var tag1: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tag1 = arguments?.getString("tag", POST).orEmpty()

        val subTitle = arguments?.getString(SUB_TITLE).orEmpty()
        activity.binding.mainToolbar.subtitle = subTitle

        val t1 = tag1.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val t2 = tag1.uppercase()
        val t3 = tag1.lowercase()

        val query = Firebase.firestore.collection(POSTS)
            .whereArrayContainsAny("tags", listOf(tag1, t1, t2, t3))

        getItems(viewLifecycleOwner) {
            viewModel.getTagPosts(tag1, query)
        }

        binding.pagerItemsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.pagerItemsRecycler.itemAnimator = null
    }

    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(lifecycleOwner = viewLifecycleOwner, activity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.binding.mainToolbar.subtitle = null
    }

    companion object {

        const val TAG = "TagFragment"

        fun newInstance(bundle: Bundle) = TagFragment().apply {
            arguments = bundle
        }
    }

    override fun getDefaultInfoText(): String {
        return "No posts based on $tag1"
    }

}