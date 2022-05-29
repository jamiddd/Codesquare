package com.jamid.codesquare.ui.create

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.paging.ExperimentalPagingApi
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.MainViewModel
import com.jamid.codesquare.databinding.FragmentCreatePostBinding

@ExperimentalPagingApi
class CreatePostFragment2: BaseFragment<FragmentCreatePostBinding, MainViewModel>() {
    override val viewModel: MainViewModel by activityViewModels()

    override fun getViewBinding(): FragmentCreatePostBinding {
        return FragmentCreatePostBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // a reactive form to save all the data in viewModel reactively
        // progress on create
        // ui on success and failure

    }

}