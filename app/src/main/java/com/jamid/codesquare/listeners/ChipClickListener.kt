package com.jamid.codesquare.listeners

import com.google.android.material.chip.Chip
// something simple
interface ChipClickListener {
    fun onCloseIconClick(chip: Chip) {}
    fun onClick(chip: Chip) {}
    fun onLongClick(chip: Chip) {}
}