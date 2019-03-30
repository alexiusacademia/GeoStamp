package com.alexiusacademia.geotagstamp

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.alexiusacademia.geotagstamp.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)

        if (savedInstanceState != null) {
            return
        }

        fragmentManager.beginTransaction().
            add(R.id.frame_settings_fragment_container, SettingsFragment()).
            commit()
    }
}
