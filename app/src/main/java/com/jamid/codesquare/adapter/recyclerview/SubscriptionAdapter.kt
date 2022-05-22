package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.SubscriptionComparator
import com.jamid.codesquare.data.OneTimeProduct
import com.jamid.codesquare.databinding.SubscriptionItemBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.SubscriptionListener
import com.jamid.codesquare.show

class SubscriptionAdapter(private val subscriptionListener: SubscriptionListener): ListAdapter<OneTimeProduct, SubscriptionAdapter.SubscriptionViewHolder>(SubscriptionComparator()) {

    inner class SubscriptionViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: SubscriptionItemBinding

        fun bind(oneTimeProduct: OneTimeProduct) {
            binding = SubscriptionItemBinding.bind(view)



          /*  binding.durationHeaderText.text = if (oneTimeProduct.period == "P1M") {
                "MONTHLY"
            } else {
                "YEARLY"
            }*/

            binding.featuresText.text = oneTimeProduct.description

            /*val price = oneTimeProduct.priceText + if (oneTimeProduct.period == "P1M") {
                "/month"
            } else {
                "/year"
            }*/

            if (oneTimeProduct.priceText.contains("150.00")) {
                binding.bestValueAnimation.show()
            } else {
                binding.bestValueAnimation.hide()
            }

            binding.priceText.text = oneTimeProduct.priceText
            binding.subscriptionSelectBtn.isFocusable = false
            binding.subscriptionSelectBtn.isClickable = false

            Log.d("SubscriptionAdapter", oneTimeProduct.isSelected.toString())

            binding.subscriptionSelectBtn.isChecked = oneTimeProduct.isSelected

            binding.root.setOnClickListener {
                subscriptionListener.onSubscriptionSelected(oneTimeProduct, absoluteAdapterPosition)
                check()
            }
        }

        fun check() {
            binding.subscriptionSelectBtn.isChecked = true
        }

        fun uncheck() {
            binding.subscriptionSelectBtn.isChecked = false
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        return SubscriptionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.subscription_item, parent, false))
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun getPrefixBasedOnCurrency(currency: String): String {
        return when (currency) {
            "INR" -> "₹"
            "USD" -> "$"
            else -> "$"
        } + " "
    }

}