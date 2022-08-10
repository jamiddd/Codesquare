package com.jamid.codesquare.ui.profile

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostAdapter
import com.jamid.codesquare.adapter.recyclerview.SuperPostViewHolder
import com.jamid.codesquare.data.Post2
import com.jamid.codesquare.data.User
import com.jamid.codesquare.ui.DefaultPagingFragment
import kotlinx.coroutines.flow.map
// something simple
class CollaborationsFragment: DefaultPagingFragment<Post2, SuperPostViewHolder>() {

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val otherUser = arguments?.getParcelable<User>(USER)

        val action = resources.getDimension(R.dimen.action_bar_height).toInt()
        binding.pagerItemsRecycler.setPadding(0, 0, 0, action)

        var query: Query = Firebase.firestore.collection(POSTS)
            .whereEqualTo(ARCHIVED, false)

        val currentUser = UserManager.currentUser

        if (otherUser == null) {
            query = query.whereNotEqualTo("creator.userId", currentUser.id)
                .whereArrayContains(CONTRIBUTORS, currentUser.id)
                .orderBy("creator.userId")

            getItems(viewLifecycleOwner) {
                viewModel.getCollaborations(query).map {
                    it.map { p ->
                        Post2.Collab(p)
                    }
                }
            }
        } else {
            query = query.whereArrayContains(CONTRIBUTORS, otherUser.id)

            getItems(viewLifecycleOwner) {
                viewModel.getOtherUserCollaborations(query, otherUser)
            }
        }

        setAppBarRecyclerBehavior()

    }

    override fun onPagingDataChanged(itemCount: Int) {
        super.onPagingDataChanged(itemCount)
        val otherUser = arguments?.getParcelable<User>(USER)
        val currentUser = UserManager.currentUser
        if (itemCount == 0) {
            binding.pagerNoItemsText.show()

            binding.noDataImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                verticalBias = 0.18f
            }

            if (otherUser == null) {
                binding.pagerActionBtn.icon = getImageResource(R.drawable.ic_round_search_24)
                binding.pagerActionBtn.text = getString(R.string.search_for_posts)
                binding.pagerActionBtn.show()

                binding.pagerActionBtn.setOnClickListener {
                    findNavController().navigate(R.id.preSearchFragment)
                }
                binding.pagerNoItemsText.text = getString(R.string.empty_collaborations_greet)
            } else {
                if (otherUser.id == currentUser.id) {
                    binding.pagerActionBtn.icon = getImageResource(R.drawable.ic_round_search_24)
                    binding.pagerActionBtn.text = getString(R.string.search_for_posts)
                    binding.pagerActionBtn.show()

                    binding.pagerActionBtn.setOnClickListener {
                        findNavController().navigate(R.id.preSearchFragment)
                    }
                    binding.pagerNoItemsText.text = getString(R.string.empty_collaborations_greet)
                } else {
                    binding.pagerNoItemsText.text = "No collaborations by ${otherUser.name}"
                }
            }
        } else {
            binding.pagerNoItemsText.hide()
            binding.pagerActionBtn.hide()
        }
    }

    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(lifecycleOwner = viewLifecycleOwner, activity)
    }

    companion object {

        private const val TAG = "CollaborationsFrag"

        fun newInstance(user: User? = null) = CollaborationsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(USER, user)
            }
        }

    }

    override fun getDefaultInfoText(): String {
        return "No collaborations"
    }

}