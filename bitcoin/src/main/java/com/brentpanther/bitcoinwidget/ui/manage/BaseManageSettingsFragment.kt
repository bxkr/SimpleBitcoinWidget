package com.brentpanther.bitcoinwidget.ui.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import com.brentpanther.bitcoinwidget.BuildConfig
import com.brentpanther.bitcoinwidget.R
import com.brentpanther.bitcoinwidget.WidgetProvider
import com.brentpanther.bitcoinwidget.db.Configuration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

open class BaseManageSettingsFragment : PreferenceFragmentCompat() {

    private val viewModel : ManageWidgetsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.globalSettings.distinctUntilChanged().collect { config ->
                lifecycleScope.launch(Dispatchers.Main) {
                    loadPreferences(config, rootKey)
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun loadPreferences(config: Configuration, rootKey: String?) {
        preferenceManager.preferenceDataStore = ConfigDataStore(config)
        setPreferencesFromResource(R.xml.global_preferences, rootKey)
        addPreferencesFromResource(R.xml.additional_global_preferences)
        loadAdditionalPreferences()
        findPreference<Preference>("build_info")?.apply {
            summary = getString(R.string.version, BuildConfig.VERSION_NAME)
            setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    val view = layoutInflater.inflate(R.layout.layout_license, null) as TextView
                    view.movementMethod = LinkMovementMethod.getInstance()
                    view.setText(HtmlCompat.fromHtml(getString(R.string.licenses), HtmlCompat.FROM_HTML_MODE_COMPACT), TextView.BufferType.SPANNABLE)
                    setView(view)
                    show()
                }
                true
            }
        }
        findPreference<ListPreference>("refresh_interval")?.setSummaryProvider {
            val pref = (it as ListPreference)
            getString(R.string.summary_refresh_interval, pref.entries[pref.findIndexOfValue(pref.value)])
        }
    }

    protected open fun loadAdditionalPreferences() {}

    inner class ConfigDataStore(private val config: Configuration) : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when(key) {
                "fixed_size" -> config.consistentSize
                else -> throw IllegalArgumentException()
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when(key) {
                "fixed_size" -> config.consistentSize = value
            }
            viewModel.updateGlobalSettings(config)
            WidgetProvider.refreshWidgets(requireContext())
        }

        override fun getString(key: String?, defValue: String?): String {
            return when(key) {
                "refresh_interval" -> config.refresh.toString()
                else -> throw IllegalArgumentException()
            }
        }

        override fun putString(key: String?, value: String?) {
            when(key) {
                "refresh_interval" -> config.refresh = value?.toInt() ?: 15
            }
            viewModel.updateGlobalSettings(config)
            WidgetProvider.refreshWidgets(requireContext(), restart = true)
        }
    }

}