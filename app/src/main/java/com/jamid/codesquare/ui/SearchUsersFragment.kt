package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.adapter.recyclerview.VagueUserAdapter
import com.jamid.codesquare.data.QUERY_TYPE_USER
import com.jamid.codesquare.databinding.FragmentSearchUsersBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.show

@ExperimentalPagingApi
class SearchUsersFragment: Fragment() {

    private lateinit var binding: FragmentSearchUsersBinding
    private lateinit var vagueUserAdapter: VagueUserAdapter
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchUsersBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.recentSearchList.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                binding.noSearchedUsers.hide()
                val users = it.filter { it1 ->
                    it1.type == QUERY_TYPE_USER
                }

                if (users.isEmpty()) {
                    binding.noSearchedUsers.show()
                } else {

                    val ids = users.map { it1 ->
                        it1.id
                    }

                    vagueUserAdapter = VagueUserAdapter(ids, viewLifecycleOwner.lifecycleScope) { userId ->
                        viewModel.getLocalUser(userId)
                    }

                    binding.searchUsersRecycler.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = vagueUserAdapter
                    }

                }
            } else {
                binding.noSearchedUsers.show()
            }
        }

    }


    /*override fun onSearchItemClick(searchQuery: SearchQuery) {
        val userRef = Firebase.firestore.collection("users").document(searchQuery.id)
        FireUtility.getDocument(userRef) {
            if (it.isSuccessful) {
                val user = it.result.toObject(User::class.java)!!
                val bundle = bundleOf("user" to user)
                findNavController().navigate(R.id.action_searchFragment_to_profileFragment, bundle)

                viewModel.insertSearchQuery(searchQuery)
            } else {
                toast("Something went wrong !")
            }
        }
    }

    override fun onSearchItemForwardClick(query: SearchQuery) {
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
        val searchItem = toolbar.menu.getItem(0)
        // submit automatically
        (searchItem.actionView as SearchView).setQuery(query.queryString, true)
    }

    override fun onSearchOptionClick(view: View, query: SearchQuery) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
        popupMenu.inflate(R.menu.search_result_menu)

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.popup_remove_query -> {
                    viewModel.deleteSearchQuery(query)
                }
            }
            true
        }

        popupMenu.show()
    }*/
}