package com.outgoer.api.aws

import android.content.Context
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.outgoer.api.authentication.model.AwsInformation
import com.outgoer.base.extension.onSafeError
import com.outgoer.base.extension.onSafeSuccess
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.io.File

class FileUploader {
    private val baseUrl = "https://outgoer.s3.amazonaws.com/"
    private val bucket = "hinote-media"

    fun upload(
        uploadFile: UploadFile,
        transferUtility: TransferUtility,
        awsData: AwsInformation?
    ): Single<UploadFile> {
        return Single.create { emitter ->
            if (awsData == null) {
                emitter.onSafeError(Exception("Server not responding. Please try after sometime"))
                return@create
            }
            val observer = transferUtility.upload(
                awsData.awsBucket ?: bucket,
                uploadFile.file.name,
                uploadFile.file,
                CannedAccessControlList.PublicReadWrite
            )
            observer.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState) {
                    when (state) {
                        TransferState.COMPLETED -> {
                            val finalBaseUrl = awsData.awsBaseUrl ?: baseUrl
                            val uploadFileWithUrl =
                                uploadFile.copy(fileUrl = finalBaseUrl + uploadFile.file.name)
                            Timber.i("Uploaded File %s", uploadFileWithUrl.toString())
                            emitter.onSafeSuccess(uploadFileWithUrl)
                        }
                        TransferState.FAILED -> {
                            emitter.onSafeError(Throwable("Fail to upload video"))
                        }
                        else -> {

                        }
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}

                override fun onError(id: Int, ex: Exception) {
                    emitter.onSafeError(Throwable(ex.localizedMessage))
                }
            })
        }
    }

    fun uploadMultipleFile(
        context: Context,
        uploadFiles: List<UploadFile>,
        awsInformation: AwsInformation
    ): Single<List<UploadFile>> {
        return transferUtility(context, awsInformation).flatMap { transferUtility ->
            Observable.fromIterable(uploadFiles)
                .flatMapSingle { uploadFile ->
                    upload(
                        uploadFile,
                        transferUtility,
                        awsInformation
                    )
                }.toList()

        }
    }

    private fun transferUtility(
        context: Context,
        awsData: AwsInformation?
    ): Single<TransferUtility> {
        return Single.create { emitter ->
            if (awsData != null) {
                TransferNetworkLossHandler.getInstance(context)
                emitter.onSafeSuccess(
                    TransferUtility.builder().s3Client(s3ClientInitialization(awsData))
                        .context(context).build()
                )
            } else {
                emitter.onSafeError(Exception("Server not responding. Please try after sometime"))
            }
        }
    }

    private fun s3ClientInitialization(awsData: AwsInformation): AmazonS3 {
        val clientConfiguration = ClientConfiguration()
        clientConfiguration.maxErrorRetry = 2
        clientConfiguration.connectionTimeout = 35 * 1000
        clientConfiguration.socketTimeout = 35 * 1000
        return AmazonS3Client(
            BasicAWSCredentials(
                awsData.awsAccessKeyId,
                awsData.awsSecretAccessKey
            ), Region.getRegion(awsData.awsDefaultRegion ?: Regions.AP_SOUTHEAST_2.name)
        )
    }
}

data class UploadFile(
    val file: File,
    val fileType: FileType,
    val fileUrl: String? = null
)

enum class FileType {
    video,
    //videoThumb,
    music
}


enum class CreateMediaType {
    post,
    post_video,
    reels,
    reels_video,
    sponty,
    sponty_video,
    story,
    story_video
}