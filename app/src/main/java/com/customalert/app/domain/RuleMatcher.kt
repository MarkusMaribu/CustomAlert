package com.customalert.app.domain

import com.customalert.app.data.AppMapping
import com.customalert.app.data.MatchField
import com.customalert.app.data.Rule
import com.customalert.app.data.RuleScope

data class NotificationContent(
    val packageName: String,
    val title: String,
    val text: String
)

data class MatchResult(
    val soundId: String,
    val rule: Rule?,
    val fromAppDefault: Boolean = false
)

object RuleMatcher {
    fun resolve(
        content: NotificationContent,
        candidates: List<Rule>,
        appMapping: AppMapping?
    ): MatchResult? {
        val appRules = candidates
            .filter { it.scope == RuleScope.APP && it.packageName == content.packageName && it.enabled }
            .sortedBy { it.priority }
        for (rule in appRules) {
            if (matches(rule, content)) {
                return MatchResult(soundId = rule.soundId, rule = rule)
            }
        }

        val globalRules = candidates
            .filter { it.scope == RuleScope.GLOBAL && it.enabled }
            .sortedBy { it.priority }
        for (rule in globalRules) {
            if (matches(rule, content)) {
                return MatchResult(soundId = rule.soundId, rule = rule)
            }
        }

        if (appMapping != null && appMapping.enabled && !appMapping.defaultSoundId.isNullOrBlank()) {
            return MatchResult(
                soundId = appMapping.defaultSoundId,
                rule = null,
                fromAppDefault = true
            )
        }

        return null
    }

    fun matches(rule: Rule, content: NotificationContent): Boolean {
        val pattern = rule.pattern.trim()
        if (pattern.isEmpty()) return false
        val needle = pattern.lowercase()
        val title = content.title.lowercase()
        val text = content.text.lowercase()
        return when (rule.matchField) {
            MatchField.TITLE -> title.contains(needle)
            MatchField.TEXT -> text.contains(needle)
            MatchField.BOTH -> title.contains(needle) || text.contains(needle)
        }
    }
}
