package com.jamid.codesquare.adapter.recyclerview

import androidx.recyclerview.widget.ListAdapter
import com.jamid.codesquare.adapter.comparators.MediaItemWrapperComparator
import com.jamid.codesquare.data.MediaItemWrapper

abstract class SuperMediaAdapter :
    ListAdapter<MediaItemWrapper, SuperMediaViewHolder>(MediaItemWrapperComparator())
