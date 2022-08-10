package com.jamid.codesquare.listeners
// something simple
interface ItemSelectResultListener<T : Any> {
    fun onItemsSelected(items: List<T>, externalSelect: Boolean)
}