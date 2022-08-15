package com.jamid.codesquare.ui.profile

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.paging.map
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
class PostsFragment: DefaultPagingFragment<Post2, SuperPostViewHolder>() {

    @OptIn(ExperimentalPagingApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val action = resources.getDimension(R.dimen.action_bar_height).toInt()
        binding.pagerItemsRecycler.setPadding(0, 0, 0, action)

        val subFieldName = "$CREATOR.$USER_ID"

        val otherUser = arguments?.getParcelable<User>(USER)
        if (otherUser == null) {
            val currentUser = UserManager.currentUser
            val query = Firebase.firestore.collection(POSTS)
                .whereEqualTo(ARCHIVED, false)
                .whereEqualTo(subFieldName, currentUser.id)

            binding.pagerNoItemsText.text = getString(R.string.empty_user_posts_greet)

            getItems(viewLifecycleOwner) {
                viewModel.getCurrentUserPosts(query).map {
                    it.map { p ->
                        Post2.Collab(p)
                    }
                }
            }

        } else {
            val query = Firebase.firestore
                .collection(POSTS)
                .whereEqualTo(ARCHIVED, false)
                .whereEqualTo(subFieldName, otherUser.id)

            getItems(viewLifecycleOwner) {
                viewModel.getOtherUserPosts(query, otherUser).map {
                    it.map { p ->
                        Post2.Collab(p)
                    }
                }
            }
        }

        setAppBarRecyclerBehavior()

    }

    override fun onPagingDataChanged(itemCount: Int) {
        super.onPagingDataChanged(itemCount)
        val otherUser = arguments?.getParcelable<User>(USER)
        if (itemCount == 0) {
            binding.noDataImage.updateLayoutParams<ConstraintLayout.LayoutParams> {
                verticalBias = 0.18f
            }

            binding.pagerNoItemsText.show()

            if (otherUser == null) {
                binding.pagerNoItemsText.text = "No posts yet. Create one to start collaborating ..."
                binding.pagerActionBtn.show()

                binding.pagerActionBtn.text = getString(R.string.create_post)
                binding.pagerActionBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_24)

                binding.pagerActionBtn.setOnClickListener {
                    findNavController().navigate(R.id.createPostFragment, null)
                }
            } else {
                binding.pagerNoItemsText.text = "No posts by ${otherUser.name}"
            }

        } else {
            binding.pagerNoItemsText.hide()
            binding.pagerActionBtn.hide()
        }
    }

    override fun getPagingAdapter(): PagingDataAdapter<Post2, SuperPostViewHolder> {
        return PostAdapter(viewLifecycleOwner, activity)
    }



    companion object {

        private const val TAG = "PostsFragment"

        fun newInstance(user: User? = null)
            = PostsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(USER, user)
                }
        }

    }

    override fun getDefaultInfoText(): String {
        return "No posts"
    }

}