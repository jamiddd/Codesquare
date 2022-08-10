package com.jamid.codesquare.listeners

interface ItemSelectResultListener<T : Any> {
    fun onItemsSelected(items: List<T>, externalSelect: Boolean)
}