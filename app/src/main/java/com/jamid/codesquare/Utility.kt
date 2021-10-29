package com.jamid.codesquare

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.data.UserMinimal
import com.jamid.codesquare.ui.auth.CreateAccountFragment
import java.lang.IllegalStateException
import java.util.*
import java.util.regex.Pattern

fun getWindowHeight() = Resources.getSystem().displayMetrics.heightPixels

fun Activity.getFullScreenHeight(): Int {
    return if (Build.VERSION.SDK_INT > 29) {
        val rect = windowManager.maximumWindowMetrics.bounds
        rect.bottom - rect.top
    } else {
        getWindowHeight()
    }
}

fun Fragment.getFullScreenHeight(): Int {
    return requireActivity().getFullScreenHeight()
}


fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.disappear() {
    this.visibility = View.INVISIBLE
}

fun Fragment.toast(msg: String) {
    requireContext().toast(msg)
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun slideRightNavOptions(): NavOptions {
    return navOptions {
        anim {
            enter = R.anim.slide_in_right
            exit = R.anim.slide_out_left
            popEnter = R.anim.slide_in_left
            popExit = R.anim.slide_out_right
        }
    }
}

fun Fragment.showError(exception: Exception) {
    requireContext().showError(exception)
}

fun Context.showError(exception: Exception) {
    exception.localizedMessage?.let {
        toast(it)
        Log.e(TAG, it)
    }
}

fun randomId(): String {
    return UUID.randomUUID().toString().replace("-", "")
}

fun CharSequence?.isValidEmail() =
    !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun CharSequence?.isValidPassword() =
    !isNullOrEmpty() && Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{4,}\$")
        .matcher(this).matches()

fun showSnack(rootLayout: ViewGroup, msg: String, anchor: View? = null, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val snackBar = if (anchor != null) {
        Snackbar.make(rootLayout, msg, Snackbar.LENGTH_LONG)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .setAnchorView(anchor)
    } else {
        Snackbar.make(rootLayout, msg, Snackbar.LENGTH_LONG)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
    }

    if (actionText != null) {
        snackBar.setAction(actionText) {
            onAction!!()
        }
    }

    snackBar.show()
}

fun View.updateLayout(
    height: Int? = null,
    width: Int? = null,
    margin: Int? = null,
    marginLeft: Int? = null,
    marginTop: Int? = null,
    marginRight: Int? = null,
    marginBottom: Int? = null,
    padding: Int? = null,
    paddingLeft: Int? = null,
    paddingTop: Int? = null,
    paddingRight: Int? = null,
    paddingBottom: Int? = null,
    ignoreParams: Boolean? = true,
    ignoreMargin: Boolean? = true,
    ignorePadding: Boolean? = true,
    extras: Map<String, Int>? = null) {

    var ilp = ignoreParams
    var im = ignoreMargin
    var ip = ignorePadding

    if (width != null || height != null) {
        ilp = false
    }

    if (margin != null || marginLeft != null || marginTop != null || marginRight != null || marginBottom != null) {
        im = false
    }

    if (padding != null || paddingLeft != null || paddingTop != null || paddingRight != null || paddingBottom != null) {
        ip = false
    }

    if (ilp != null && !ilp) {
        val params = if (extras != null) {
            val p1 = this.layoutParams as ConstraintLayout.LayoutParams
            p1.height = height ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            p1.width = width ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            val defaultId = (this.parent as ConstraintLayout).id
            p1.apply {
                startToStart = extras[START_TO_START] ?: defaultId
                endToEnd = extras[END_TO_END] ?: defaultId
                topToTop = extras[TOP_TO_TOP] ?: defaultId
                bottomToBottom = extras[BOTTOM_TO_BOTTOM] ?: defaultId
                if (extras.containsKey(START_TO_END)) {
                    startToEnd = extras[START_TO_END]!!
                }
                if (extras.containsKey(END_TO_START)) {
                    endToStart = extras[END_TO_START]!!
                }
                if (extras.containsKey(TOP_TO_BOTTOM)) {
                    topToBottom = extras[TOP_TO_BOTTOM]!!
                }
                if (extras.containsKey(BOTTOM_TO_TOP)) {
                    bottomToTop = extras[BOTTOM_TO_TOP]!!
                }
            }
            p1
        } else {
            val p1 = this.layoutParams as ViewGroup.LayoutParams
            p1.height = height ?: ViewGroup.LayoutParams.WRAP_CONTENT
            p1.width = width ?: ViewGroup.LayoutParams.MATCH_PARENT
            p1
        }

        this.layoutParams = params
    }

    if (im != null && !im) {
        val marginParams = this.layoutParams as ViewGroup.MarginLayoutParams
        if (margin != null) {
            marginParams.setMargins(margin)
        } else {
            marginParams.setMargins(marginLeft ?: 0, marginTop ?: 0, marginRight ?: 0, marginBottom ?: 0)
        }
        this.requestLayout()
    }

    if (ip != null && !ip) {
        if (padding != null) {
            this.setPadding(padding)
        } else {
            this.setPadding(paddingLeft ?: 0, paddingTop ?: 0, paddingRight ?: 0, paddingBottom ?: 0)
        }
    }
}

fun Context.convertDpToPx(dp: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    resources.displayMetrics
).toInt()

fun convertDpToPx(dp: Int, context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    context.resources.displayMetrics
).toInt()

fun Fragment.convertDpToPx(dp: Int) = requireContext().convertDpToPx(dp)

fun getTextForTime(time: Long): String {
    return DateUtils.getRelativeTimeSpanString(time, Calendar.getInstance().timeInMillis, DateUtils.MINUTE_IN_MILLIS).toString()
}

fun View.slideReset() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun View.slideUp(offset: Float) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun View.slideDown(offset: Float) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offset)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun Fragment.showKeyboard() {
    view?.let { activity?.showKeyboard() }
}

private fun Context.showKeyboard() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (Build.VERSION.SDK_INT > 30) {
        inputMethodManager.showSoftInput((this as Activity).window.decorView, 0)
    } else {
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

/**
 * Extension function to hide the keyboard from the given context
 *
 * @param view
 */
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}


fun RecyclerView.addOnIdleListener(onIdle: (layoutManager: LinearLayoutManager) -> Unit) {
    val linearLayoutManager = this.layoutManager
    if (linearLayoutManager is LinearLayoutManager && linearLayoutManager.orientation == LinearLayoutManager.VERTICAL) {
        this.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onIdle(linearLayoutManager)
                }
            }
        })
    } else {
        throw IllegalStateException("Recyclerview must be vertical and linear.")
    }
}


fun getExtensionForMime(mime: String): String {
    return when (mime) {
        "audio/aac" -> ".aac"
        "video/x-msvideo" -> ".avi"
        "image/bmp" -> ".bmp"
        "text/css" -> ".css"
        "text/csv" -> ".csv"
        "application/msword" -> ".doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
        "application/gzip" -> ".gz"
        "image/gif" -> ".gif"
        "text/html" -> ".html"
        "image/vnd.microsoft.icon" -> ".ico"
        "image/jpeg" -> ".jpg"
        "text/javascript" -> ".js"
        "application/json" -> ".json"
        "audio/mpeg" -> ".mp3"
        "video/mp4" -> ".mp4"
        "video/mpeg" -> ".mpeg"
        "application/vnd.oasis.opendocument.presentation" -> ".odp"
        "application/vnd.oasis.opendocument.spreadsheet" -> ".ods"
        "application/vnd.oasis.opendocument.text" -> ".odt"
        "audio/ogg" -> ".oga"
        "video/ogg" -> ".ogv"
        "font/otf" -> ".otf"
        "image/png" -> ".png"
        "application/pdf" -> ".pdf"
        "application/x-httpd-php" -> ".php"
        "application/vnd.ms-powerpoint" -> ".ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx"
        "application/vnd.rar" -> ".rar"
        "application/rtf" -> ".rtf"
        "application/x-sh" -> ".sh"
        "image/svg+xml" -> ".svg"
        "application/x-tar" -> ".tar"
        "font/ttf" -> ".ttf"
        "text/plain" -> ".txt"
        "audio/wav" -> ".wav"
        "audio/webm" -> ".weba"
        "video/webm" -> ".webm"
        "image/webp" -> ".webp"
        "font/woff" -> ".woff"
        "font/woff2" -> ".woff2"
        "application/xhtml+xml" -> ".xhtml"
        "application/vnd.ms-excel" -> ".xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx"
        "application/xml" -> ".xml"
        "text/xml" -> ".xml"
        "application/zip" -> ".zip"
        "application/x-7z-compressed" -> ".7z"
        else -> ""
    }
}

fun getTextForSizeInBytes(size: Long): String {
    return when {
        size > (1024 * 1024) -> {
            val sizeInMB = size.toFloat()/(1024 * 1024)
            sizeInMB.toString().take(4) + " MB"
        }
        size/1024 > 100 -> {
            val sizeInMB = size.toFloat()/(1024 * 1024)
            sizeInMB.toString().take(4) + " MB"
        }
        else -> {
            val sizeInKB = size.toFloat()/1024
            sizeInKB.toString().take(4) + " KB"
        }
    }
}



private const val TAG = "UtilityTAG"