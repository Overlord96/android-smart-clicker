package com.smartclicker.app.model

data class AutomationRule(
    val id: String,
    val name: String,
    val triggerTexts: List<String>,
    val targetViewId: String? = null,
    val clickCooldownMs: Long = 1000L,
    val isEnabled: Boolean = true
) {
    companion object {
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
    }
}
