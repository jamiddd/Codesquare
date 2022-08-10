package com.jamid.codesquare.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.FireUtility
import com.jamid.codesquare.data.RankedRule
import com.jamid.codesquare.databinding.FragmentExtraBinding
import com.jamid.codesquare.toast

class ExtraFragment: BaseFragment<FragmentExtraBinding>() {


    // something simple
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addRuleBtn.setOnClickListener {

            if (binding.rankedRuleContentText.text.isNullOrBlank())
                return@setOnClickListener

            if (binding.rankedRuleHeadingText.text.isNullOrBlank())
                return@setOnClickListener

            val ruleHeading = binding.rankedRuleHeadingText.text.toString()
            val ruleContent = binding.rankedRuleContentText.text.toString()

            if (ruleHeading.isNotBlank() && ruleContent.isNotBlank()) {

                val rankRule = RankedRule().apply {
                    name = ruleHeading
                    content = ruleContent
                }

                FireUtility.addNewRankedRule(rankRule) {

                    binding.rankedRuleContentText.text.clear()
                    binding.rankedRuleHeadingText.text.clear()

                    if (it.isSuccessful) {
                        toast("Added new rule")
                    } else {
                        toast("Something went wrong")
                    }
                }
            }

        }

        /* For adding interest */

        /*val interest = binding.textInputLayout2.editText!!.text!!.trim().toString()

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
                       }*/
    }

    override fun onCreateBinding(inflater: LayoutInflater): FragmentExtraBinding {
        return FragmentExtraBinding.inflate(inflater)
    }

}