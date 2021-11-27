package com.jamid.codesquare

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
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
import com.facebook.imagepipeline.image.CloseableImage

import com.facebook.common.references.CloseableReference

import com.facebook.imagepipeline.request.ImageRequest

import com.facebook.imagepipeline.common.ResizeOptions

import com.facebook.imagepipeline.request.ImageRequestBuilder

import com.facebook.drawee.backends.pipeline.Fresco

import com.facebook.imagepipeline.core.ImagePipeline

import com.facebook.datasource.DataSubscriber

import android.net.Uri
import androidx.core.animation.doOnEnd
import androidx.core.text.isDigitsOnly

import com.facebook.imagepipeline.image.CloseableBitmap

import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import android.content.res.TypedArray
import androidx.core.view.iterator
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView


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

fun View.showWithAnimations() {

    this.scaleX = 0f
    this.scaleY = 0f

    this.show()

    val objAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f, 1f)
    val objAnimator1 = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f, 1f)

    AnimatorSet().apply {
        duration = 250
        interpolator = AccelerateDecelerateInterpolator()
        playTogether(objAnimator, objAnimator1)
        start()
    }
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.hideWithAnimation() {
    val objAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f)
    val objAnimator1 = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f )

    AnimatorSet().apply {
        duration = 250
        interpolator = AccelerateDecelerateInterpolator()
        playTogether(objAnimator, objAnimator1)
        start()
    }.doOnEnd {
        this.visibility = View.GONE
    }
}

fun View.disappear() {
    this.visibility = View.INVISIBLE
}

fun Fragment.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().toast(msg, duration)
}

fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg, duration).show()
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

fun <T: Any> List<T>.addItemToList(item: T): List<T> {
    val newList = this.toMutableList()
    newList.add(item)
    return newList
}

fun <T: Any> List<T>.removeItemFromList(item: T): List<T> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        val newList = this.toMutableList()
        newList.remove(item)
        newList
    }
}

fun <T: Any> List<T>.removeItemFromList(pos: Int): List<T> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        val newList = this.toMutableList()
        newList.removeAt(pos)
        newList
    }
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

fun Fragment.isNightMode(): Boolean {
    return requireContext().isNightMode()
}

fun Context.isNightMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
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

// must be in the form [a, b, c]
fun String.toList() : List<Any> {

    if (this.first() != '[') {
        return emptyList()
    }

    if (this.first() != this.last()) {
        return emptyList()
    }

    if (this.length == 2) {
        return emptyList()
    }

    val newList = mutableListOf<Any>()
    val newString = this.substring(1, this.length - 1)
    val words = newString.split(',')

    val sample = words.first()
    when {
        sample.startsWith('{') -> {
            // all are objects
            for (word in words) {
                newList.add(word.toHashMap())
            }
        }
        sample.startsWith('\"') -> {
            // all are strings
            for (word in words) {
                newList.add(word.substring(1, word.length - 1))
            }
        }
        sample.isDigitsOnly() -> {
            // all are numbers
            for (word in words) {
                newList.add(word.toInt())
            }
        }
    }

    return newList
}

// must be in the form { }
fun String.toHashMap(): Map<String, Any> {

    if (this.first() != '{') {
        return emptyMap()
    }

    if (this.first() != this.last()) {
        return emptyMap()
    }

    val map = mutableMapOf<String, Any>()

    fun z(pair: String) {
        val keyValue = pair.split(':')
        val key = keyValue.first()
        val value = keyValue.last()

        // object
        if (value.startsWith('{')) {
            val newValue = value.toHashMap()
            map[key] = newValue
        }

        // list
        if (value.startsWith('[')) {
            val newValue = value.toList()
            map[key] = newValue
        }

        // number
        if (value.isNotEmpty() && value.isDigitsOnly()) {
            val newValue = value.toLong()
            map[key] = newValue
        }

        // string
        if (value.first() == '\"' && value.last() == '\"') {
            map[key] = value
        }

    }

    if (this.contains(',')) {
        // multiple elements
        val fieldsAndValuesList = this.split(',')
        for (pair in fieldsAndValuesList) {
            z(pair)
        }
    } else {
        // single element
        z(this)
    }

    return map
}

fun CollectionReference.addOrder(field: String, direction: Query.Direction): Query {
    return this.orderBy(field, direction)
}

fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap? {
    val widthLight = bitmap.width
    val heightLight = bitmap.height

    val padding = if (widthLight > heightLight) {
        val diff = widthLight - heightLight
        diff/2 to 0
    } else {
        val diff = heightLight - widthLight
        0 to diff/2
    }

    val output = Bitmap.createBitmap(
        bitmap.width, bitmap.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)
    val paintColor = Paint()
    paintColor.flags = Paint.ANTI_ALIAS_FLAG
    val rectF = RectF(Rect(padding.first, padding.second, widthLight - padding.first, heightLight - padding.second))
    Log.d(TAG, rectF.toShortString())
    canvas.drawRoundRect(rectF, (widthLight / 2).toFloat(), (heightLight / 2).toFloat(), paintColor)
    val paintImage = Paint()
    paintImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paintImage)
    return output
}

fun Context.accentColor(): Int {
    val typedValue = TypedValue()
    val a = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
    val color = a.getColor(0, 0)
    a.recycle()
    return color
}

fun getCircleBitmap(bitmap: Bitmap): Bitmap {

    Log.d(TAG, "${bitmap.width} x ${bitmap.height}")

    val output = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)

    val color = -0xbdbdbe
    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.height)

    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color

    canvas.drawCircle(
        bitmap.width / 2f, bitmap.height / 2f,
        (minOf(bitmap.width, bitmap.height)) / 2f, paint
    )
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)

    Log.d(TAG, "${output.width} x ${output.height}")

    return output
}

fun BottomNavigationView.attachToViewPager(viewPager: ViewPager2) {

    val menuItems = this.menu.iterator().withIndex()

    this.setOnItemSelectedListener {
        for (item in menuItems) {
            if (item.value.itemId == it.itemId) {
                viewPager.setCurrentItem(item.index, false)
            }
        }
        true
    }

    viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            for (item in menuItems) {
                if (item.index == position) {
                    this@attachToViewPager.selectedItemId = item.value.itemId
                }
            }
        }
    })
}

fun <T: Any> MutableList<T>.removeAtIf(predicate: (T) -> Boolean) {
    var removePos = -1
    for (i in this.indices) {
        if (predicate(this[i])) {
            removePos = i
        }
    }
    this.removeAt(removePos)
}

const val TAG = "UtilityTAG"