package com.jwenfeng.pulltodetailslayout

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.jwenfeng.library.PullToDetailsLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_pull_to_details.onPageChangeListener = object:PullToDetailsLayout.OnPageChangeListener{
            override fun onChange(status: Int) {
                Log.d("MainActivity","status:$status")
            }

        }
    }
}
