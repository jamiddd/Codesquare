package com.jamid.codesquare.adapter.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.codesquare.R
import com.jamid.codesquare.adapter.comparators.RankedRulesComparator
import com.jamid.codesquare.data.RankedRule
import com.jamid.codesquare.databinding.RuleComponentBinding
import com.jamid.codesquare.hide
import com.jamid.codesquare.listeners.RankedRuleClickListener
import com.jamid.codesquare.show

class RankedRulesAdapter(val rankedRuleClickListener: RankedRuleClickListener? = null) : ListAdapter<RankedRule, RankedRulesAdapter.RankedRulesViewHolder>(RankedRulesComparator()) {

    inner class RankedRulesViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(rankedRule: RankedRule) {

            val binding = RuleComponentBinding.bind(view)

            binding.ruleName.text = rankedRule.name

            val indexList = mutableListOf<Int>()

            var i = 1
            var numIndex = rankedRule.content.indexOf("$i. ")
            while (numIndex != -1) {
                indexList.add(numIndex)
                i++
                numIndex = rankedRule.content.indexOf("$i. ")
            }

            val rules = mutableListOf<String>()

            for (x in indexList.indices) {
                val nextPos = x + 1
                val start = indexList[x]

                val end = if (nextPos < indexList.size) {
                    indexList[nextPos]
                } else {
                    rankedRule.content.length - 1
                }

                rules.add(rankedRule.content.substring(start until end))
            }

            binding.ruleContent.removeAllViews()

            for (rule in rules) {
                val textView = TextView(view.context)
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Body1)
                textView.text = rule
                binding.ruleContent.addView(textView)

                textView.updateLayoutParams<LinearLayout.LayoutParams> {
                    bottomMargin = view.context.resources.getDimension(R.dimen.unit_len).toInt()
                }
            }

            if (rankedRule.isOpened) {
                binding.ruleContent.show()
                binding.toggleRuleBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_keyboard_arrow_up_24)
            } else {
                binding.ruleContent.hide()
                binding.toggleRuleBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_keyboard_arrow_down_24)
            }

            binding.toggleRuleBtn.setOnClickListener {
                rankedRuleClickListener?.onToggleButtonClick(rankedRule, bindingAdapterPosition)
            }

            binding.root.setOnClickListener {
                rankedRuleClickListener?.onToggleButtonClick(rankedRule, bindingAdapterPosition)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankedRulesViewHolder {
        return RankedRulesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rule_component, parent, false))
    }

    override fun onBindViewHolder(holder: RankedRulesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}