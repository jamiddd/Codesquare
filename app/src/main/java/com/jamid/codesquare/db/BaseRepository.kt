package com.jamid.codesquare.db

import androidx.lifecycle.MutableLiveData

abstract class BaseRepository(db: CodesquareDatabase) {

    open val database = db

    val networkError = MutableLiveData<Exception>().apply { value = null }
    val databaseError = MutableLiveData<Exception>().apply { value = null }




}