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
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.PostAdapter3
import com.jamid.codesquare.adapter.recyclerview.UserAdapter
import com.jamid.codesquare.data.*
import com.jamid.codesquare.databinding.FragmentPost2Binding
import com.jamid.codesquare.listeners.ChipClickListener
import com.jamid.codesquare.listeners.CommentMiniListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper


class PostFragment2 : BaseFragment<FragmentPost2Binding>(), CommentMiniListener, ChipClickListener {

    private lateinit var post: Post
    private lateinit var userAdapter: UserAdapter
    private val staticPostAdapter: PostAdapter3 by lazy { PostAdapter3(viewLifecycleOwner, activity, activity).apply {
        shouldShowJoinButton = true
        allowContentClick = false
    }}
    private var pAdapter: PostAdapter3? = null
    private val adId = randomId()
    private val staticPostList = mutableListOf<Post2>(Post2.Advertise(adId))

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

        binding.staticPostRecycler.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
        }

        viewModel.getPost(post.id).observe(viewLifecycleOwner) {
            if (it != null) {
                binding.staticPostRecycler.adapter = staticPostAdapter
                if (staticPostList.size == 1) {
                    staticPostList.add(0, Post2.Collab(it))
                } else {
                    staticPostList[0] = Post2.Collab(it)
                }

                staticPostAdapter.submitList(staticPostList)
            }
        }

        setPostExtraContent()
        setSimilarPosts()
    }

    private fun setSimilarPosts() {

        FireUtility.getSimilarPosts(post) {
            if (it.isNotEmpty()) {
                viewModel.insertPosts(it)
            } else {
                Log.d(TAG, "setSimilarPosts: No similar posts")
            }
        }

        if (pAdapter == null)
            pAdapter = PostAdapter3(viewLifecycleOwner, activity, activity)

        if (post.tags.isNotEmpty()) {
            val randomTag = post.tags.random()
            viewModel.getSimilarPosts(post.id, randomTag).observe(viewLifecycleOwner) { posts ->
                if (!posts.isNullOrEmpty()) {
                    binding.similarPostsHeader.show()
                    binding.divider21.show()
                    binding.relatedPostsRecycler.show()

                    binding.relatedPostsRecycler.apply {
                        adapter = pAdapter
                        layoutManager = LinearLayoutManager(requireContext())
                    }

                    pAdapter?.submitList(posts.map { Post2.Collab(it) })

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
            }
        } else {
            onSimilarPostsNotFound()
        }
    }

    

    private fun onSimilarPostsNotFound() {
        binding.similarPostsHeader.hide()
        binding.divider21.hide()
        binding.relatedPostsRecycler.hide()
    }


    private fun setPostExtraContent() {
        binding.postExtraItem.root.show()

        binding.postExtraItem.apply {

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
        val d = ContextCompat.getDrawable(activity, R.drawable.forward_icon)
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
                    activity.supportFragmentManager,
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
                    bundleOf(
                        TITLE to chip.text,
                        "tag" to chip.text,
                        SUB_TITLE to "Posts related to ${chip.text}"
                    )
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

        runOnBackgroundThread {
            val contributorsFetchResult = withContext(Dispatchers.IO) {
                FireUtility.getContributors(post.chatChannel, 5)
            }

            when (contributorsFetchResult) {
                is Result.Error -> {
                    // TODO("Show error")
                }
                is Result.Success -> {
                    onContributorsFetched(contributorsFetchResult.data)
                }
            }
        }

    }

    private fun onContributorsFetched(contributors: List<User>) {
        binding.postExtraItem.apply {

            contributorsHeader.show()
            postContributorsRecycler.show()

            userAdapter.submitList(contributors)

            val list = arrayListOf<User>()
            for (item in contributors) {
                list.add(item)
            }

            contributorsHeader.setOnClickListener {
                findNavController().navigate(
                    R.id.postContributorsFragment, bundleOf(
                        POST to post, TITLE to "Contributors", SUB_TITLE to post.name
                    )
                )
            }
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

    override fun onCreateBinding(inflater: LayoutInflater): FragmentPost2Binding {
        return FragmentPost2Binding.inflate(inflater)
    }

}