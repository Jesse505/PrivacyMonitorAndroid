package com.android.jesse.privacymonitor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.jesse.collect.PrivacyCollect
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Test.getMacAddressByInterface()

        tvConfirm.setOnClickListener {
            PrivacyCollect.stopCollect(this)
        }
    }
}
