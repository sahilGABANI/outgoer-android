package com.outgoer.api.authentication

import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson
import com.outgoer.api.authentication.model.LoggedInUser
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.api.profile.model.LocationUpdateRequest
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.base.prefs.LocalPrefs
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import timber.log.Timber

/**
 *
 * This class is responsible for caching the logged in user so that the user can
 * be accessed without having to contact the server meaning it's faster and can
 * be accessed offline
 */
class LoggedInUserCache(
    private val localPrefs: LocalPrefs,
) {
    var ITEM_1 = "item1"
    var ITEM_2 = "item2"
    private var outgoerUser: OutgoerUser? = null
    private var registerVenueRequest: RegisterVenueRequest? = null

    private val loggedInUserCacheUpdatesSubject = PublishSubject.create<Unit>()
    val loggedInUserCacheUpdates: Observable<Unit> = loggedInUserCacheUpdatesSubject.hide()

    private val userAuthenticationFailSubject = PublishSubject.create<Unit>()
    val userAuthenticationFail: Observable<Unit> = userAuthenticationFailSubject.hide()

    private val invitedAsCoHostSubject = PublishSubject.create<LiveEventInfo>()
    val invitedAsCoHost: Observable<LiveEventInfo> = invitedAsCoHostSubject.hide()

    private var userUnauthorized: Boolean = false

    enum class PreferenceKey(val identifier: String) {
        LOGGED_IN_USER_JSON_KEY("loggedInUser"),
        LOGGED_IN_USER_TOKEN("token"),
        LOGGED_IN_USER_SOCKET_TOKEN("socket_token"),
        LOGGED_IN_USER_CHANNEL_ID("channel_id"),
        LOCATION_LATITUDE("location_latitude"),
        LOCATION_LONGITUDE("location_longitude"),
        LOCATION_LONGITUDE_BOUNDS("location_longitude_bounds"),
        MAP_BOTTOMSHEET_OPEN("map_sheet_open"),
        CREATE_VENUE_DATA("create_venue_data"),
        CREDENTIAL_STORAGE("cred_store_info"),
    }

    init {
        userUnauthorized = false
        loadLoggedInUserFromLocalPrefs()
    }

    private var loggedInUserTokenLocalPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOGGED_IN_USER_TOKEN.identifier, value)
        }

    private var loggedInUserSocketTokenLocalPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOGGED_IN_USER_SOCKET_TOKEN.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOGGED_IN_USER_SOCKET_TOKEN.identifier, value)
        }

    private var loggedInUserLocationLatitudePref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOCATION_LATITUDE.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOCATION_LATITUDE.identifier, value)
        }

    private var loggedInUserLocationLongitudePref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOCATION_LONGITUDE.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOCATION_LONGITUDE.identifier, value)
        }

    private var loggedInUserLocationLongitudeBoundsPref: String?
        get() {
            return localPrefs.getString(PreferenceKey.LOCATION_LONGITUDE_BOUNDS.identifier, null)
        }
        set(value) {
            localPrefs.putString(PreferenceKey.LOCATION_LONGITUDE_BOUNDS.identifier, value)
        }

    private var saveCredentialsInfo: MutableSet<String>
        get() {
            return localPrefs.getStringSet(PreferenceKey.CREDENTIAL_STORAGE.identifier, mutableSetOf())
        }
        set(value) {
            localPrefs.putStringSet(PreferenceKey.CREDENTIAL_STORAGE.identifier, value)
        }


    private var mapBottomSheetAlreadyOpen: Boolean
        get() {
            return localPrefs.getBoolean(PreferenceKey.MAP_BOTTOMSHEET_OPEN.identifier, false)
        }
        set(value) {
            localPrefs.putBoolean(PreferenceKey.MAP_BOTTOMSHEET_OPEN.identifier, value)
        }
        fun setVenueRequest(registerVenueRequest: RegisterVenueRequest?) {
        localPrefs.putString(
            PreferenceKey.CREATE_VENUE_DATA.identifier,
            Gson().toJson(registerVenueRequest)
        )
        loadVenueRequestLocalPrefs()
        loggedInUserCacheUpdatesSubject.onNext(Unit)
    }


    private fun loadVenueRequestLocalPrefs() {
        val userJsonString =
            localPrefs.getString(PreferenceKey.CREATE_VENUE_DATA.identifier, null)
        var registerVenueRequest: RegisterVenueRequest? = null

        if (userJsonString != null) {
            try {
                registerVenueRequest = Gson().fromJson(userJsonString, RegisterVenueRequest::class.java)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
        }
        this.registerVenueRequest = registerVenueRequest
    }

    fun getVenueRequest(): RegisterVenueRequest? {
        return registerVenueRequest
    }


    fun saveCredInfoEncrypt(listCred: Pair<String, String>) {
        var jsonObj = JSONObject()
        jsonObj.put(ITEM_1, listCred.first)
        jsonObj.put(ITEM_2, listCred.second)

        var mInfo = saveCredentialsInfo
        mInfo.add(jsonObj.toString())
        saveCredentialsInfo = mInfo
    }

    fun getCredInfoEncrypt(): MutableSet<String> {
        return saveCredentialsInfo
    }


    fun setMapAlreadyOpen(isAlreadyOpen: Boolean) {
        mapBottomSheetAlreadyOpen = isAlreadyOpen
    }

    fun getMapAlreadyOpen(): Boolean {
        return mapBottomSheetAlreadyOpen
    }

    fun setLocation(locationUpdateRequest: LocationUpdateRequest) {
        loggedInUserLocationLongitudePref = locationUpdateRequest.longitude.toString()
        loggedInUserLocationLatitudePref = locationUpdateRequest.latitude.toString()
    }

    fun setLocationBounds(bounds: LatLngBounds) {
        val boundsJson = Gson().toJson(bounds)
        loggedInUserLocationLongitudeBoundsPref = boundsJson
    }

    fun getBounds(): LatLngBounds? {
        val boundsJson = loggedInUserLocationLongitudeBoundsPref
        return if (boundsJson != null) {
            Gson().fromJson(boundsJson, LatLngBounds::class.java)
        } else {
            null
        }
    }

    fun getLocationLongitude(): String {
        return loggedInUserLocationLongitudePref ?: "0.0"
    }

    fun getLocationLatitude(): String {
        return loggedInUserLocationLatitudePref ?: "0.0"
    }

    private fun loadLoggedInUserFromLocalPrefs() {
        val userJsonString =
            localPrefs.getString(PreferenceKey.LOGGED_IN_USER_JSON_KEY.identifier, null)
        var outgoerUser: OutgoerUser? = null

        if (userJsonString != null) {
            try {
                outgoerUser = Gson().fromJson(userJsonString, OutgoerUser::class.java)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse logged in user from json string")
            }
        }
        this.outgoerUser = outgoerUser
    }

    fun setLoggedInUserToken(token: String?) {
        userUnauthorized = false
        loggedInUserTokenLocalPref = token
        loadLoggedInUserFromLocalPrefs()
    }

    fun getLoginUserToken(): String? {
        return loggedInUserTokenLocalPref
    }

    fun setLoggedInUserSocketToken(token: String?) {
        userUnauthorized = false
        loggedInUserSocketTokenLocalPref = token
        loadLoggedInUserFromLocalPrefs()
    }

    fun getLoginUserSocketToken(): String? {
        return loggedInUserSocketTokenLocalPref
    }

    fun setLoggedInUser(outgoerUser: OutgoerUser?) {
        localPrefs.putString(
            PreferenceKey.LOGGED_IN_USER_JSON_KEY.identifier,
            Gson().toJson(outgoerUser)
        )
        loadLoggedInUserFromLocalPrefs()
        loggedInUserCacheUpdatesSubject.onNext(Unit)
    }

    fun setEventChannelId(channelId: String) {
        localPrefs.putStringSet(
            PreferenceKey.LOGGED_IN_USER_CHANNEL_ID.identifier,
            setOf(channelId)
        )
    }

    fun getUserChannel(): Set<String> {
        return localPrefs.getStringSet(
            PreferenceKey.LOGGED_IN_USER_CHANNEL_ID.identifier,
            mutableSetOf()
        )
    }

    fun removeEventChannelId(channelId: String) {
        localPrefs.removeStringFromSet(
            channelId,
            PreferenceKey.LOGGED_IN_USER_CHANNEL_ID.identifier
        )
    }

    fun clearLoggedInUserLocalPrefs() {
        // These preferences are currently only saved locally so we only want to wipe
        // them when a new user logs in, otherwise if the user logs out and logs back
        // in the settings will be gone
        clearUserPreferences()
    }

    fun getLoggedInUser(): LoggedInUser? {
        val loggedInUser = outgoerUser ?: return null
        return LoggedInUser(loggedInUser, loggedInUserTokenLocalPref)
    }

    fun getUserId(): Int? {
        return getLoggedInUser()?.loggedInUser?.id
    }


    /**
     * Clear previous user preferences, if the current logged in user is different
     */
    private fun clearUserPreferences() {
        try {
            outgoerUser = null
            for (preferenceKey in PreferenceKey.values()) {
                if(!preferenceKey.equals(PreferenceKey.CREDENTIAL_STORAGE))
                    localPrefs.removeValue(preferenceKey.identifier)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun userUnauthorized() {
        if (!userUnauthorized) {
            userAuthenticationFailSubject.onNext(Unit)
        }
        userUnauthorized = true
    }

    fun invitedAsCoHost(liveEventInfo: LiveEventInfo) {
        invitedAsCoHostSubject.onNext(liveEventInfo)
    }
}