package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.SubscriptionComparator
import com.jamid.codesquare.data.Subscription
import com.jamid.codesquare.databinding.SubscriptionItemBinding
import com.jamid.codesquare.listeners.SubscriptionListener

class SubscriptionAdapter(private val subscriptionListener: SubscriptionListener): ListAdapter<Subscription, SubscriptionAdapter.SubscriptionViewHolder>(SubscriptionComparator()) {

    inner class SubscriptionViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private lateinit var binding: SubscriptionItemBinding

        fun bind(subscription: Subscription) {
            binding = SubscriptionItemBinding.bind(view)

            binding.durationHeaderText.text = if (subscription.period == "P1M") {
                "MONTHLY"
            } else {
                "YEARLY"
            }
            binding.featuresText.text = subscription.description

            val price = subscription.priceText + if (subscription.period == "P1M") {
                "/month"
            } else {
                "/year"
            }

            binding.priceText.text = price
            binding.subscriptionSelectBtn.isFocusable = false
            binding.subscriptionSelectBtn.isClickable = false

            Log.d("SubscriptionAdapter", subscription.isSelected.toString())

            binding.subscriptionSelectBtn.isChecked = subscription.isSelected

            binding.root.setOnClickListener {
                subscriptionListener.onSubscriptionSelected(subscription, absoluteAdapterPosition)
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