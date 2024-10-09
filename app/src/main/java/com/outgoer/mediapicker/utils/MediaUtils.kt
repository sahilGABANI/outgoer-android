package com.outgoer.mediapicker.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import com.outgoer.R
import com.outgoer.mediapicker.constants.BaseConstants
import com.outgoer.mediapicker.constants.FileConstants
import com.outgoer.mediapicker.models.AlbumPhotoModel
import com.outgoer.mediapicker.models.PhotoModel
import timber.log.Timber
import java.io.File

object MediaUtils {

    private var albumWithMediaArrayList = ArrayList<AlbumPhotoModel>()

    @SuppressLint("Range")
    fun loadAlbumsWithPhotoList(context: Context, onAlbumLoad: (albumPhotoModelArrayList: ArrayList<AlbumPhotoModel>) -> Unit = {}) {
        //doAsync {
        albumWithMediaArrayList = ArrayList()

        val contentResolver: ContentResolver = context.contentResolver

        val projectionList: MutableList<String> = ArrayList()
        projectionList.add(MediaStore.Files.FileColumns._ID)
        projectionList.add(MediaStore.MediaColumns.DATA)
        projectionList.add(MediaStore.MediaColumns.DISPLAY_NAME)
        projectionList.add(MediaStore.MediaColumns.DATE_MODIFIED)
        projectionList.add(MediaStore.MediaColumns.MIME_TYPE)
        projectionList.add(MediaStore.MediaColumns.WIDTH)
        projectionList.add(MediaStore.MediaColumns.HEIGHT)
        projectionList.add(MediaStore.MediaColumns.SIZE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            projectionList.add(MediaStore.MediaColumns.ORIENTATION)
        }

        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projectionList.toTypedArray(), selection, null,
            MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        )

        val allPhotosAlbumName = context.getString(R.string.label_all_i)
        val allPhotoModelArrayList = ArrayList<PhotoModel>()

        val aModel = AlbumPhotoModel()
        aModel.albumName = allPhotosAlbumName
        aModel.photoModelArrayList = allPhotoModelArrayList
        albumWithMediaArrayList.add(aModel)

        if (cursor == null) {
        } else if (cursor.moveToFirst()) {
            val mMetadataRetriever = MediaMetadataRetriever()

            var albumNameCol = -1
            var orientationCol = -1

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                albumNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                orientationCol = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION)
            }

            do {
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
                val name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                val dateTime = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED))
                val type = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
                val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))

                if (path.isNullOrEmpty() || type.isNullOrEmpty()) {
                    continue
                }

                val file = File(path)
                if (!file.isFile) {
                    continue
                }

                var width: Int
                var height: Int
                var orientation = 0

                val isVideo = type.contains(FileConstants.MediaTypeVideo)

                if (orientationCol != -1) {
                    orientation = cursor.getInt(orientationCol)
                }
                width = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH))
                height = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT))
                if (width == 0 || height == 0) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(path, options)
                    width = options.outWidth
                    height = options.outHeight
                }
                if (orientation == 90 || orientation == 270) {
                    val temp = width
                    width = height
                    height = temp
                }
//                if (width < FileConstants.AlbumPhotoMinWidth || height < FileConstants.AlbumPhotoMinHeight) {
//                    continue
//                }

                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri("external"), id)

                var duration: String? = "0"

                if (duration.isNullOrEmpty()) {
                    duration = "0"
                }

                val photoModel = PhotoModel()
                photoModel.name = name
                photoModel.uri = uri
                photoModel.path = path
                photoModel.time = dateTime
                photoModel.width = width
                photoModel.height = height
                photoModel.orientation = orientation
                photoModel.size = size
                photoModel.duration = 0
                photoModel.type = type

                if (albumNameCol != -1) {
                    val albumName = cursor.getString(albumNameCol)?.let { it } ?: ""
                    if (albumName.isNotEmpty()) {
                        val mPos = getAlbumPosFromAlbumList(albumName)
                        if (mPos != -1) {
                            albumWithMediaArrayList[mPos].photoModelArrayList.add(photoModel)
                        } else {
                            val albumModel = AlbumPhotoModel()
                            albumModel.albumName = albumName
                            albumModel.albumUrl = path
                            albumModel.photoModelArrayList.add(photoModel)
                            albumWithMediaArrayList.add(albumModel)
                        }
                    }
                }
                allPhotoModelArrayList.add(photoModel)
            } while (cursor.moveToNext())
            cursor.close()
            mMetadataRetriever.release()

            val mPos = getAlbumPosFromAlbumList(allPhotosAlbumName)
            if (mPos != -1) {
                albumWithMediaArrayList[mPos].photoModelArrayList = allPhotoModelArrayList
            }

            val size = albumWithMediaArrayList.size
            Timber.tag(BaseConstants.LogTag).e(size.toString())
        }
        onAlbumLoad.invoke(albumWithMediaArrayList)
        //}
    }

    private fun getAlbumPosFromAlbumList(albumName: String?): Int {
        for (i in 0 until albumWithMediaArrayList.size) {
            if (albumWithMediaArrayList[i].albumName == albumName) {
                return i
            }
        }
        return -1
    }
}