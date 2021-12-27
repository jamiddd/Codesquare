package com.jamid.codesquare.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@ExperimentalPagingApi
class HomeFragment: Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var toolbar: MaterialToolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun setBitmapDrawable(bitmap: Bitmap?) {
        val drawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
        drawable.cornerRadius = convertDpToPx(24, requireContext()).toFloat()
        var isMenuReady = false
        while (!isMenuReady) {
            val menu = toolbar?.menu
            isMenuReady = menu != null && menu.size() > 3 && menu.findItem(R.id.profile) != null
            if (menu == null) {
                Log.d(TAG, "Menu is null")
            } else {
                Log.d(TAG, "Menu size is ${menu.size()} and profile item is ${menu.findItem(R.id.profile)}")
            }

        }
        requireActivity().runOnUiThread {
            toolbar?.menu?.getItem(3)?.icon = drawable
        }
    }

    private fun downloadBitmapUsingFresco(photo: String, onComplete: (bitmap: Bitmap?) -> Unit) {

        val imagePipeline = Fresco.getImagePipeline()
        val imageRequest = ImageRequestBuilder.newBuilderWithSource(photo.toUri())
            .build()

        val dataSource = imagePipeline.fetchDecodedImage(imageRequest, this)

        dataSource.subscribe(object: BaseBitmapDataSubscriber() {
            override fun onNewResultImpl(bitmap: Bitmap?) {
                onComplete(bitmap)
            }

            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                onComplete(null)
            }

        }, Executors.newSingleThreadExecutor())

    }

    private fun setCurrentUserPhotoAsDrawable(photo: String) = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
        val currentSavedBitmap = viewModel.currentUserBitmap
        if (currentSavedBitmap != null) {
            setBitmapDrawable(currentSavedBitmap)
        } else {
            downloadBitmapUsingFresco(photo) {
                viewModel.currentUserBitmap = it
                setBitmapDrawable(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
        toolbar = requireActivity().findViewById(R.id.main_toolbar)

        toolbar?.setOnClickListener {
            if (binding.homeViewPager.currentItem == 0) {
                val recyclerView = activity?.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            } else {
                val recyclerView = activity?.findViewById<RecyclerView>(R.id.pager_items_recycler)
                recyclerView?.smoothScrollToPosition(0)
            }
        }
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
                findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null, slideRightNavOptions())
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
        return binding.root
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)
        val tabLayout = activity.findViewById<TabLayout>(R.id.main_tab_layout)
        TabLayoutMediator(tabLayout, binding.homeViewPager) { a, b ->
            when (b) {
                0 -> a.text = "Projects"
                1 -> a.text = "Chats"
            }
        }.attach()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        (binding.homeViewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val mAuth = Firebase.auth
        if (mAuth.currentUser == null || mAuth.currentUser?.isEmailVerified == false) {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, slideRightNavOptions())
        }

        val currentUser = UserManager.currentUser
        val currentUserPhoto = currentUser.photo
        if (currentUserPhoto != null) {
            setCurrentUserPhotoAsDrawable(currentUserPhoto)
        }

        /*UserManager.currentUserLive.observe(viewLifecycleOwner) {
            if (it != null) {
                val currentUserPhoto = it.photo
                if (currentUserPhoto != null) {
                    setCurrentUserPhotoAsDrawable(currentUserPhoto)
                }
            }
        }*/
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}