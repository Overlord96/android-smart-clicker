package com.smartclicker.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartclicker.app.R
import com.smartclicker.app.databinding.ActivityMainBinding
import com.smartclicker.app.model.AutomationRule
import com.smartclicker.app.service.FloatingOverlayService
import com.smartclicker.app.service.SmartAccessibilityService
import com.smartclicker.app.utils.LocaleHelper
import com.smartclicker.app.utils.PermissionUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val logAdapter = LogAdapter()
    private lateinit var ruleAdapter: RuleAdapter
    private var rulesList = mutableListOf<AutomationRule>()

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupRulesSection()
        setupUIListeners()
        observeServiceState()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateOverlayButtonState(FloatingOverlayService.isOverlayRunning.value)
    }

    private fun setupRecyclerViews() {
        // Logs list
        binding.rvLogs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
    }

    private fun setupRulesSection() {
        rulesList = AutomationRule.loadRules(this)
        SmartAccessibilityService.updateRules(rulesList)

        ruleAdapter = RuleAdapter(
            rules = rulesList,
            onRuleToggle = { _, _ ->
                AutomationRule.saveRules(this, rulesList)
                SmartAccessibilityService.updateRules(rulesList)
            },
            onRuleDelete = { rule ->
                ruleAdapter.removeRule(rule)
                AutomationRule.saveRules(this, rulesList)
                SmartAccessibilityService.updateRules(rulesList)
            }
        )

        binding.rvRules.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = ruleAdapter
        }

        binding.btnAddRule.setOnClickListener {
            showAddRuleDialog()
        }
    }

    private fun showAddRuleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_rule, null)
        val etName = dialogView.findViewById<EditText>(R.id.etRuleName)
        val etTriggers = dialogView.findViewById<EditText>(R.id.etRuleTriggers)
        val etViewId = dialogView.findViewById<EditText>(R.id.etRuleViewId)
        val etCooldown = dialogView.findViewById<EditText>(R.id.etRuleCooldown)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etName.text.toString().trim()
                val triggersRaw = etTriggers.text.toString().trim()
                if (name.isNotEmpty() && triggersRaw.isNotEmpty()) {
                    val triggers = triggersRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val viewId = etViewId.text.toString().trim().ifEmpty { null }
                    val cooldown = etCooldown.text.toString().toLongOrNull() ?: 1000L

                    val newRule = AutomationRule(
                        id = "rule_${System.currentTimeMillis()}",
                        name = name,
                        triggerTexts = triggers,
                        targetViewId = viewId,
                        clickCooldownMs = cooldown,
                        isEnabled = true
                    )
                    ruleAdapter.addRule(newRule)
                    AutomationRule.saveRules(this, rulesList)
                    SmartAccessibilityService.updateRules(rulesList)
                    SmartAccessibilityService.emitLog("Added custom rule '$name' with triggers [${triggers.joinToString()}]")
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupUIListeners() {
        // Toggle language button (EN <-> RU)
        binding.btnLanguageToggle.setOnClickListener {
            val currentLang = LocaleHelper.getSavedLanguage(this)
            val newLang = if (currentLang == "ru") "en" else "ru"
            LocaleHelper.setLanguage(this, newLang)
            recreate()
        }

        // Grant permission button
        binding.btnGrantPermission.setOnClickListener {
            PermissionUtils.openAccessibilitySettings(this)
        }

        // Floating Overlay toggle
        binding.btnToggleOverlay.setOnClickListener {
            handleOverlayToggle()
        }

        // Start / Stop Automation Toggle
        binding.btnToggleAutomation.setOnClickListener {
            val isEnabled = PermissionUtils.isAccessibilityServiceEnabled(
                this,
                SmartAccessibilityService::class.java
            )
            if (!isEnabled) {
                Toast.makeText(this, R.string.log_permission_required, Toast.LENGTH_SHORT).show()
                PermissionUtils.openAccessibilitySettings(this)
                return@setOnClickListener
            }

            val currentlyRunning = SmartAccessibilityService.isAutomationRunning.value
            SmartAccessibilityService.setAutomationRunning(!currentlyRunning)
        }

        // Clear logs
        binding.btnClearLogs.setOnClickListener {
            logAdapter.clear()
        }

        // Engine switches
        binding.switchNodeEngine.setOnCheckedChangeListener { _, isChecked ->
            SmartAccessibilityService.setNodeEngineEnabled(isChecked)
        }

        binding.switchVisionEngine.setOnCheckedChangeListener { _, isChecked ->
            SmartAccessibilityService.setVisionEngineEnabled(isChecked)
        }

        // Cooldown configuration
        binding.etCooldownMs.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            val cooldown = text.toLongOrNull() ?: 500L
            SmartAccessibilityService.setClickCooldown(cooldown)
        }
    }

    private fun handleOverlayToggle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val isRunning = FloatingOverlayService.isOverlayRunning.value
        if (isRunning) {
            FloatingOverlayService.stopOverlay(this)
        } else {
            val intent = Intent(this, FloatingOverlayService::class.java)
            startService(intent)
            // Move activity to back so user is immediately over their game/target app
            moveTaskToBack(true)
        }
    }

    private fun updateOverlayButtonState(isRunning: Boolean) {
        if (isRunning) {
            binding.btnToggleOverlay.text = getString(R.string.action_stop_overlay)
            binding.btnToggleOverlay.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_red)
        } else {
            binding.btnToggleOverlay.text = getString(R.string.action_start_overlay)
            binding.btnToggleOverlay.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
        }
    }

    private fun observeServiceState() {
        // Service bound state
        lifecycleScope.launch {
            SmartAccessibilityService.isServiceBound.collectLatest { isBound ->
                updateStatusDisplay(isBound, SmartAccessibilityService.isAutomationRunning.value)
            }
        }

        // Automation running state
        lifecycleScope.launch {
            SmartAccessibilityService.isAutomationRunning.collectLatest { isRunning ->
                val isBound = SmartAccessibilityService.isServiceBound.value
                updateStatusDisplay(isBound, isRunning)
            }
        }

        // Floating overlay state
        lifecycleScope.launch {
            FloatingOverlayService.isOverlayRunning.collectLatest { isRunning ->
                updateOverlayButtonState(isRunning)
            }
        }

        // Log events stream
        lifecycleScope.launch {
            SmartAccessibilityService.logFlow.collectLatest { entry ->
                logAdapter.addLog(entry)
                binding.rvLogs.scrollToPosition(0)
            }
        }
    }

    private fun updatePermissionStatus() {
        val isServiceEnabled = PermissionUtils.isAccessibilityServiceEnabled(
            this,
            SmartAccessibilityService::class.java
        )
        updateStatusDisplay(isServiceEnabled, SmartAccessibilityService.isAutomationRunning.value)
    }

    private fun updateStatusDisplay(isServiceEnabled: Boolean, isRunning: Boolean) {
        if (!isServiceEnabled) {
            binding.viewStatusIndicator.setBackgroundResource(R.drawable.shape_status_indicator)
            binding.tvServiceStatus.text = getString(R.string.status_disabled)
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            binding.tvAutomationStatus.text = getString(R.string.status_permission_missing)
            binding.btnGrantPermission.visibility = View.VISIBLE
            binding.btnToggleAutomation.isEnabled = false
            binding.btnToggleAutomation.text = getString(R.string.action_start)
            binding.btnToggleAutomation.backgroundTintList = ContextCompat.getColorStateList(this, R.color.surface_variant)
        } else {
            binding.btnGrantPermission.visibility = View.GONE
            binding.btnToggleAutomation.isEnabled = true
            binding.tvServiceStatus.text = getString(R.string.status_enabled)
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green))

            if (isRunning) {
                binding.tvAutomationStatus.text = getString(R.string.status_running)
                binding.tvAutomationStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
                binding.btnToggleAutomation.text = getString(R.string.action_stop)
                binding.btnToggleAutomation.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_red)
            } else {
                binding.tvAutomationStatus.text = getString(R.string.status_stopped)
                binding.tvAutomationStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                binding.btnToggleAutomation.text = getString(R.string.action_start)
                binding.btnToggleAutomation.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_green)
            }
        }
    }
}
