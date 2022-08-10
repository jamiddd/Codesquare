package com.jamid.codesquare.adapter.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.jamid.codesquare.R
import com.jamid.codesquare.data.InterestItem
import com.jamid.codesquare.databinding.ActionChipBinding
import com.jamid.codesquare.listeners.InterestItemClickListener

class InterestItemViewHolder(private val view: View, private val interestItemClickListener: InterestItemClickListener): RecyclerView.ViewHolder(view) {

    private lateinit var binding: ActionChipBinding
    init {
        Log.d("Something", "Simple: ")
    }
    fun bind(interestItem: InterestItem?) {
        if (interestItem != null) {
            binding = ActionChipBinding.bind(view)

            val chip = binding.root
            chip.text = interestItem.content
            chip.isCheckable = true
            chip.isChecked = interestItem.isChecked
            chip.isCheckedIconVisible = false
            chip.isCloseIconVisible = false
            chip.tag = interestItem.content

            chip.setOnClickListener {
                interestItemClickListener.onInterestClick(interestItem)
            }

            val smallMargin = view.context.resources.getDimension(R.dimen.generic_len)

            chip.updateLayoutParams<FlexboxLayoutManager.LayoutParams> {
                setMargins(0, 0, smallMargin.toInt(), 0)
            }

        }
    }

    companion object {
        fun newInstance(parent: ViewGroup, interestItemClickListener: InterestItemClickListener)
            = InterestItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.default_chip, parent, false), interestItemClickListener)

    }

}