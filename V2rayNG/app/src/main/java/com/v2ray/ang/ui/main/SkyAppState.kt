package com.v2ray.ang.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.v2ray.ang.handler.MmkvManager

/**
 * SkyVPN: lightweight, backend-free state for the customer-facing screens (welcome name,
 * selected connection mode, simulated plan/subscription, promo codes).
 *
 * The brief's promo-code backend (section 4) does not exist yet, so this mirrors the exact
 * local-simulation approach already used by the reference web mockup (skyvpn-mobile.html):
 * choosing a plan or redeeming a code only updates local state, nothing is charged or synced
 * to a server. Persisted via MMKV so - unlike the web mockup, which resets on page reload -
 * it survives an app restart.
 */
object SkyAppState {
    private const val KEY_WELCOME_NAME = "sky_welcome_name"
    private const val KEY_PLAN = "sky_plan"
    private const val KEY_DAYS_LEFT = "sky_days_left"
    private const val KEY_REDEEMED_CODES = "sky_redeemed_codes"
    private const val KEY_SELECTED_MODE = "sky_selected_mode"

    var welcomeName: String by mutableStateOf(
        MmkvManager.decodeSettingsString(KEY_WELCOME_NAME, "") ?: ""
    )
        private set

    var plan: String by mutableStateOf(
        MmkvManager.decodeSettingsString(KEY_PLAN, "") ?: ""
    )
        private set

    var daysLeft: Int by mutableStateOf(
        MmkvManager.decodeSettingsInt(KEY_DAYS_LEFT, 0)
    )
        private set

    var selectedMode: Int by mutableStateOf(
        MmkvManager.decodeSettingsInt(KEY_SELECTED_MODE, 0)
    )
        private set

    val hasWelcomeName: Boolean get() = welcomeName.isNotBlank()

    private val redeemedCodes: MutableSet<String> =
        MmkvManager.decodeSettingsStringSet(KEY_REDEEMED_CODES) ?: mutableSetOf()

    // Demo codes only, same two as the reference web mockup - real redemption needs the
    // brief's section 4 backend, which is not built yet.
    private val demoCodes: Map<String, Pair<String, Int>> = mapOf(
        "SKYVPN7" to ("plus" to 7),
        "SKYVPN30" to ("premium" to 30),
    )

    fun setWelcomeName(name: String) {
        welcomeName = name
        MmkvManager.encodeSettings(KEY_WELCOME_NAME, name)
    }

    fun selectMode(index: Int) {
        selectedMode = index
        MmkvManager.encodeSettings(KEY_SELECTED_MODE, index)
    }

    fun choosePlan(id: String) {
        plan = id
        daysLeft = 30
        MmkvManager.encodeSettings(KEY_PLAN, id)
        MmkvManager.encodeSettings(KEY_DAYS_LEFT, 30)
    }

    sealed interface RedeemResult {
        data class Success(val planId: String, val days: Int) : RedeemResult
        data object Invalid : RedeemResult
        data object AlreadyUsed : RedeemResult
    }

    fun redeemCode(rawCode: String): RedeemResult {
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) return RedeemResult.Invalid
        val match = demoCodes[code] ?: return RedeemResult.Invalid
        if (redeemedCodes.contains(code)) return RedeemResult.AlreadyUsed

        redeemedCodes.add(code)
        MmkvManager.encodeSettings(KEY_REDEEMED_CODES, redeemedCodes)

        val (planId, days) = match
        val newDaysLeft = if (plan.isNotBlank()) daysLeft + days else days
        val newPlan = plan.ifBlank { planId }

        plan = newPlan
        daysLeft = newDaysLeft
        MmkvManager.encodeSettings(KEY_PLAN, newPlan)
        MmkvManager.encodeSettings(KEY_DAYS_LEFT, newDaysLeft)

        return RedeemResult.Success(planId, days)
    }
}

data class SkyPlanInfo(
    val id: String,
    val name: String,
    val price: String,
    val features: List<String>,
)

fun skyPlanInfo(id: String): SkyPlanInfo = when (id) {
    "premium" -> SkyPlanInfo(
        id = "premium",
        name = "Premium",
        price = "399 ₽/мес",
        features = listOf(
            "Всё из Plus",
            "5 устройств",
            "Ранний доступ к новым функциям",
            "Поддержка 24/7",
        )
    )

    else -> SkyPlanInfo(
        id = "plus",
        name = "Plus",
        price = "199 ₽/мес",
        features = listOf(
            "Оба режима без ограничений",
            "Скорость без лимита",
            "3 устройства",
            "Приоритетные серверы",
        )
    )
}
