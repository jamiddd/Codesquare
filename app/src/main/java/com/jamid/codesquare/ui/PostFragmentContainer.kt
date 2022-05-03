package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.PostFragmentContainerBinding

@ExperimentalPagingApi
class PostFragmentContainer: BaseFragment<PostFragmentContainerBinding, MainViewModel>() {

    private lateinit var post: Post
    override val viewModel: MainViewModel by activityViewModels()
    private var isPrimaryFragment = true

    override fun getViewBinding(): PostFragmentContainerBinding {
        return PostFragmentContainerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        post = arguments?.getParcelable(POST) ?: return
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* inflating the first fragment on start*/
        if (childFragmentManager.backStackEntryCount == 0) {
            val frag = PostFragment.newInstance(bundleOf(POST to post, "image_pos" to 0))
            childFragmentManager.beginTransaction()
                .add(binding.postFragContainer.id, frag, PostFragment.TAG)
                .addToBackStack(PostFragment.TAG)
                .commit()
        }

        setJoinBtn()

        childFragmentManager.addOnBackStackChangedListener {

            isPrimaryFragment = childFragmentManager.backStackEntryCount == 1
            setJoinBtn()

            val topFragment = childFragmentManager.fragments.lastOrNull()
            if (topFragment != null) {
                setToolbarForFragment(topFragment)
            }

            updateNavigation()

        }
    }

    fun getCurrentFragmentTag(): String {
        val lastFragment = childFragmentManager.fragments.lastOrNull()
        return if (lastFragment != null) {
            when (lastFragment) {
                is PostFragment -> {
                    PostFragment.TAG
                }
                is PostContributorsFragment -> {
                    PostContributorsFragment.TAG
                }
                else -> {
                    lastFragment::class.java.simpleName
                }
            }
        } else {
            Log.d(TAG, "onViewCreated: Last fragment is null")
            "NULL"
        }
    }

    override fun onResume() {
        super.onResume()
        updateNavigation()
    }

    private fun updateNavigation() {
        if (isPrimaryFragment) {
            // We care about select mode only if the current child fragment is chat fragment
            setNavigation {
                findNavController().navigateUp()
            }
        } else {
            setNavigation {
                childFragmentManager.popBackStack()
            }
        }
    }

    private fun setNavigation(onNavigation: () -> Unit) {
        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onNavigation()
        }

        activity.binding.mainToolbar.setNavigationOnClickListener {
            onNavigation()
        }
    }


    /**
     * To send a request to join in the current post
     *
     * Check :
     * 1. if the post is made by the current user, in which case current user can send request to himself
     * 2. if the post is already a collaboration of the current user in which case no need to show
     * 3. if the post is already requested, in which case show the button but set it to undo functionality
     * 4. and lastly idle state, which is normal state
     *
     * */
    private fun setJoinBtn() {
        activity.binding.mainPrimaryBtn.apply {

            when {
                !isPrimaryFragment -> hide()
                post.isMadeByMe -> hide()
                post.isCollaboration -> hide()
                post.isRequested -> {
                    show()
                    text = getString(R.string.undo_request)
                    icon = getImageResource(R.drawable.ic_arrow_undo)

                    setOnClickListener {
                        activity.onPostUndoClick(post) {
                            post = it
                            setJoinBtn()
                        }
                    }
                }
                else -> {
                    show()
                    text = getString(R.string.join_post)
                    icon = getImageResource(R.drawable.ic_round_add_24)

                    setOnClickListener {
                        activity.onPostJoinClick(post) {
                            post = it
                            setJoinBtn()
                        }
                    }
                }
            }
        }
    }

    fun setJoinBtnForChildScroll(v1: NestedScrollView) {
        v1.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY && scrollY - oldScrollY >= 100) {
                // slide down
                activity.binding.mainPrimaryBtn.shrink()
            }

            if (scrollY < oldScrollY && oldScrollY - scrollY >= 50) {
                // slide up
                activity.binding.mainPrimaryBtn.extend()
            }
        }
    }


    /**
     * This function assumes that a title is provided in the fragment arguments for all child fragments
     * for a unified design structure
     * */
    private fun setToolbarForFragment(fragment: Fragment) {
        val args = fragment.arguments

        if (args != null) {
            val title = args.getString(TITLE)
            val subtitle = args.getString(SUB_TITLE)

            if (fragment !is PostFragment) {
                activity.binding.mainToolbar.title = title
                activity.binding.mainToolbar.subtitle = subtitle
            } else {
                activity.binding.mainToolbar.title = "Post"
                activity.binding.mainToolbar.subtitle = ""
            }

        }

    }

    /**
     * This function assumes that title and/or subtitle will be provided in the bundle for any particular
     * child fragment of this current fragment
     * */
    fun navigate(tag: String, bundle: Bundle = bundleOf()) {
        val fragment = getFragmentByTag(tag, bundle)
        hideKeyboard()
        childFragmentManager.beginTransaction()
            .add(binding.postFragContainer.id, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }


}