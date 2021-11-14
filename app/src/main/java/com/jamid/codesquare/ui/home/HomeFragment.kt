package com.jamid.codesquare.ui.home

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.HorizontalScrollView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.viewpager.MainViewPagerAdapter
import com.jamid.codesquare.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL

class HomeFragment: Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        if (viewModel.currentUserBitmap != null) {
            val d = BitmapDrawable(resources, viewModel.currentUserBitmap)
            toolbar.menu.getItem(2).icon = d
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.search -> {
                findNavController().navigate(R.id.action_homeFragment_to_searchFragment, null)
                true
            }
            R.id.create_project -> {
                findNavController().navigate(R.id.action_homeFragment_to_createProjectFragment, null)
                true
            }
            R.id.profile -> {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment, null)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        if (Firebase.auth.currentUser == null) {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.main_tab_layout)

        binding.homeViewPager.adapter = MainViewPagerAdapter(activity)

        binding.homeViewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, binding.homeViewPager) { tab, pos ->
            if (pos == 0) {
                tab.text = "Projects"
            } else {
                tab.text = "Chats"
            }
        }.attach()

        viewModel.currentUser.observe(viewLifecycleOwner) { currentUser ->
            if (currentUser != null) {
                if (currentUser.photo != null) {
                    if (viewModel.currentUserBitmap == null) {
                        viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
                            try {
                                val isNetworkAvailable = viewModel.isNetworkAvailable.value
                                if (isNetworkAvailable != null && isNetworkAvailable) {
                                    val url = URL(currentUser.photo)
                                    val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                                    viewModel.currentUserBitmap = getRoundedCroppedBitmap(image)
                                    val d = BitmapDrawable(resources, viewModel.currentUserBitmap)
                                    activity.runOnUiThread {
                                        if (toolbar.menu.size() > 1) {
                                            toolbar.menu.getItem(2).icon = d
                                        }
                                    }
                                }
                            } catch (e: IOException) {
                                viewModel.setCurrentError(e)
                            }
                        }
                    } else {
                        val d = BitmapDrawable(resources, viewModel.currentUserBitmap)
                        if (toolbar.menu.hasVisibleItems() && toolbar.menu.size() == 3) {
                            toolbar.menu.getItem(2).icon = d
                        }
                    }
                } else {
                    //
                }

            }
        }

    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            toast(query)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

}