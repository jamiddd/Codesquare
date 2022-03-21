package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingFlowParams
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.recyclerview.SubscriptionAdapter
import com.jamid.codesquare.data.Subscription
import com.jamid.codesquare.databinding.SubscriptionLayoutBinding
import com.jamid.codesquare.listeners.SubscriptionListener
import com.jamid.codesquare.show

@ExperimentalPagingApi
class SubscriptionFragment : BottomSheetDialogFragment(), SubscriptionListener {

    private lateinit var binding: SubscriptionLayoutBinding
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private val viewModel: MainViewModel by activityViewModels()
    private var lastPosition = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SubscriptionLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscriptionAdapter = SubscriptionAdapter(this)

        binding.subscriptionsRecycler.apply {
            adapter = subscriptionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.subscriptionDetails.observe(viewLifecycleOwner) { list ->
            if (!list.isNullOrEmpty()) {
                val subscriptions = list.map {
                    Subscription(it.sku, it.priceAmountMicros, it.price, it.priceCurrencyCode, it.subscriptionPeriod, false, it.description, System.currentTimeMillis(), false)
                }

                viewModel.setSubscriptions(subscriptions)
            }
        }

        viewModel.subscriptions.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Log.d(TAG, it.map {it1 -> it1.isSelected }.toString())
                subscriptionAdapter.submitList(it)
            } else {
                Log.d(TAG, "No subscriptions")
            }
        }


        binding.subscriptionDoneBtn.isEnabled = false

        binding.subscriptionCloseBtn.setOnClickListener {
            dismiss()
        }

        binding.subscriptionDoneBtn.setOnClickListener {
            val billingClient = (activity as MainActivity?)?.playBillingController?.billingClient
            if (billingClient != null) {

                binding.loadingScrim.show()

                val list = viewModel.subscriptionDetails.value
                if (!list.isNullOrEmpty()) {
                    val detail = list[lastPosition]
                    viewModel.setCurrentlySelectedSku(detail)
                    billingClient.launchBillingFlow(requireActivity(), BillingFlowParams.newBuilder().setSkuDetails(detail).build())
                }
            }
        }

    }

    override fun onSubscriptionSelected(subscription: Subscription, position: Int) {
        binding.subscriptionDoneBtn.isEnabled = true

        val existingList = viewModel.subscriptions.value
        if (!existingList.isNullOrEmpty()) {
            val newList = existingList.toMutableList()

            // change old position
            if (lastPosition != -1) {
                val oldSubscription = newList[lastPosition]
                oldSubscription.isSelected = false
                newList[lastPosition] = oldSubscription

                val lastChild =getChildRadioBtn(lastPosition)
                lastChild?.isChecked = false
            }

            // set new position
            subscription.isSelected = true
            newList[position] = subscription
            lastPosition = position

            val child = getChildRadioBtn(position)
            child?.isChecked = true

            // set the list
            viewModel.setSubscriptions(newList)
        }
    }

    private fun getChildRadioBtn(position: Int) : RadioButton? {
        return binding.subscriptionsRecycler.getChildAt(position)?.findViewById(
            R.id.subscription_select_btn)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Paused subscription fragment")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Resumed subscription fragment")
    }

    companion object {
        private const val TAG = "SubscriptionFragment"
    }

}