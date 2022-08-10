package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import com.android.billingclient.api.BillingFlowParams
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.R
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.data.Post
import com.jamid.codesquare.databinding.RankedEntryFragmentBinding
import com.jamid.codesquare.toast
import java.util.*


class RankedEntryFragment: BaseFragment<RankedEntryFragmentBinding>() {

    private var selectedPost: Post? = null
    private var selectedCategory: String? = null

    override fun onCreateBinding(inflater: LayoutInflater): RankedEntryFragmentBinding {
        return RankedEntryFragmentBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.enterRankedBtn.isEnabled = false

        binding.selectProjectCategory.setOnClickListener {
            val pop = PopupMenu(requireContext(), binding.view2)
            pop.inflate(R.menu.ranked_category_menu)
            pop.setOnMenuItemClickListener { it1 ->
                when (it1.itemId) {
                    R.id.rank_category_technology -> {
                        selectedCategory = "technology"
                        binding.enterRankedBtn.isEnabled = selectedPost != null
                        binding.selectProjectCategory.text = selectedCategory!!.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        }
                        true
                    }
                    R.id.rank_category_creative -> {
                        selectedCategory = "creative"
                        binding.enterRankedBtn.isEnabled = selectedPost != null
                        binding.selectProjectCategory.text = selectedCategory!!.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        }
                        true
                    }
                    else -> true
                }
            }
            pop.show()
        }

        binding.selectProjectBtn.setOnClickListener {
            val postSelectorFragment = PostSelectorFragment {
                if (it.isNotEmpty()) {
                    binding.enterRankedBtn.isEnabled = selectedCategory != null
                    binding.selectProjectBtn.text = it.map { it1 -> it1.name }.firstOrNull()
                    selectedPost = it.first()
                } else {
                    binding.enterRankedBtn.isEnabled = false
                    binding.selectProjectBtn.text = "Select project"
                    selectedPost = null
                }
            }.apply { isSingleSelection = true }
            postSelectorFragment.show(activity.supportFragmentManager, "PostSelectorFragment")
        }

        val currentUser = UserManager.currentUser

        activity.playBillingController.isPurchaseAcknowledged.observe(viewLifecycleOwner) { isPurchaseAcknowledged ->
            if (isPurchaseAcknowledged == true) {
                /*if (subscriptionFragment != null && subscriptionFragment!!.isVisible) {
                    subscriptionFragment!!.dismiss()
                }
                navController.navigate(R.id.subscriberFragment, null, slideRightNavOptions())*/

                    if (selectedPost != null) {
                        val f = PostSubmissionFragment(selectedPost!!)
                        f.show(activity.supportFragmentManager, "PostSubmission")
                    }

            }
        }


        binding.enterRankedBtn.setOnClickListener {
            // start checkout
            // for now directly move to next screen
            if (!currentUser.hasTicket) {
                val billingClient = (activity as MainActivity?)?.playBillingController?.billingClient
                if (billingClient != null) {
                    val list = viewModel.productDetails.value
                    if (!list.isNullOrEmpty()) {
                        val detail = list.first()

                        val pp = mutableListOf(
                            BillingFlowParams.ProductDetailsParams
                                .newBuilder()
                                .setProductDetails(detail)
                                .build()
                        )

                        val flowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(pp)
                            .build()

                        billingClient.launchBillingFlow(requireActivity(), flowParams).responseCode
                    } else {
                        toast("Something went wrong!")
                    }
                }
            } else {
                if (selectedPost != null && selectedCategory != null) {
                    selectedPost!!.rankCategory = selectedCategory!!
                    val f = PostSubmissionFragment(selectedPost!!)
                    f.show(requireActivity().supportFragmentManager, "PostSubmission")
                }
            }

        }


    }

    companion object {
        private const val TAG = "RankedEntryFragment"
    }

}