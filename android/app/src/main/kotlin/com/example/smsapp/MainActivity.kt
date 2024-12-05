package com.example.smsapp

import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FlutterActivity() {
    private val CHANNEL = "sms.channel"
    private val READ_SMS_PERMISSION_REQUEST = 101

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Ensure binaryMessenger is not null
        val binaryMessenger = flutterEngine.dartExecutor.binaryMessenger
        if (binaryMessenger != null) {
            MethodChannel(binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
                when (call.method) {
                    "getSms" -> {
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            // Request permission
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(android.Manifest.permission.READ_SMS),
                                READ_SMS_PERMISSION_REQUEST
                            )
                            result.error("PERMISSION_DENIED", "SMS read permission required", null)
                        } else {
                            val smsList = getAllSms()
                            result.success(smsList)
                        }
                    }
                    else -> result.notImplemented()
                }
            }
        } else {
            // Handle the case where binaryMessenger is null
            println("Error: BinaryMessenger is null.")
        }
    }

    private fun getAllSms(): List<Map<String, String>> {
        val smsList = mutableListOf<Map<String, String>>()
        val uri: Uri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address")) ?: "No Address"
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: "No Body"
                val dateMillis = it.getLong(it.getColumnIndexOrThrow("date"))
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateMillis))

                smsList.add(
                    mapOf(
                        "address" to address,
                        "body" to body,
                        "date" to date
                    )
                )
            }
        }
        return smsList
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_SMS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                val binaryMessenger = flutterEngine?.dartExecutor?.binaryMessenger
                if (binaryMessenger != null) {
                    MethodChannel(binaryMessenger, CHANNEL).invokeMethod("getSms", null)
                }
            }
        }
    }
}
