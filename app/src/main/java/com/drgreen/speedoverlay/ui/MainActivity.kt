/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Filter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.service.SpeedService
import com.drgreen.speedoverlay.util.PermissionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private companion object {
        val LANGUAGES = listOf("en", "de", "es", "fr", "it")
    }

    private val settings by lazy { SettingsManager(this) }
    private val permissionManager by lazy { PermissionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings.applySettings()
        setContentView(R.layout.activity_main)

        if (!settings.isDisclaimerAccepted) showDisclaimer()
        else checkBatteryOptimization()

        setupUI()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-setup UI to refresh strings from resources
        setContentView(R.layout.activity_main)
        setupUI()
    }

    private fun setupUI() {
        // Service Controls
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (permissionManager.hasAllCriticalPermissions()) {
                startSpeedService()
            } else {
                if (!permissionManager.hasLocationPermission()) permissionManager.requestLocationPermission()
                else if (!permissionManager.hasOverlayPermission()) permissionManager.requestOverlayPermission()
            }
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener { stopSpeedService() }

        // Automation
        val btnDevice = findViewById<Button>(R.id.btn_select_device)
        findViewById<MaterialSwitch>(R.id.switch_autostart).apply {
            isChecked = settings.isAutostartBtEnabled
            btnDevice.visibility = if (isChecked) View.VISIBLE else View.GONE
            setOnCheckedChangeListener { _, checked ->
                settings.isAutostartBtEnabled = checked
                btnDevice.visibility = if (checked) View.VISIBLE else View.GONE
                if (checked) handleBluetoothRequirement(btnDevice)
            }
        }
        btnDevice.setOnClickListener { showDeviceSelectionDialog(it as Button) }
        updateDeviceButtonText(btnDevice)

        // Alerts & Units
        bindSwitch(R.id.switch_audio, settings.isAudioWarningEnabled) { settings.isAudioWarningEnabled = it }
        bindSwitch(R.id.switch_unit, settings.useMph) { settings.useMph = it }

        bindSlider(R.id.slider_tolerance, R.id.tv_tolerance_value, settings.tolerance.toFloat(), "%.0f") { settings.tolerance = it.toInt() }
        bindSlider(R.id.slider_size, R.id.tv_size_value, settings.overlaySize, "%.1fx") { settings.overlaySize = it }
        bindSlider(R.id.slider_alpha, R.id.tv_alpha_value, settings.overlayAlpha, "%d%%", 100f) { settings.overlayAlpha = it }

        // System Settings
        setupDropdowns()
    }

    private fun setupDropdowns() {
        // Dark Mode
        val darkOptions = arrayOf(R.string.dark_mode_auto, R.string.dark_mode_off, R.string.dark_mode_on).map { getString(it) }
        setupExposedDropdown(R.id.dropdown_dark_mode, darkOptions, settings.darkMode) { settings.darkMode = it }

        // Language
        val langOptions = arrayOf(R.string.lang_en, R.string.lang_de, R.string.lang_es, R.string.lang_fr, R.string.lang_it).map { getString(it) }
        val currentIdx = LANGUAGES.indexOf(settings.language).coerceAtLeast(0)
        setupExposedDropdown(R.id.dropdown_language, langOptions, currentIdx) { settings.language = LANGUAGES[it] }
    }

    private fun setupExposedDropdown(id: Int, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
        val adapter = object : ArrayAdapter<String>(this, R.layout.list_item, options) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = options
                        results.count = options.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        findViewById<MaterialAutoCompleteTextView>(id).apply {
            setAdapter(adapter)
            if (selectedIndex in options.indices) {
                setText(options[selectedIndex], false)
            }
            setOnItemClickListener { _, _, position, _ -> onSelect(position) }
        }
    }

    private fun bindSwitch(id: Int, initial: Boolean, onAction: (Boolean) -> Unit) {
        findViewById<MaterialSwitch>(id).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onAction(checked) }
        }
    }

    private fun bindSlider(sliderId: Int, textId: Int, initial: Float, format: String, factor: Float = 1f, onAction: (Float) -> Unit) {
        val tv = findViewById<TextView>(textId)
        findViewById<Slider>(sliderId).apply {
            value = initial
            tv.text = if (format.contains("%d")) String.format(format, (initial * factor).toInt()) else String.format(Locale.getDefault(), format, initial)
            addOnChangeListener { _, v, _ ->
                onAction(v)
                tv.text = if (format.contains("%d")) String.format(format, (v * factor).toInt()) else String.format(Locale.getDefault(), format, v)
            }
        }
    }

    private fun handleBluetoothRequirement(btn: Button) {
        if (!permissionManager.hasBluetoothPermission()) {
            permissionManager.requestBluetoothPermission()
        } else {
            showDeviceSelectionDialog(btn)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateDeviceButtonText(button: Button) {
        val addr = settings.autostartBtDeviceAddress ?: return run { button.text = getString(R.string.select_bt_device) }
        if (!permissionManager.hasBluetoothPermission()) { button.text = addr; return }

        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = try { adapter?.getRemoteDevice(addr) } catch (e: Exception) { null }
        button.text = device?.name ?: addr
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceSelectionDialog(button: Button) {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter?.isEnabled != true) return Toast.makeText(this, "Bluetooth error", Toast.LENGTH_SHORT).show()

        val devices = adapter.bondedDevices.toList()
        if (devices.isEmpty()) return Toast.makeText(this, "No devices", Toast.LENGTH_SHORT).show()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_bt_device)
            .setItems(devices.map { it.name ?: it.address }.toTypedArray()) { _, which ->
                settings.autostartBtDeviceAddress = devices[which].address
                updateDeviceButtonText(button)
            }
            .show()
    }

    private fun showDisclaimer() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.legal_disclaimer_title)
            .setMessage(Html.fromHtml(getString(R.string.legal_disclaimer_text), Html.FROM_HTML_MODE_COMPACT))
            .setCancelable(false)
            .setPositiveButton(R.string.legal_accept) { _, _ -> settings.isDisclaimerAccepted = true; checkBatteryOptimization() }
            .setNegativeButton(R.string.legal_decline) { _, _ -> finish() }
            .show()
    }

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battery_opt_title)
                .setMessage(R.string.battery_opt_message)
                .setPositiveButton(R.string.battery_opt_ok) { _, _ ->
                    val intent = try {
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
                    } catch (e: Exception) {
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    }
                    startActivity(intent)
                }
                .setNegativeButton(R.string.battery_opt_cancel, null)
                .show()
        }
    }

    private fun startSpeedService() {
        val intent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopSpeedService() = stopService(Intent(this, SpeedService::class.java))

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            PermissionManager.REQ_LOCATION -> if (granted) {
                if (permissionManager.hasAllCriticalPermissions()) startSpeedService()
                else permissionManager.requestOverlayPermission()
            } else {
                Toast.makeText(this, R.string.perm_rational, Toast.LENGTH_LONG).show()
            }
            PermissionManager.REQ_BT -> if (granted) updateDeviceButtonText(findViewById(R.id.btn_select_device))
        }
    }
}
