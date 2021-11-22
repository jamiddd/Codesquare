package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.NotificationAdapter
import com.jamid.codesquare.adapter.recyclerview.NotificationViewHolder
import com.jamid.codesquare.data.Notification

@ExperimentalPagingApi
class NotificationFragment: PagerListFragment<Notification, NotificationViewHolder>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.notifications_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_notifications -> {
                Firebase.firestore.collection("users")
                    .document(viewModel.currentUser.value!!.id)
                    .collection("notifications")
                    .limit(50)
                    .get()
                    .addOnSuccessListener {

                        val batch = Firebase.firestore.batch()

                        it.forEach { it1 ->
                            batch.delete(it1.reference)
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                viewModel.clearAllNotifications()
                            }.addOnFailureListener { it1 ->
                                Log.e(TAG, it1.localizedMessage.orEmpty())
                            }

                    }.addOnFailureListener {
                        Log.e(TAG, it.localizedMessage.orEmpty())
                    }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        val currentUser = viewModel.currentUser.value!!
        val query = Firebase.firestore.collection("users")
            .document(currentUser.id)
            .collection("notifications")

        getItems {
            viewModel.getNotifications(query)
        }

        recyclerView?.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        noItemsText?.text = "No notifications at the moment"

        isEmpty.observe(viewLifecycleOwner) {
            val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.main_toolbar)
            toolbar?.menu?.getItem(0)?.isVisible = !it
        }

    }

    override fun getAdapter(): PagingDataAdapter<Notification, NotificationViewHolder> {
        return NotificationAdapter()
    }

    companion object {
        private const val TAG = "NotificationFragment"
    }
}