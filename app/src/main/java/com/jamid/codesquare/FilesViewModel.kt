package com.jamid.codesquare

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jamid.codesquare.data.MediaItem
import com.jamid.codesquare.data.MediaItemWrapper
import com.jamid.codesquare.ui.ItemSelectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// something simple
class FilesViewModel: ViewModel() {

    private val _selectMediaItems = MutableLiveData<List<MediaItemWrapper>>().apply { value = emptyList() }
    val selectMediaItems: LiveData<List<MediaItemWrapper>> = _selectMediaItems

    private fun setSelectMediaItems(mediaItems: List<MediaItemWrapper>) {
        _selectMediaItems.postValue(mediaItems)
    }

    fun addMediaItemsToList(items: List<MediaItemWrapper>)  = viewModelScope.launch (Dispatchers.IO) {

        Log.d(TAG, "addMediaItemsToList: adding items to list")

        val existingList = selectMediaItems.value
        if (existingList != null) {

            Log.d(TAG, "addMediaItemsToList: Really adding")

            val newList = existingList.toMutableList()
            newList.addAll(items)
            setSelectMediaItems(newList)
        } else {

            Log.d(TAG, "addMediaItemsToList: setting list")

            setSelectMediaItems(items)
        }
    }

    fun loadItemsFromExternal(contentResolver: ContentResolver, type: ItemSelectType, limit: Int = 50, lastItemSortAnchor: Long = System.currentTimeMillis()): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Files.getContentUri("external")
            }

        var selection = when (type) {
            ItemSelectType.GALLERY_ONLY_IMG -> {
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            }
            ItemSelectType.GALLERY_ONLY_VID -> {
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND " + MediaStore.Files.FileColumns.SIZE + "< 15728640"
            }
            ItemSelectType.GALLERY -> {
                "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND " + MediaStore.Files.FileColumns.SIZE + "< 15728640"     //Selection criteria
            }
            ItemSelectType.DOCUMENT -> {
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE
            }
        }

        selection = if (selection.isBlank()) {
            MediaStore.Files.FileColumns.DATE_MODIFIED + "<" + lastItemSortAnchor
        } else {
            selection + " AND " + MediaStore.Files.FileColumns.DATE_MODIFIED + "<" + lastItemSortAnchor
        }

        val selectionArgs = arrayOf<String>()

        val sortOrder: String = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE
        )

        val cursor = contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)

        if (cursor != null) {
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            var count = 0

            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol)

                val (t, fileUri) = when (type) {
                    ItemSelectType.GALLERY_ONLY_IMG -> {
                        image to ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
                    ItemSelectType.GALLERY_ONLY_VID -> {
                        video to ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
                    ItemSelectType.GALLERY -> {
                        if (mimeType.contains(video)) {
                            video to ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                        } else {
                            image to ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                        }
                    }
                    ItemSelectType.DOCUMENT -> {
                        document to ContentUris.withAppendedId(
                            MediaStore.Files.getContentUri("external"),
                            id
                        )
                    }
                }

                Log.d(TAG, "loadItemsFromExternal: ${cursor.getString(nameCol)} -- ${cursor.getLong(modifiedCol)} -- ${cursor.getLong(sizeCol)}")

                val name = cursor.getString(nameCol) ?: (randomId() + ".pdf")
                val dateModified = cursor.getLong(modifiedCol)
                val size = cursor.getLong(sizeCol)
                val createdAt = cursor.getLong(dateAddedCol)

                val ext = if (name.split('.').size > 1) {
                    "." +  name.substringAfterLast('.', "")
                } else {
                    if (mimeType.contains("video")) {
                        ".mp4"
                    } else {
                        ".jpg"
                    }
                }

                val mediaItem = MediaItem(
                    fileUri.toString(),
                    name, t, mimeType ?: "", size, ext, "", null, createdAt, dateModified
                )
                mediaItems.add(mediaItem)
                count++
            }
            cursor.close()
        }

        return mediaItems
    }

    fun updateSelectStatePos(pos: Int, b: Boolean) {
        val existingList = selectMediaItems.value
        if (existingList != null) {
            val newList = existingList.toMutableList()
            newList[pos].isSelected = b
            setSelectMediaItems(newList)
        }
    }

    fun updateSelectCountAtPos(pos: Int, i: Int) {
        val existingList = selectMediaItems.value
        if (existingList != null) {
            val newList = existingList.toMutableList()
            newList[pos].selectedCount = i
            setSelectMediaItems(newList)
        }
    }

}