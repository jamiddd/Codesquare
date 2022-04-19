package com.jamid.codesquare.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.adapter.recyclerview.MyInviteAdapter
import com.jamid.codesquare.data.Project
import com.jamid.codesquare.data.ProjectInvite
import com.jamid.codesquare.data.Result
import com.jamid.codesquare.databinding.FragmentInvitesBinding
import com.jamid.codesquare.listeners.ProjectMiniItemClickListener

@OptIn(ExperimentalPagingApi::class)
class InvitesFragment: Fragment(), ProjectMiniItemClickListener {

    private lateinit var binding: FragmentInvitesBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val invitesList = mutableListOf<ProjectInvite>()
    private lateinit var myInvitesAdapter: MyInviteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInvitesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myInvitesAdapter = MyInviteAdapter(this)

        binding.invitesRecycler.apply {
            adapter = myInvitesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.invitesReferesher.setOnRefreshListener {
            getInvitesSentByMe()
        }

        getInvitesSentByMe()

        viewModel.getCurrentUserProjects().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                if (it.size != UserManager.currentUser.projects.size) {
                    // get all projects
                    FireUtility.downloadAllUserProjects { it1 ->
                        val allProjectsResult = it1 ?: return@downloadAllUserProjects
                        when (allProjectsResult) {
                            is Result.Error -> viewModel.setCurrentError(allProjectsResult.exception)
                            is Result.Success -> {
                                viewModel.insertProjects(allProjectsResult.data)
                            }
                        }
                    }
                } else {
                    // we have all projects
                }
            } else {
                FireUtility.downloadAllUserProjects { it1 ->
                    val allProjectsResult = it1 ?: return@downloadAllUserProjects
                    when (allProjectsResult) {
                        is Result.Error -> viewModel.setCurrentError(allProjectsResult.exception)
                        is Result.Success -> {
                            viewModel.insertProjects(allProjectsResult.data)
                        }
                    }
                }
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getInvitesSentByMe() {
        Firebase.firestore.collectionGroup(INVITES)
            .whereEqualTo(SENDER_ID, UserManager.currentUserId)
            .get()
            .addOnSuccessListener { invitesSnapshot ->

                binding.invitesReferesher.isRefreshing = false

                if (invitesSnapshot != null && !invitesSnapshot.isEmpty) {

                    binding.noInvitesText.hide()

                    val invites = invitesSnapshot.toObjects(ProjectInvite::class.java)
                    invitesList.clear()
                    invitesList.addAll(invites)
                    myInvitesAdapter.submitList(invitesList)
                    myInvitesAdapter.notifyDataSetChanged()
                } else {
                    binding.noInvitesText.show()
                }
            }.addOnFailureListener {
                binding.noInvitesText.show()
                Log.e(TAG, "getInvitesSentByMe: ${it.localizedMessage}")
            }
    }

    override fun onInviteClick(project: Project, receiverId: String, onFailure: () -> Unit) {

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onRevokeInviteClick(invite: ProjectInvite, onFailure: () -> Unit) {
        FireUtility.revokeInvite(invite) {
            if (!it.isSuccessful) {
                Snackbar.make(binding.root, "Something went wrong while trying to revoke project invite", Snackbar.LENGTH_LONG).show()
                onFailure()
                viewModel.setCurrentError(it.exception)
            } else {
                invitesList.remove(invite)
                myInvitesAdapter.submitList(invitesList)
                myInvitesAdapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val TAG = "InvitesFragment"
    }

}