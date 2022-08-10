package com.jamid.codesquare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.android.billingclient.api.BillingFlowParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.data.OneTimeProduct
import com.jamid.codesquare.databinding.SubscriptionLayout2Binding
import com.jamid.codesquare.disappear
import com.jamid.codesquare.hide
import com.jamid.codesquare.show
// something simple
class SubscriptionFragment : RoundedBottomSheetDialogFragment() {

    private lateinit var binding: SubscriptionLayout2Binding
    private val viewModel: MainViewModel by activityViewModels()
    private var lastPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SubscriptionLayout2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        subscriptionAdapter = SubscriptionAdapter(this)

        val dialog = dialog!!
        val frame = dialog.window!!.decorView.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(frame)
//        behavior.isDraggable = false

        behavior.state = BottomSheetBehavior.STATE_EXPANDED

       /* binding.subscriptionsRecycler.apply {
            adapter = subscriptionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }*/

        viewModel.productDetails.observe(viewLifecycleOwner) { list ->
            if (!list.isNullOrEmpty()) {
                val products = list.map {
                    OneTimeProduct(it.productId, it.oneTimePurchaseOfferDetails!!.priceAmountMicros, it.oneTimePurchaseOfferDetails!!.formattedPrice, it.oneTimePurchaseOfferDetails!!.priceCurrencyCode, false, it.description, System.currentTimeMillis(), false)
                }

                viewModel.setProducts(products)
            }
        }


        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        binding.upgradBtn.setOnClickListener {
            val billingClient = (activity as MainActivity?)?.playBillingController?.billingClient
            if (billingClient != null) {

                binding.upgradeProgress.show()
                binding.upgradBtn.disappear()

                val list = viewModel.productDetails.value
                if (!list.isNullOrEmpty()) {
                    val detail = list[lastPosition]

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
                }
            }
        }


        (activity as MainActivity).playBillingController.errors.observe(this) {
            if (it != null) {
                binding.upgradeProgress.hide()
                binding.upgradBtn.show()
            }
        }

    }

   /* override fun onSubscriptionSelected(oneTimeProduct: OneTimeProduct, position: Int) {
        binding.subscriptionDoneBtn.isEnabled = true

        val existingList = viewModel.products.value
        if (!existingList.isNullOrEmpty()) {
            val newList = existingList.toMutableList()

            // change old position
            if (lastPosition != -1) {
                val oldProduct = newList[lastPosition]
                oldProduct.isSelected = false
                newList[lastPosition] = oldProduct

                val lastChild = getChildRadioBtn(lastPosition)
                lastChild?.isChecked = false
            }

            // set new position
            oneTimeProduct.isSelected = true
            newList[position] = oneTimeProduct
            lastPosition = position

            val child = getChildRadioBtn(position)
            child?.isChecked = true

            // set the list
            viewModel.setProducts(newList)
        }
    }

    private fun getChildRadioBtn(position: Int) : RadioButton? {
        return binding.subscriptionsRecycler.getChildAt(position)?.findViewById(
            R.id.subscription_select_btn)
    }*/

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