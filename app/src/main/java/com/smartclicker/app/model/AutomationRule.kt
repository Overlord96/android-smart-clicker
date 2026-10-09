package com.smartclicker.app.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AutomationRule(
    val id: String,
    val name: String,
    val triggerTexts: List<String>,
    val targetViewId: String? = null,
    val clickCooldownMs: Long = 1000L,
    var isEnabled: Boolean = true
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("triggerTexts", JSONArray(triggerTexts))
        json.put("targetViewId", targetViewId ?: "")
        json.put("clickCooldownMs", clickCooldownMs)
        json.put("isEnabled", isEnabled)
        return json
    }

    companion object {
        private const val PREFS_NAME = "smart_clicker_rules"
        private const val KEY_RULES = "saved_rules_json"

        fun defaultRules(): List<AutomationRule> = listOf(
            AutomationRule(
                id = "rule_claim",
                name = "Claim / Collect Rewards",
                triggerTexts = listOf("Claim", "Collect", "Забрать", "Собрать", "Получить"),
                clickCooldownMs = 1500L
            ),
            AutomationRule(
                id = "rule_next",
                name = "Next / Continue / OK",
                triggerTexts = listOf("Next", "Continue", "OK", "Далее", "Продолжить", "Принять"),
                clickCooldownMs = 1000L
            ),
            AutomationRule(
                id = "rule_battle",
                name = "Start / Battle / Attack",
                triggerTexts = listOf("Start", "Battle", "Attack", "Play", "Старт", "Бой", "Атака", "Играть"),
                clickCooldownMs = 2000L
            ),
            AutomationRule(
                id = "rule_retry",
                name = "Retry / Replay",
                triggerTexts = listOf("Retry", "Replay", "Повторить", "Снова"),
                clickCooldownMs = 2000L
            )
        )

        fun fromJson(json: JSONObject): AutomationRule {
            val triggers = mutableListOf<String>()
            val triggersArray = json.optJSONArray("triggerTexts")
            if (triggersArray != null) {
                for (i in 0 until triggersArray.length()) {
                    triggers.add(triggersArray.getString(i))
                }
            }
            val viewId = json.optString("targetViewId")
            return AutomationRule(
                id = json.optString("id", System.currentTimeMillis().toString()),
                name = json.optString("name", "Rule"),
                triggerTexts = triggers,
                targetViewId = if (viewId.isNullOrEmpty()) null else viewId,
                clickCooldownMs = json.optLong("clickCooldownMs", 1000L),
                isEnabled = json.optBoolean("isEnabled", true)
            )
        }

        fun loadRules(context: Context): MutableList<AutomationRule> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_RULES, null) ?: return defaultRules().toMutableList()
            val list = mutableListOf<AutomationRule>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                return defaultRules().toMutableList()
            }
            return if (list.isEmpty()) defaultRules().toMutableList() else list
        }

        fun saveRules(context: Context, rules: List<AutomationRule>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val array = JSONArray()
            for (rule in rules) {
                array.put(rule.toJson())
            }
            prefs.edit().putString(KEY_RULES, array.toString()).apply()
        }
    }
}
