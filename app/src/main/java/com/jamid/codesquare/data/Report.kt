package com.jamid.codesquare.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.jamid.codesquare.UserManager
import com.jamid.codesquare.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep// something simple
data class Report(
    var id: String,
    var senderId: String,
    var title: String,
    var image: String,
    var contextId: String,
    var snapshots: List<String>,
    var reason: String,
    var type: ReportType,
    var createdAt: Long
): Parcelable {

    constructor(): this(randomId(), UserManager.currentUserId, "", "", "", emptyList(), "", ReportType.REPORT_PROJECT, System.currentTimeMillis())

    companion object {

        fun getReportForPost(post: Post): Report {
            val report = Report()
            report.title = post.name
            report.image = post.mediaList.first()
            report.contextId = post.id
            report.type = ReportType.REPORT_PROJECT
            return report
        }

        fun getReportForComment(comment: Comment): Report {
            val report = Report()
            report.title = "Comment by ${comment.sender.name}"
            report.image = comment.sender.photo
            report.contextId = comment.commentId
            report.type = ReportType.REPORT_COMMENT
            return report
        }

        fun getReportForUser(user: User): Report {
            val report = Report()
            report.title = user.name
            report.image = user.photo
            report.contextId = user.id
            report.type = ReportType.REPORT_USER
            return report
        }

    }

}

enum class ReportType {
    REPORT_USER, REPORT_COMMENT, REPORT_PROJECT
}