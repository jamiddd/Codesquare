package com.jamid.codesquare

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.paging.ExperimentalPagingApi
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import androidx.viewbinding.ViewBindings
import com.jamid.codesquare.data.AdLimit
import com.jamid.codesquare.data.AdLimit.*
import com.jamid.codesquare.data.User
import com.jamid.codesquare.databinding.FragmentPagerBinding
import com.jamid.codesquare.ui.MainActivity
import com.jamid.codesquare.ui.MessageDialogFragment

@ExperimentalPagingApi
abstract class BaseFragment<T: ViewBinding, U: ViewModel> : Fragment() {

    protected lateinit var binding: T
    protected abstract val viewModel: U
    protected lateinit var activity: MainActivity

    abstract fun getViewBinding(): T

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getViewBinding()
        return binding.root
    }

    fun getUserImpulsive(userId: String, onGet: (User) -> Unit) {
        activity.getUserImpulsive(userId, onGet)
    }

    fun SwipeRefreshLayout.setDefaultSwipeRefreshLayoutUi() {
        setColorSchemeColors(requireContext().accentColor())

        if (isNightMode()) {
            setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.darkest_grey_2))
        } else {
            setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        val zero = resources.getDimension(R.dimen.zero).toInt()
        val actionBarOffset = resources.getDimension(R.dimen.action_bar_height).toInt()

        setProgressViewOffset(false, zero, actionBarOffset)
    }

}
