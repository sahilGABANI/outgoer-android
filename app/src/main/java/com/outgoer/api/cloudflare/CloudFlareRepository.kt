package com.outgoer.api.cloudflare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.outgoer.api.cloudflare.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Completable
import io.reactivex.Single
import io.tus.java.client.*
import okhttp3.MultipartBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL

class CloudFlareRepository(
    private val cloudFlareRetrofitAPI: CloudFlareRetrofitAPI,
) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()
    private var data: MutableLiveData<Double> = MutableLiveData<Double>()

    private var tusClient: TusClient? = null
    private var tusUploader: TusUploader? = null
    fun getCloudFlareConfig(): Single<OutgoerResponse<CloudFlareConfig>> {
        return cloudFlareRetrofitAPI.getCloudFlareConfig()
            .doAfterSuccess {}
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun uploadImageToCloudFlare(apiUrl: String, authToken: String, filePart: MultipartBody.Part): Single<UploadImageCloudFlareResponse> {
        return cloudFlareRetrofitAPI.uploadImageToCloudFlare(apiUrl, authToken, filePart)
            .doAfterSuccess {}
            .map { it }
    }

    fun uploadVideoToCloudFlare(apiUrl: String, authToken: String, filePart: MultipartBody.Part): Single<UploadVideoCloudFlareResponse> {
        return cloudFlareRetrofitAPI.uploadVideoToCloudFlare(apiUrl, authToken, filePart)
            .doAfterSuccess {}
            .map { it }
    }


    fun getUploadVideoDetails(apiUrl: String, authToken: String): Single<CloudFlareVideoInfo> {
        return cloudFlareRetrofitAPI.getUploadVideoDetails(apiUrl, authToken)
            .doAfterSuccess {}
            .map { it.result }
    }

    fun getUploadVideoStatus(apiUrl: String, authToken: String): Single<CloudFlareVideoStatus> {
        return cloudFlareRetrofitAPI.getUploadVideoStatus(apiUrl, authToken)
            .doAfterSuccess {}
            .map { it }
    }

    fun uploadVideoUsingTus(apiUrl: String, authToken: String, file: File): Single<String> {
        return Single.create { singleEmitter ->
            val headers = mutableMapOf<String, String>()
            headers["Authorization"] = authToken
            tusClient = TusClient()
            tusClient?.uploadCreationURL = URL(apiUrl)
            tusClient?.headers = headers
            tusClient?.enableResuming(TusURLMemoryStore())
            val upload = TusUpload(file)
            val executor: TusExecutor = object : TusExecutor() {
                @Throws(ProtocolException::class, IOException::class)
                override fun makeAttempt() {
                    tusUploader = tusClient?.resumeOrCreateUpload(upload)

                    tusUploader?.let { tusUploader ->
                        tusUploader.chunkSize = 256 * 1024
                        do {
                            val totalBytes = upload.size
                            val bytesUploaded = tusUploader.offset
                            val progress = bytesUploaded.toDouble() / totalBytes * 100
                            Timber.i("Upload at %06.2f%%.\n", progress)

                            data.postValue(progress)
                        } while (tusUploader.uploadChunk() > -1)
                        tusUploader.finish()
                        Timber.i("Upload finished.")
                        Timber.i(
                            System.out.format(
                                "Upload available at: %s",
                                tusUploader.uploadURL.toString()
                            ).toString()
                        )
                        singleEmitter.onSuccess(tusUploader.uploadURL.toString())

                    }
                }
            }
            executor.makeAttempts()
        }
    }

    fun stopClient() {
        tusUploader?.finish()
    }

    fun getLiveData(): LiveData<Double>? {
        return data
    }

    fun uploadAudioToCloudFlare(apiUrl: String, filePart: MultipartBody.Part, token: String, date: String, signature: String): Completable {
        return cloudFlareRetrofitAPI.uploadAudioToCloudFlare(apiUrl, filePart, date, token, signature)
    }
}