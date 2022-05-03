package com.jamid.codesquare.extra

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.*
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.databinding.FragmentExtraBinding

@OptIn(ExperimentalPagingApi::class)
class ExtraFragment: BaseFragment<FragmentExtraBinding, MainViewModel>() {

    override val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.button2.setOnClickListener {
            val interest = binding.textInputLayout2.editText!!.text!!.trim().toString()

            val x = binding.textInputLayout.editText!!.text!!.trim().toString()
            val associations = x.split(", ").map {
                it.lowercase().trim().replace(" ", "_")
            }

            val id = interest.lowercase().trim().replace(" ", "_")
            val interestItem = InterestItem(id, id, interest, System.currentTimeMillis(), associations, System.currentTimeMillis(), 0.001)

            Firebase.firestore.collection(INTERESTS)
                .document(interestItem.itemId)
                .set(interestItem)
                .addOnSuccessListener {
                    binding.textInputLayout.editText?.text?.clear()
                    binding.textInputLayout2.editText?.text?.clear()
                }.addOnFailureListener {
                    toast(it.localizedMessage!!.toString())
                }

        }

    }

    override fun getViewBinding(): FragmentExtraBinding {
        return FragmentExtraBinding.inflate(layoutInflater)
    }

}