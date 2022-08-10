package com.jamid.codesquare.data.dao

import android.util.Log
import androidx.room.Dao
import com.jamid.codesquare.data.Interest

@Dao
abstract class InterestDao: BaseDao<Interest>() {
    init {
        Log.d("Something", "Simple: ")
    }
}