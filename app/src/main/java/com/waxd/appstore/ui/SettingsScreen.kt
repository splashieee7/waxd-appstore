package com.waxd.appstore.ui

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import com.waxd.appstore.R
import com.waxd.appstore.core.appResources

class SettingsScreen : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = appResources.getString(R.string.pref_file_settings)
        addPreferencesFromResource(R.xml.settings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSlideTransitions(this)
    }
}
