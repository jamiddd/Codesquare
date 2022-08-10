package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.PostFragmentContainerBinding

class PostFragmentContainer: BaseFragment<PostFragmentContainerBinding>() {

    private lateinit var post: Post
    override val viewModel: MainViewModel by activityViewModels()
    private var isPrimaryFragment = true

    override fun onCreateBinding(inflater: LayoutInflater): PostFragmentContainerBinding {
        return PostFragmentContainerBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        post = arguments?.getParcelable(POST) ?: return
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* inflating the first fragment on start*/
        if (childFragmentManager.backStackEntryCount == 0) {
            val frag = PostFragment2.newInstance(bundleOf(POST to post))
            childFragmentManager.beginTransaction()
                .add(binding.postFragContainer.id, frag, PostFragment2.TAG)
                .addToBackStack(PostFragment2.TAG)
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
                is PostFragment2 -> {
                    PostFragment2.TAG
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

            if (fragment !is PostFragment2) {
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