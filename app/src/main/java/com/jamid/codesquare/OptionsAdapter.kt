package com.jamid.codesquare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.data.Option
import com.jamid.codesquare.databinding.ListItemBinding
import com.jamid.codesquare.listeners.OptionClickListener

class OptionsAdapter(private val optionClickListener: OptionClickListener): ListAdapter<Option, OptionsAdapter.OptionsViewHolder>(comparator) {

    companion object {
        val comparator = object : DiffUtil.ItemCallback<Option>() {
            override fun areItemsTheSame(oldItem: Option, newItem: Option): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Option, newItem: Option): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class OptionsViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        fun bind(option: Option) {
            val binding = ListItemBinding.bind(view)

            binding.listOptionItem.text = option.item

            if (option.icon != null) {
                binding.listOptionItem.setCompoundDrawablesRelativeWithIntrinsicBounds(option.icon, 0, 0, 0)
            } else {
                binding.listOptionItem.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
            }

            binding.listOptionItem.setOnClickListener {
                optionClickListener.onOptionClick(option)
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionsViewHolder {
        return OptionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
    }

    override fun onBindViewHolder(holder: OptionsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}