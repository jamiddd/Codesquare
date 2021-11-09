package com.jamid.codesquare.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.HorizontalScrollView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

      /*  val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        toolbar.show()

        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.main_tab_layout)
        tabLayout.show()

        val searchItem = toolbar.menu.getItem(0)
        searchItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                toolbar.menu.getItem(1).isVisible = false
                toolbar.menu.getItem(2).isVisible = false

                tabLayout.hide()

                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                toolbar.menu.getItem(1).isVisible = true
                toolbar.menu.getItem(2).isVisible = true

                tabLayout.show()

                return true
            }
        })

        val searchView = (searchItem.actionView as SearchView)
        searchView.setOnQueryTextListener(this)
*/
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