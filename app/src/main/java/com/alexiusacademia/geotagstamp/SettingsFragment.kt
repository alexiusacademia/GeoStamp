package com.alexiusacademia.geotagstamp

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat


class SettingsFragment : PreferenceFragmentCompat() {


    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences_layout)
    }*/

    // Replaced the onCreate method because of using the fragment to the existing layout
    // instead of using a separate layout for the fragment
    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        setPreferencesFromResource(R.xml.preferences_layout, p1)
    }
}