package com.jamid.codesquare

import android.widget.ProgressBar
import androidx.lifecycle.*
import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.multipleindex.IndexQuery
import com.algolia.search.model.multipleindex.IndexedQuery
import com.algolia.search.model.response.ResponseMultiSearch
import com.algolia.search.model.response.ResponseSearch
import com.algolia.search.model.response.ResultMultiSearch
import com.algolia.search.model.search.Query
import com.jamid.codesquare.data.*
import com.jamid.codesquare.db.CodesquareDatabase
import com.jamid.codesquare.db.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel: ViewModel() {

    private val _recentSearchList = MutableLiveData<List<SearchQuery>>().apply { value = null }
    val recentSearchList: LiveData<List<SearchQuery>> = _recentSearchList

    private val client = ClientSearch(ApplicationID(BuildConfig.ALGOLIA_ID), APIKey(BuildConfig.ALGOLIA_SECRET))

    var recentUserSearchCache: Map<String, User> = emptyMap()
    var recentProjectSearchCache: Map<String, Project> = emptyMap()

    private val _networkError = MutableLiveData<Exception>().apply { value = null }
    val networkError: LiveData<Exception> = _networkError

    private fun setSearchData(searchList: List<SearchQuery>?) {
        _recentSearchList.postValue(searchList)
    }

    private fun setNetworkError(exception: Exception?) {
        _networkError.postValue(exception)
    }

    private fun addUserToCache(vararg users: User) {
        val newMap = recentUserSearchCache.toMutableMap()
        for (user in users) {
            newMap[user.id] = user
        }
        recentUserSearchCache = newMap
    }

    private fun addProjectToCache(vararg projects: Project) {
        val newMap = recentProjectSearchCache.toMutableMap()
        for (project in projects) {
            newMap[project.id] = project
        }
        recentProjectSearchCache = newMap
    }

    @Suppress("UNCHECKED_CAST")
    fun search(query: String) = viewModelScope.launch (Dispatchers.IO) {
        val newQueries = mutableListOf<IndexedQuery>()
        val iq = IndexQuery(
            IndexName("projects"), Query(query)
        )

        val iq1 = IndexQuery(IndexName("users"), Query(query))

        newQueries.add(iq)
        newQueries.add(iq1)

        try {
            val response: ResponseMultiSearch = client.search(newQueries)

            val list = response.results as List<ResultMultiSearch<ResponseSearch>>

            val searchList = mutableListOf<SearchQuery>()
            for (result in list) {
                for (hit in result.response.hits) {
                    val type = hit.json["type"].toString()
                    if (type == "\"user\"") {
                        val user = hit.deserialize(User.serializer())
                        addUserToCache(user)
                        val searchQuery = SearchQuery(user.id, user.name, System.currentTimeMillis(), QUERY_TYPE_USER)
                        searchList.add(searchQuery)
                    } else {
                        val project = hit.deserialize(Project.serializer())
                        addProjectToCache(project)
                        val searchQuery = SearchQuery(project.id, project.name, System.currentTimeMillis(), QUERY_TYPE_PROJECT)
                        searchList.add(searchQuery)
                    }
                }
            }

            setSearchData(searchList)
        } catch (e: Exception) {
            setNetworkError(e)
        }
    }

}

/*
@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SearchViewModel() as T
    }
}*/
