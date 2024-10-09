package com.outgoer.ui.savecredentials.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.outgoer.api.follow.model.SuggestedUser
import org.json.JSONObject
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


class SecurePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "create"
        private const val KEY = "secure_key_crede" // Change this to your own secret key
        private const val ALGORITHM = "AES"

        var ITEM_1 = "item1"
        var ITEM_2 = "item2"
        var ITEM_3 = "item3"
        var ITEM_4 = "item4"
        var CRED_STORE_INFO = "saved_posts_info"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(username: String, password: String, uName: String, avatar: String) {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey())
            val encryptedUsername = cipher.doFinal(username.toByteArray())
            val encryptedPassword = cipher.doFinal(password.toByteArray())


            val set = hashSetOf<String>()

            val pefCredList = prefs.getStringSet(CRED_STORE_INFO, mutableSetOf())
            if (pefCredList != null) {
                set.addAll(pefCredList)
            }
            println("pefCredList: " + pefCredList)

            var jsonObj = JSONObject()
            jsonObj.put(ITEM_1, Base64.encodeToString(encryptedUsername, Base64.DEFAULT))
            jsonObj.put(ITEM_2, Base64.encodeToString(encryptedPassword, Base64.DEFAULT))
            jsonObj.put(ITEM_3, uName)
            jsonObj.put(ITEM_4, avatar)

            set.add(jsonObj.toString())

            println("mInfo: " + set)

            var editor = prefs.edit()
            editor.putStringSet(CRED_STORE_INFO, set)
            editor.apply()
            editor.commit()
            editor = null

            println("mInfo:123 " + prefs.getStringSet(CRED_STORE_INFO, mutableSetOf()))
        } catch (e: GeneralSecurityException) {
            println("Error on save: " + e.printStackTrace())
            println("Error on save: " + e.message)
        }
    }

    fun getCredentials(): ArrayList<SuggestedUser> {

        var listOfCredentials: ArrayList<SuggestedUser> =  arrayListOf()

        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, generateSecretKey())

            val listcred = prefs.getStringSet(CRED_STORE_INFO, mutableSetOf())

            listcred?.forEach { cred ->
                var jsonObject = JSONObject(cred)
                var username = jsonObject.getString(ITEM_1)
                var password = jsonObject.getString(ITEM_2)
                var avatar = jsonObject.getString(ITEM_4)
                var uName = jsonObject.getString(ITEM_3)


                if (username != null && password != null) {
                    val decryptedUsername = cipher.doFinal(Base64.decode(username, Base64.DEFAULT)).toString(Charsets.UTF_8)
                    val decryptedPassword = cipher.doFinal(Base64.decode(password, Base64.DEFAULT)).toString(Charsets.UTF_8)

                    println("ITEM_1: ${username} :de: ${decryptedUsername}")
                    println("ITEM_2: ${password} :de: ${decryptedPassword}")
                    println("ITEM_4: " + avatar)
                    println("ITEM_3: " + uName)

                    listOfCredentials.add(SuggestedUser(uId = decryptedUsername, uPass = decryptedPassword, avatar = avatar, uName = uName))
                }
            }

            return listOfCredentials

        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        }

        return arrayListOf()
    }

    private fun generateSecretKey(): SecretKey {
        return SecretKeySpec(KEY.toByteArray(), ALGORITHM)
    }
}
