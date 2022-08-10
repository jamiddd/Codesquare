package com.jamid.codesquare.listeners

import com.jamid.codesquare.data.RankedRule
// something simple
interface RankedRuleClickListener {
    fun onToggleButtonClick(rankedRule: RankedRule, position: Int)
}