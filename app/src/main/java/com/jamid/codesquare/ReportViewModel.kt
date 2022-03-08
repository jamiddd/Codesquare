package com.jamid.codesquare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.jamid.codesquare.data.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportViewModel: ViewModel() {

    private val _currentReport = MutableLiveData<Report>().apply { value = Report() }
    val currentReport: LiveData<Report> = _currentReport

    fun addImagesToReport(images: List<String>) {
        val currentReport = currentReport.value
        if (currentReport != null) {
            currentReport.snapshots = images
            _currentReport.postValue(currentReport)
        }
    }

    fun setReportContext(context: String) {
        val currentReport = currentReport.value
        if (currentReport != null) {
            currentReport.contextId = context
            _currentReport.postValue(currentReport)
        }
    }

    fun setReportContent(content: String) {
        val currentReport = currentReport.value
        if (currentReport != null) {
            currentReport.reason = content
            _currentReport.postValue(currentReport)
        }
    }

    fun clearAllImagesFromReport() {
        val currentReport = currentReport.value
        if (currentReport != null) {
            currentReport.snapshots = emptyList()
            _currentReport.postValue(currentReport)
        }
    }

    fun sendReportToFirebase(onComplete: (Task<Void>) -> Unit) = viewModelScope.launch (Dispatchers.IO) {
        val currentReport = currentReport.value
        if (currentReport != null) {
            FireUtility.sendReport(currentReport) {
                onComplete(it)
            }
        }
    }

}