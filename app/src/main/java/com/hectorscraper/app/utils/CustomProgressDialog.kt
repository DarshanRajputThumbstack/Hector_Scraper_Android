package com.hectorscraper.app.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.hectorscraper.app.R
import com.hectorscraper.app.databinding.CustomProgressbarBinding

class CustomProgressDialog(context: Context) : Dialog(context) {

    var binding: CustomProgressbarBinding = CustomProgressbarBinding.inflate(layoutInflater)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window!!.decorView.setBackgroundResource(R.color.transparent)
        setContentView(binding.root)
        setCancelable(false)
    }
}