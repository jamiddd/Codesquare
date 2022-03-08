package com.jamid.codesquare.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.databinding.FragmentHomeBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.SubscriptionFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@ExperimentalPagingApi
class HomeFragment: Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun setBitmapDrawable(bitmap: Bitmap) {

        val length = resources.getDimension(R.dimen.unit_len) * 6

        val drawable = RoundedBitmapDrawableFactory.create(resources, bitmap).also {
            it.cornerRadius = length
        }

        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_toolbar)
        requireActivity().runOnUiThread {
            if (toolbar != null) {
                val menu = toolbar.menu
                if (menu != null) {
                    if (menu.size() > 3) {
                        val profileItem = menu.getItem(3)
                        if (profileItem != null) {
                            profileItem.icon = drawable
                        }
                    }
                }
            }
        }
    }

    private fun setCurrentUserPhotoAsDrawable(photo: String) = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
        val currentSavedBitmap = viewModel.currentUserBitmap
        if (currentSavedBitmap != null) {
            setBitmapDrawable(currentSavedBitmap)
        } else {
            downloadBitmapUsingFresco(requireContext(), photo) {
                viewModel.currentUserBitmap = it
                if (it != null) {
                    setBitmapDrawable(it)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
        requireActivity().findViewById<Toolbar>(R.id.main_toolbar)?.setOnClickListener {
            if (binding.homeViewPager.currentItem == 0) {
                val recyclerView = activity?.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            } else {
                val recyclerView = activity?.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            }
        }
    }

    private fun setImage() {
        val currentUser = UserManager.currentUser
        setCurrentUserPhotoAsDrawable(currentUser.photo)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.notifications -> {
                findNavController().navigate(R.id.action_homeFragment_to_notificationCenterFragment, bundleOf("type" to 0), slideRightNavOptions())
                true
            }
            R.id.search -> {
                findNavController().navigate(R.id.action_homeFragment_to_preSearchFragment, null, slideRightNavOptions())
                true
            }
            R.id.create_project -> {

                val currentUser = UserManager.currentUser
                if (currentUser.premiumState.toInt() == 1 || currentUser.projects.size < 2) {
                    findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null, slideRightNavOptions())
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Collab")
                        .setMessage("You have already created 2 projects. To create more, upgrade your subscription plan!")
                        .setPositiveButton("Upgrade") { _, _ ->
                            val act = activity as MainActivity
                            act.subscriptionFragment = SubscriptionFragment()
                            act.subscriptionFragment?.show(act.supportFragmentManager, "SubscriptionFragment")
                        }.setNegativeButton("Cancel") { a, _ ->
                            a.dismiss()
                        }
                        .show()
                }

                true
            }
            R.id.profile -> {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment, null, slideRightNavOptions())
                true
            }
            else -> true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar).inflateMenu(R.menu.generic_menu)
        return binding.root
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            // for delayed toolbar
            setImage()
        }

        binding.homeViewPager.offscreenPageLimit = 2

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        TabLayoutMediator(tabLayout, binding.homeViewPager) { a, b ->
            when (b) {
                0 -> a.text = "Projects"
                1 -> a.text = "Chats"
            }
        }.attach()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.homeViewPager.currentItem == 0) {
                requireActivity().finish()
            } else {
                binding.homeViewPager.setCurrentItem(0, true)
            }
        }

        (binding.homeViewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val mAuth = Firebase.auth
        if (mAuth.currentUser == null) {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, slideRightNavOptions())
        }

        setImage()

    }

}