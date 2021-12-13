package com.android.jesse.privacymonitor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.jesse.collect.PrivacyCollect
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(this, "test", Toast.LENGTH_SHORT).show()

        tvConfirm.setOnClickListener {
            PrivacyCollect.stopCollect(this)
        }
    }

    override fun onStart() {
        super.onStart()

        Toast.makeText(this, "onStart", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "哈哈哈", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show()
    }
}
