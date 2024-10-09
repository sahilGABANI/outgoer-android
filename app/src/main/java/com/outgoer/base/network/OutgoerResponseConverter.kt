package com.outgoer.base.network

import com.outgoer.base.extension.DeactivatedAccountException
import com.outgoer.base.extension.onSafeError
import com.outgoer.base.extension.onSafeSuccess
import com.outgoer.base.network.model.NewOutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class OutgoerResponseConverter {
    fun <T> convert(outgoerResponse: OutgoerResponse<T>?): Single<T> {
        return convertToSingle(outgoerResponse)
    }

    fun <T> convertToSingle(outgoerResponse: OutgoerResponse<T>?): Single<T> {
        return Single.create { emitter ->
            when {
                outgoerResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !outgoerResponse.success -> {
                    emitter.onSafeError(Exception(outgoerResponse.message))
                }
                outgoerResponse.success -> {
                    emitter.onSafeSuccess(outgoerResponse.data)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun <T> convertToSingleWithFullResponse(outgoerResponse: OutgoerResponse<T>?): Single<OutgoerResponse<T>> {
        return Single.create { emitter ->
            when {
                outgoerResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !outgoerResponse.success -> {
                    if (outgoerResponse.emailVerified != null && outgoerResponse.emailVerified == false) {
                        emitter.onSafeSuccess(outgoerResponse)
                    } else if (outgoerResponse.deactive == true) {
                        emitter.onSafeError(DeactivatedAccountException(outgoerResponse.message))
                    } else {
                        emitter.onSafeError(Exception(outgoerResponse.message))
                    }
                }
                outgoerResponse.success -> {
                    emitter.onSafeSuccess(outgoerResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertCommonResponse(outgoerCommonResponse: OutgoerCommonResponse?): Single<OutgoerCommonResponse> {
        return Single.create { emitter ->
            when {
                outgoerCommonResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !outgoerCommonResponse.success -> {
                    emitter.onSafeError(Exception(outgoerCommonResponse.message))
                }
                outgoerCommonResponse.success -> {
                    emitter.onSafeSuccess(outgoerCommonResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }

    fun convertNewCommonResponse(outgoerCommonResponse: NewOutgoerCommonResponse?): Single<NewOutgoerCommonResponse> {
        return Single.create { emitter ->
            when {
                outgoerCommonResponse == null -> emitter.onSafeError(Exception("Failed to process the info"))
                !outgoerCommonResponse.success -> {
                    emitter.onSafeError(Exception(outgoerCommonResponse.message))
                }
                outgoerCommonResponse.success -> {
                    emitter.onSafeSuccess(outgoerCommonResponse)
                }
                else -> emitter.onSafeError(Exception("Failed to process the info"))
            }
        }
    }
}