package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostAdapter3
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.Comment
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentPost2Binding
import com.jamid.codesquare.listeners.ChipClickListener
import com.jamid.codesquare.listeners.CommentMiniListener
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class PostFragment2 : BaseFragment<FragmentPost2Binding>(), CommentMiniListener, ChipClickListener {

    private lateinit var post: Post
    private lateinit var userAdapter: UserAdapter
    private lateinit var postAdapter: PostAdapter3
    private val staticList = mutableListOf<Post>()

    companion object {
        const val TAG = "PostFragment2"
        private const val CHIP_LINKS = "chip_links"
        private const val CHIP_TAGS = "chip_tags"

        fun newInstance(bundle: Bundle) = PostFragment2().apply {
            arguments = bundle
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post = arguments?.getParcelable(POST) ?: throw NullPointerException("Post is null")
        setStaticPostRecycler()
    }

    private fun setStaticPostRecycler() {
        val list = listOf(post, Post.newInstance(UserManager.currentUser).apply { isAd = true })
        staticList.clear()
        staticList.addAll(list)

        postAdapter = PostAdapter3(viewLifecycleOwner, activity, activity).apply {
            shouldShowJoinButton = true
            allowContentClick = false
        }

        binding.staticPostRecycler.apply {
            adapter = postAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
        }

        postAdapter.submitList(staticList)

        binding.staticPostRecycler.post {
            setPostExtraContent()
            setJoinBtn()
            setSimilarPosts()
        }
    }

    private fun setSimilarPosts() = runDelayed(1700) {
        val query = if (post.tags.isNotEmpty()) {
            Firebase.firestore.collection(POSTS)
                .whereArrayContainsAny(TAGS, post.tags.take(minOf(post.tags.size,10)))
        } else {
            Firebase.firestore.collection(POSTS)
        }

        query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener {
                if (this@PostFragment2.isVisible) {
                    if (!it.isEmpty) {
                        val posts = it.toObjects(Post::class.java).filter { p -> p.id != post.id }
                        processPosts(posts)
//                        val posts = processPosts(it.toObjects(Post::class.java).toTypedArray()).filter { p -> p.id != post.id }

                        if (posts.isNotEmpty()) {
                            binding.similarPostsHeader.show()
                            binding.divider21.show()
                            binding.relatedPostsRecycler.show()
                            val pAdapter = PostAdapter3(viewLifecycleOwner, activity, activity)

                            binding.relatedPostsRecycler.apply {
                                adapter = pAdapter
                                addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
                                layoutManager = LinearLayoutManager(requireContext())
                            }

                            pAdapter.submitList(posts.toList())

                            binding.root.setOnScrollChangeListener { _, _, _, _, _ ->
                                val posArray = IntArray(2)
                                binding.similarPostsHeader.getLocationOnScreen(posArray)

                                if (posArray[1] < 100 && binding.similarPostsHeader.isVisible) {
                                    activity.binding.mainToolbar.title = "Similar posts"
                                } else {
                                    activity.binding.mainToolbar.title = "Post"
                                }
                            }
                        } else {
                            binding.similarPostsHeader.hide()
                            binding.divider21.hide()
                            binding.relatedPostsRecycler.hide()
                        }
                    } else {
                        binding.similarPostsHeader.hide()
                        binding.divider21.hide()
                        binding.relatedPostsRecycler.hide()
                    }
                }
            }.addOnFailureListener {
                if (this@PostFragment2.isVisible) {
                    binding.similarPostsHeader.hide()
                    binding.divider21.hide()
                    binding.relatedPostsRecycler.hide()
                }
            }
    }

    private fun setPostExtraContent() = runDelayed(700) {
        binding.postExtraItem.root.show()

        binding.postExtraItem.apply {
            // Tags related
            if (post.tags.isNotEmpty()) {
                tagsHeader.show()
                postTags.show()
                addTags(post.tags)
            } else {
                tagsHeader.hide()
                postTags.hide()
            }

            // links related
            if (post.sources.isNotEmpty()) {
                linksHeader.show()
                postLinks.show()
                addLinks(post.sources)
            } else {
                linksHeader.hide()
                postLinks.hide()
            }
        }

        // Contributors related
        setContributors()
    }

    private fun addLinks(links: List<String>) {
        val d = ContextCompat.getDrawable(requireContext(), R.drawable.forward_icon)
        binding.postExtraItem.postLinks.addTagChips(
            links,
            chipIcon = d,
            tag = CHIP_LINKS,
            chipClickListener = this
        )
    }

    override fun onLongClick(chip: Chip) {
        super.onLongClick(chip)
        when (chip.tag) {
            CHIP_TAGS -> {
                val choices = arrayListOf(OPTION_28)
                val icons = arrayListOf(R.drawable.ic_round_add_24)

                activity.optionsFragment = OptionsFragment.newInstance(
                    title = "\"${chip.text}\"",
                    options = choices,
                    icons = icons,
                    tag = tag
                )
                activity.optionsFragment?.show(
                    requireActivity().supportFragmentManager,
                    OptionsFragment.TAG
                )
            }
        }
    }

    override fun onClick(chip: Chip) {
        super.onClick(chip)
        when (chip.tag) {
            CHIP_LINKS -> {
                if (chip.text.startsWith("https://") || chip.text.startsWith("http://")) {
                    activity.onLinkClick(chip.text.toString())
                } else {
                    activity.onLinkClick("https://" + chip.text.toString())
                }
            }
            CHIP_TAGS -> {
                findNavController().navigate(
                    R.id.tagFragment,
                    bundleOf(TITLE to chip.text, "tag" to chip.text, SUB_TITLE to "Posts related to ${chip.text}")
                )
            }
        }
    }

    private fun setContributors() {

        userAdapter = UserAdapter()

        binding.postExtraItem.postContributorsRecycler.apply {
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            adapter = userAdapter
            layoutManager =
                LinearLayoutManager(activity)
            OverScrollDecoratorHelper.setUpOverScroll(
                this,
                OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
            )
        }

        Firebase.firestore.collection(USERS)
            .whereArrayContains(COLLABORATIONS, post.id)
            .limit(5)
            .get()
            .addOnSuccessListener {
                if (it != null && !it.isEmpty) {
                    val contributors = mutableListOf<User>()
                    val users = it.toObjects(User::class.java)
                    contributors.addAll(users)

                    FireUtility.getUser(post.creator.userId) { it1 ->
                        if (it1 != null) {
                            contributors.add(it1)
                        }
                        onContributorsFetched(contributors)
                    }
                } else {
                    FireUtility.getUser(post.creator.userId) { it1 ->
                        if (it1 != null) {
                            onContributorsFetched(listOf(it1))
                        }
                    }
                }

                binding.postExtraItem.contributorsHeader.show()
                binding.postExtraItem.postContributorsRecycler.show()

            }.addOnFailureListener {
                Log.e(TAG, "setContributors: ${it.localizedMessage}")
                Snackbar.make(
                    binding.root,
                    "Something went wrong while trying to fetch contributors. Try again later ..",
                    Snackbar.LENGTH_LONG
                )
                    .setBackgroundTint(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.error_color
                        )
                    )
                    .show()
            }
    }

    private fun onContributorsFetched(contributors: List<User>) = requireActivity().runOnUiThread {
        userAdapter.submitList(contributors)
        val list = arrayListOf<User>()
        for (item in contributors) {
            list.add(item)
        }

        binding.postExtraItem.contributorsHeader.setOnClickListener {
            findNavController().navigate(
                R.id.postContributorsFragment, bundleOf(
                    POST to post, TITLE to "Contributors", SUB_TITLE to post.name
                )
            )
        }
    }

    private fun addTags(tags: List<String>) {
        binding.postExtraItem.postTags.addTagChips(
            tags,
            tag = CHIP_TAGS,
            chipClickListener = this
        )
    }

    override fun onOptionClick(comment: Comment) {

    }

    private fun setJoinBtn() {

    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPost2Binding {
        return FragmentPost2Binding.inflate(inflater)
    }

}