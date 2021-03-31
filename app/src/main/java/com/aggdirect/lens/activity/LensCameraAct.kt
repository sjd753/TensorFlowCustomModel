package com.aggdirect.lens.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aggdirect.lens.R
import com.aggdirect.lens.fragment.CameraPreviewFragment

class LensCameraAct : AppCompatActivity() {

    companion object {
        internal const val RC_CAPTURE = 111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lens_activity_custom_camera)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        supportFragmentManager.beginTransaction()
            .add(
                R.id.fragmentContainer,
                CameraPreviewFragment(),
                CameraPreviewFragment::class.java.simpleName
            )
            .commit()
    }
}