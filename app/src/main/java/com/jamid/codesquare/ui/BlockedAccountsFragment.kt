package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jamid.codesquare.*
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.BlockedAccountItemBinding
import com.jamid.codesquare.databinding.FragmentBlockedAccountsBinding

@OptIn(ExperimentalPagingApi::class)
class BlockedAccountsFragment: BaseFragment<FragmentBlockedAccountsBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()
    private val blockedList = mutableListOf<String>()
    private lateinit var blockAccountAdapter: BlockAccountsAdapter

    override fun getViewBinding(): FragmentBlockedAccountsBinding {
        return FragmentBlockedAccountsBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val blockedUsers = UserManager.currentUser.blockedUsers

        if (blockedUsers.isEmpty()) {
            binding.noBlockedAccounts.show()
        }

        blockedList.addAll(blockedUsers)

        blockAccountAdapter = BlockAccountsAdapter(blockedList)

        binding.blockedAccountsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = blockAccountAdapter
        }
    }

    inner class BlockAccountsAdapter(private val userIds: List<String>): RecyclerView.Adapter<BlockAccountViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockAccountViewHolder {
            return BlockAccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.blocked_account_item, parent, false))
        }

        override fun onBindViewHolder(holder: BlockAccountViewHolder, position: Int) {
            holder.bind(userIds[position], position)
        }

        override fun getItemCount(): Int {
            return userIds.size
        }
    }

    inner class BlockAccountViewHolder(private val v: View): RecyclerView.ViewHolder(v) {

        fun bind(userId: String, pos: Int) {
            val binding = BlockedAccountItemBinding.bind(v)
            binding.unblockBtn.hide()

            FireUtility.getUser(userId) {
                val res = it ?: return@getUser
                when (res) {
                    is Result.Error -> Log.e(TAG, "bind: ${res.exception.localizedMessage}")
                    is Result.Success -> {
                        val user = res.data

                        binding.blockedAccountImg.setImageURI(user.photo)
                        binding.blockedAccountName.text = user.name
                        binding.unblockBtn.show()

                        binding.unblockBtn.setOnClickListener {

                            binding.unblockBtn.disappear()
                            binding.unblockProgress.show()

                            FireUtility.unblockUser(user) { it1 ->
                                if (it1.isSuccessful) {
                                    binding.unblockBtn.show()
                                    binding.unblockProgress.hide()

                                    blockedList.removeAt(pos)
                                    blockAccountAdapter.notifyItemRemoved(pos)
                                    Snackbar.make(activity.binding.root, "${user.name} is unblocked.", Snackbar.LENGTH_LONG).show()
                                } else {
                                    Log.e(TAG, "bind: ${it1.exception?.localizedMessage}")
                                }
                            }
                        }
                    }
                }
            }
        }

    }


}