package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.algolia.search.saas.Client
import com.algolia.search.saas.Index
import com.algolia.search.saas.Query
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.viewpager.SearchPagerAdapter
import com.jamid.codesquare.data.SearchResult
import com.jamid.codesquare.databinding.FragmentSearchBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

class SearchFragment: Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var client: Client
    private lateinit var index: Index
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        val searchItem = toolbar.menu.getItem(0)
        searchItem.expandActionView()

        searchItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                findNavController().navigateUp()
                return true
            }
        })

        (searchItem.actionView as SearchView).setOnQueryTextListener(this)
        (searchItem.actionView as SearchView).isSubmitButtonEnabled = true


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_1 -> {
                //
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        client = Client("7HGJNUJMWZ", "b35045d67a73575dde3d008e52f5da34")
        index = client.getIndex("projects")

        binding.searchPager.adapter = SearchPagerAdapter(requireActivity())
        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.main_tab_layout)

        TabLayoutMediator(tabLayout, binding.searchPager) { t, p ->
            when (p) {
                0 -> {
                    t.text = "Projects"
                }
                1 -> {
                    t.text = "Users"
                }
            }
        }.attach()

    }

    companion object {
        private const val TAG = "SearchFragment"
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        val progress = requireActivity().findViewById<ProgressBar>(R.id.main_progress_bar)
        progress.show()
        if (query != null) {
            val attribute: String

            val areProjectsBeingSearched = binding.searchPager.currentItem == 0

            if (areProjectsBeingSearched) {
                index = client.getIndex("projects")
                attribute = "title"
            } else {
                index = client.getIndex("users")
                attribute = "name"
            }

            index.searchAsync(Query(query)) { jsonObject, b ->
                if (jsonObject != null) {

                    val ss = jsonObject.toString()

                    val titles = findValuesForKey(attribute, ss)
                    val ids = findValuesForKey("objectId", ss)

                    val list = mutableListOf<SearchResult>()

                    for (i in ids.indices) {
                        list.add(SearchResult(ids[i], titles[i], areProjectsBeingSearched))
                    }

                    if (binding.searchPager.currentItem == 0) {
                        viewModel.setProjectsResult(list)
                    } else {
                        viewModel.setUsersResult(list)
                    }

                    progress.hide()

                }
            }
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

    private fun findValuesForKey(key: String, jsonString: String): List<String> {
        var index: Int
        val result = mutableListOf<String>()
        index = jsonString.indexOf(key, 0, true)
        Log.d(TAG, "Starting index for key = $key => $index")
        while (index != -1) {
            var valueString = ""
            for (i in (index + key.length + 3) until jsonString.length) {
                if (jsonString[i] != '\"') {
                    valueString += jsonString[i]
                } else {
                    break
                }
            }

            if (!valueString.contains('=') && valueString.isNotEmpty()) {
                result.add(valueString)
            }

            index = jsonString.indexOf(key, index + 1, true)
        }

        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setProjectsResult(null)
        viewModel.setUsersResult(null)
    }

}

