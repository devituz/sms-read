package com.example.smsapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartService.setOnClickListener {
            startSmsService()
            Toast.makeText(this, "Service started!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSmsService() {
        val intent = Intent(this, SmsService::class.java)
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, SmsService::class.java))
    }
}
