package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R

class GuidelinesAdapter(val onDeleteClick: (v: View, position: Int) -> Unit): ListAdapter<String, GuidelinesAdapter.GuidelineViewHolder>(comparator) {

    companion object {
        private val comparator = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem.length == newItem.length
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }


    inner class GuidelineViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val indexText: TextView = view.findViewById(R.id.rule_index)
        private val ruleContent: TextView = view.findViewById(R.id.rule_content)
        private val ruleDeleteBtn: Button = view.findViewById(R.id.delete_rule_btn)

        fun bind(rule: String) {
            indexText.text = (layoutPosition + 1).toString()
            ruleContent.text = rule

            ruleDeleteBtn.setOnClickListener {
                onDeleteClick(it, layoutPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuidelineViewHolder {
        return GuidelineViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.guideline_item, parent, false))
    }

    override fun onBindViewHolder(holder: GuidelineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}