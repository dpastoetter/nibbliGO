package com.nibbli.nibbligo.core.mobileactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device phone actions ported from Google AI Edge Gallery MobileActionsViewModel (Apache 2.0).
 */
@Singleton
class MobileActionsPerformer @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    fun turnOnFlashlight(): String = setFlashlight(isEnabled = true)

    fun turnOffFlashlight(): String = setFlashlight(isEnabled = false)

    fun createContact(params: ContactParams): String {
        val intent =
            Intent(ContactsContract.Intents.Insert.ACTION)
                .apply { type = ContactsContract.RawContacts.CONTENT_TYPE }
                .apply {
                    putExtra(ContactsContract.Intents.Insert.NAME, "${params.firstName} ${params.lastName}".trim())
                    putExtra(ContactsContract.Intents.Insert.EMAIL, params.email)
                    putExtra(
                        ContactsContract.Intents.Insert.EMAIL_TYPE,
                        ContactsContract.CommonDataKinds.Email.TYPE_WORK,
                    )
                    putExtra(ContactsContract.Intents.Insert.PHONE, params.phoneNumber)
                    putExtra(
                        ContactsContract.Intents.Insert.PHONE_TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
                    )
                }
        return launchActivity(intent, "create contact")
    }

    fun sendEmail(params: EmailParams): String {
        val uriText = buildString {
            append("mailto:")
            if (params.to.isNotBlank()) {
                append(Uri.encode(params.to))
            }
            val queryParts = buildList {
                if (params.subject.isNotBlank()) {
                    add("subject=${Uri.encode(params.subject)}")
                }
                if (params.body.isNotBlank()) {
                    add("body=${Uri.encode(params.body)}")
                }
            }
            if (queryParts.isNotEmpty()) {
                append('?')
                append(queryParts.joinToString("&"))
            }
        }
        val intent = Intent(Intent.ACTION_SENDTO, uriText.toUri())
        if (intent.resolveActivity(appContext.packageManager) == null) {
            return "No email app found on this device"
        }
        return launchActivity(intent, "send email")
    }

    fun showLocationOnMap(params: MapParams): String {
        val encodedLocation = URLEncoder.encode(params.location, StandardCharsets.UTF_8.toString())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "geo:0,0?q=$encodedLocation".toUri()
        }
        return launchActivity(intent, "show location on map")
    }

    fun openWifiSettings(): String {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        return launchActivity(intent, "open wifi settings")
    }

    fun createCalendarEvent(params: CalendarParams): String {
        var ms = System.currentTimeMillis()
        try {
            val localDateTime = LocalDateTime.parse(params.datetime)
            val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())
            ms = zonedDateTime.toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse date time: '${params.datetime}'", e)
        }

        val intent =
            Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, params.title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ms + 3_600_000)
            }
        return launchActivity(intent, "create calendar event")
    }

    private fun setFlashlight(isEnabled: Boolean): String {
        val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val isFlashAvailable =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (isFlashAvailable) {
                    cameraId = id
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set flashlight", e)
            return e.message ?: "Unknown flashlight error"
        }

        cameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, isEnabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set flashlight", e)
                return e.message ?: "Unknown flashlight error"
            }
        } ?: return "No camera with flash found on this device"

        return ""
    }

    private fun launchActivity(intent: Intent, actionLabel: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to $actionLabel", e)
            e.message ?: "Failed to $actionLabel"
        }
    }

    private companion object {
        const val TAG = "MobileActions"
    }
}
