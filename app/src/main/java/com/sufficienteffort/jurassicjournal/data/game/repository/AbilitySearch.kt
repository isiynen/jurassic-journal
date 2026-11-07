package com.sufficienteffort.jurassicjournal.data.game.repository

import com.sufficienteffort.jurassicjournal.data.game.entity.Move
import com.sufficienteffort.jurassicjournal.data.model.MovePriorityType
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType
import java.util.Locale

// ── Action taxonomy (mirrors the ability dex's Action dropdown, minus Misc/Custom) ───

enum class ActionCategory(val label: String) {
    BASIC("Basic"),
    BYPASS("Bypass"),
    POSITIVE("Positive"),
    NULLIFY("Nullify"),
    NEGATIVE("Negative"),
    CLEANSE("Cleanse"),
}

/**
 * One selectable action chip. [matches] receives a lower-cased, trimmed effect line from
 * the ability data (e.g. "increase speed by 50.0% for 1 turn(s)").
 */
enum class AbilityAction(
    val category: ActionCategory,
    val label: String,
    val matches: (String) -> Boolean,
) {
    ATTACK(ActionCategory.BASIC, "Attack", { it.startsWith("attack ") }),
    HEAL(ActionCategory.BASIC, "Heal", { it.startsWith("heal ") && !it.contains("max hp") }),
    HEAL_PCT(ActionCategory.BASIC, "Heal (% Max HP)", { it.startsWith("heal ") && it.contains("max hp") }),
    REND(ActionCategory.BASIC, "Rend", { it.startsWith("rend ") }),
    AUTOMATIC_SWAP(ActionCategory.BASIC, "Automatic Swap", { it.startsWith("automatic swap") }),

    BYPASS_ARMOR(ActionCategory.BYPASS, "Bypass Armor", { it.startsWith("bypass armor") }),
    BYPASS_DODGE(ActionCategory.BYPASS, "Bypass Dodge", { it.startsWith("bypass dodge") }),
    BYPASS_ABSORB(ActionCategory.BYPASS, "Bypass Absorb", { it.startsWith("bypass absorb") }),

    SHIELD(ActionCategory.POSITIVE, "Shield", { it.startsWith("shield by") }),
    DODGE(ActionCategory.POSITIVE, "Dodge", { it.contains("chance to dodge") }),
    CLOAK(ActionCategory.POSITIVE, "Cloak", { it == "cloak" }),
    INCREASE_DAMAGE(ActionCategory.POSITIVE, "Increase Damage", { it.startsWith("increase damage") }),
    INCREASE_CRIT(ActionCategory.POSITIVE, "Increase Crit Chance", { it.startsWith("increase crit") }),
    INCREASE_SPEED(ActionCategory.POSITIVE, "Increase Speed", { it.startsWith("increase speed") }),
    INCREASE_ARMOR(ActionCategory.POSITIVE, "Increase Armor", { it.startsWith("increase armor") }),
    INCREASE_HEALING(ActionCategory.POSITIVE, "Increase Healing Received", { it.startsWith("increase healing received") }),
    TAUNT(ActionCategory.POSITIVE, "Taunt", { it.startsWith("taunt ") }),
    DEVOUR_HEAL(ActionCategory.POSITIVE, "Devour Heal", { it.startsWith("devour heal") }),
    CHEAT_DEATH(ActionCategory.POSITIVE, "Cheat Death", { it.startsWith("cheat death") }),

    NULLIFY_POSITIVE(ActionCategory.NULLIFY, "Nullify All Positive Effects", { it.startsWith("nullify all positive") }),
    BREAK_SHIELDS(ActionCategory.NULLIFY, "Break Shields", { it.startsWith("break shields") }),
    REMOVE_DODGE(ActionCategory.NULLIFY, "Remove Dodge", { it.startsWith("remove dodge") }),
    REMOVE_CLOAK(ActionCategory.NULLIFY, "Remove Cloak", { it.startsWith("remove cloak") }),
    REMOVE_DAMAGE(ActionCategory.NULLIFY, "Remove Increased Damage", { it.startsWith("remove increased damage") }),
    REMOVE_CRIT(ActionCategory.NULLIFY, "Remove Increased Crit Chance", { it.startsWith("remove increased crit") }),
    REMOVE_SPEED(ActionCategory.NULLIFY, "Remove Increased Speed", { it.startsWith("remove increased speed") }),
    REMOVE_ARMOR(ActionCategory.NULLIFY, "Remove Increased Armor", { it.startsWith("remove increased armor") }),
    REMOVE_TAUNT(ActionCategory.NULLIFY, "Remove Taunt", { it.startsWith("remove taunt") }),

    REDUCE_DAMAGE(ActionCategory.NEGATIVE, "Reduce Damage", { it.startsWith("reduce damage") }),
    REDUCE_CRIT(ActionCategory.NEGATIVE, "Reduce Crit Chance", { it.startsWith("reduce crit") }),
    REDUCE_SPEED(ActionCategory.NEGATIVE, "Reduce Speed", { it.startsWith("reduce speed") }),
    REDUCE_ARMOR(ActionCategory.NEGATIVE, "Reduce Armor", { it.startsWith("reduce armor") }),
    REDUCE_HEALING(ActionCategory.NEGATIVE, "Reduce Healing Received", { it.startsWith("reduce healing received") }),
    VULNERABILITY(ActionCategory.NEGATIVE, "Impose Vulnerability", { it.startsWith("impose vulnerability") }),
    UNABLE_TO_SWAP(ActionCategory.NEGATIVE, "Unable To Swap", { it.startsWith("unable to swap") }),
    DAMAGE_OVER_TIME(ActionCategory.NEGATIVE, "Damage Over Time", { it.startsWith("damage over time") }),
    STUN(ActionCategory.NEGATIVE, "Stun", { it.contains("chance to stun") }),
    AFFLICTION(ActionCategory.NEGATIVE, "Affliction", { it.startsWith("affliction") }),
    DAZE(ActionCategory.NEGATIVE, "Daze", { it.startsWith("daze") }),

    CLEANSE_ALL(ActionCategory.CLEANSE, "Cleanse All Negative Effects", { it.startsWith("cleanse all negative") }),
    CLEANSE_DAMAGE(ActionCategory.CLEANSE, "Cleanse Reduced Damage", { it.startsWith("cleanse reduced damage") }),
    CLEANSE_CRIT(ActionCategory.CLEANSE, "Cleanse Crit Chance Reduction", { it.startsWith("cleanse crit chance") }),
    CLEANSE_SPEED(ActionCategory.CLEANSE, "Cleanse Speed Decrease", { it.startsWith("cleanse speed") }),
    CLEANSE_ARMOR(ActionCategory.CLEANSE, "Cleanse Armor Reduction", { it.startsWith("cleanse armor") }),
    CLEANSE_VULNERABLE(ActionCategory.CLEANSE, "Cleanse Vulnerable", { it.startsWith("cleanse vulnerable") }),
    CLEANSE_SWAP(ActionCategory.CLEANSE, "Cleanse Swap Prevention", { it.startsWith("cleanse swap") }),
    CLEANSE_DOT(ActionCategory.CLEANSE, "Cleanse Damage Over Time", { it.startsWith("cleanse damage over time") });

    companion object {
        /** Chips for one category row, alphabetised by label. */
        fun inCategory(category: ActionCategory): List<AbilityAction> =
            entries.filter { it.category == category }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}

// ── Sort modes ────────────────────────────────────────────────────────────────

enum class ValueUnit { PERCENT, MULTIPLIER, TURNS }

/**
 * Numeric sort modes for abilities. [ascending] sorts low→high (cooldown/delay: fastest
 * first); everything else sorts high→low. [format] renders a value for the divider bar.
 */
enum class AbilitySortMode(
    val label: String,
    val ascending: Boolean = false,
    val unit: ValueUnit = ValueUnit.PERCENT,
) {
    COOLDOWN("Cooldown", ascending = true, unit = ValueUnit.TURNS),
    DELAY("Delay", ascending = true, unit = ValueUnit.TURNS),
    ATTACK("Attack", unit = ValueUnit.MULTIPLIER),
    HEAL("Heal", unit = ValueUnit.MULTIPLIER),
    HEAL_PCT("Heal %"),
    DAMAGE_UP("Damage Up"),
    DAMAGE_DOWN("Damage Down"),
    SHIELD("Shield"),
    DODGE_CHANCE("Dodge Chance"),
    DODGE_AMOUNT("Dodge Amount"),
    STUN("Stun"),
    SPEED_UP("Speed Up"),
    SPEED_DOWN("Speed Down"),
    CRIT_UP("Crit Up"),
    CRIT_DOWN("Crit Down"),
    ARMOR_UP("Armor Up"),
    ARMOR_DOWN("Armor Down"),
    DOT("DoT"),
    REND("Rend");

    fun format(value: Double): String {
        val n = trim(value)
        return when (unit) {
            ValueUnit.PERCENT -> "$label $n%"
            ValueUnit.MULTIPLIER -> "$label ${n}X"
            ValueUnit.TURNS -> "$label $n"
        }
    }

    private fun trim(v: Double): String =
        if (v == Math.floor(v)) v.toLong().toString()
        else String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
}

// ── Ability list entry (one per variant: normal, and threatened when present) ─────────

data class AbilityEntry(
    val slug: String,
    val name: String,
    val threatened: Boolean,
    val trigger: MoveTriggerType,
    val priority: MovePriorityType,
    val cooldown: Int,
    val delay: Int,
    val targets: List<ParsedTarget>,
    val mainIconPath: String?,
    val overlayIconsJson: String?,
    val actions: Set<AbilityAction>,
    val stats: Map<AbilitySortMode, Double>,
    val searchText: String,
) {
    val displayName: String get() = if (threatened) "$name - Threatened" else name
    val key: String get() = if (threatened) "$slug#threatened" else slug
    val targetNames: Set<String> get() = targets.map { it.target }.toSet()
}

object AbilityEntries {

    private val NUMBER = Regex("""[\d.]+""")
    private const val REVENGE_MARKER = "on revenge"

    /** Expands every [Move] into its normal entry plus a threatened entry when one exists. */
    fun fromMove(move: Move, normal: MoveVariant, threatened: MoveVariant?): List<AbilityEntry> =
        listOfNotNull(
            build(move, normal, threatened = false),
            threatened?.let { build(move, it, threatened = true) },
        )

    private fun build(move: Move, variant: MoveVariant, threatened: Boolean): AbilityEntry {
        val allEffects = variant.targets.flatMap { it.effects }.map { it.trim().lowercase() }
        val actions = AbilityAction.entries.filterTo(LinkedHashSet()) { action ->
            allEffects.any { action.matches(it) }
        }
        val name = move.name
        val search = buildString {
            append(name.lowercase())
            if (threatened) append(" threatened")
            variant.targets.forEach { t ->
                append(' ').append(t.target.lowercase())
                t.effects.forEach { append(' ').append(it.trim().lowercase()) }
            }
        }
        return AbilityEntry(
            slug = move.slug,
            name = name,
            threatened = threatened,
            trigger = move.trigger,
            priority = variant.priority,
            cooldown = variant.cooldown,
            delay = variant.delay,
            targets = variant.targets,
            mainIconPath = move.mainIconPath,
            overlayIconsJson = move.overlayIconsJson,
            actions = actions,
            stats = computeStats(variant),
            searchText = search,
        )
    }

    /** Numeric sort values. Effects after an "On Revenge" marker are conditional and excluded. */
    internal fun computeStats(variant: MoveVariant): Map<AbilitySortMode, Double> {
        val unconditional = variant.targets.flatMap { target ->
            val marker = target.effects.indexOfFirst { it.trim().lowercase().startsWith(REVENGE_MARKER) }
            (if (marker >= 0) target.effects.take(marker) else target.effects).map { it.trim().lowercase() }
        }
        val out = LinkedHashMap<AbilitySortMode, Double>()
        out[AbilitySortMode.COOLDOWN] = variant.cooldown.toDouble()
        out[AbilitySortMode.DELAY] = variant.delay.toDouble()

        fun sum(prefix: String, filter: (String) -> Boolean = { true }): Double? =
            unconditional.filter { it.startsWith(prefix) && filter(it) }
                .mapNotNull { firstNumber(it) }.takeIf { it.isNotEmpty() }?.sum()

        fun max(predicate: (String) -> Boolean): Double? =
            unconditional.filter(predicate).mapNotNull { firstNumber(it) }.maxOrNull()

        sum("attack ")?.let { out[AbilitySortMode.ATTACK] = it }
        sum("heal ") { !it.contains("max hp") }?.let { out[AbilitySortMode.HEAL] = it }
        sum("heal ") { it.contains("max hp") }?.let { out[AbilitySortMode.HEAL_PCT] = it }
        max { it.startsWith("increase damage") }?.let { out[AbilitySortMode.DAMAGE_UP] = it }
        max { it.startsWith("reduce damage") }?.let { out[AbilitySortMode.DAMAGE_DOWN] = it }
        max { it.startsWith("shield by") }?.let { out[AbilitySortMode.SHIELD] = it }
        // "75% chance to Dodge By 67% ..." — first number is the chance, second the amount dodged.
        val dodges = unconditional.filter { it.contains("chance to dodge") }.map { numbers(it) }
        dodges.mapNotNull { it.getOrNull(0) }.maxOrNull()?.let { out[AbilitySortMode.DODGE_CHANCE] = it }
        dodges.mapNotNull { it.getOrNull(1) }.maxOrNull()?.let { out[AbilitySortMode.DODGE_AMOUNT] = it }
        max { it.contains("chance to stun") }?.let { out[AbilitySortMode.STUN] = it }
        max { it.startsWith("increase speed") }?.let { out[AbilitySortMode.SPEED_UP] = it }
        max { it.startsWith("reduce speed") }?.let { out[AbilitySortMode.SPEED_DOWN] = it }
        max { it.startsWith("increase crit") }?.let { out[AbilitySortMode.CRIT_UP] = it }
        max { it.startsWith("reduce crit") }?.let { out[AbilitySortMode.CRIT_DOWN] = it }
        max { it.startsWith("increase armor") }?.let { out[AbilitySortMode.ARMOR_UP] = it }
        max { it.startsWith("reduce armor") }?.let { out[AbilitySortMode.ARMOR_DOWN] = it }
        max { it.startsWith("damage over time") }?.let { out[AbilitySortMode.DOT] = it }
        max { it.startsWith("rend ") }?.let { out[AbilitySortMode.REND] = it }
        return out
    }

    private fun firstNumber(effect: String): Double? = NUMBER.find(effect)?.value?.toDoubleOrNull()

    private fun numbers(effect: String): List<Double> =
        NUMBER.findAll(effect).mapNotNull { it.value.toDoubleOrNull() }.toList()
}

// ── Filtering and sorting ─────────────────────────────────────────────────────────

data class AbilityFilterState(
    val query: String = "",
    val triggers: Set<MoveTriggerType> = emptySet(),
    val targets: Set<String> = emptySet(),
    val priorities: Set<MovePriorityType> = emptySet(),
    val actions: Set<AbilityAction> = emptySet(),
    val sortMode: AbilitySortMode? = null,
) {
    fun toggleTrigger(t: MoveTriggerType) = copy(triggers = triggers.toggled(t))
    fun toggleTarget(t: String) = copy(targets = targets.toggled(t))
    fun togglePriority(p: MovePriorityType) = copy(priorities = priorities.toggled(p))
    fun toggleAction(a: AbilityAction) = copy(actions = actions.toggled(a))

    private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
}

sealed class AbilityListItem {
    data class Header(val label: String) : AbilityListItem()
    data class Item(val entry: AbilityEntry) : AbilityListItem()
}

/**
 * Pure filtering/sorting. Selection semantics:
 *  • Trigger and Priority: OR (an ability has exactly one of each).
 *  • Target and Action: AND (an ability can have several; extra chips narrow the list).
 *  • Text query: every word (or the quoted phrase) must appear in the name, targets or effects.
 */
object AbilitySearch {

    fun filter(
        all: List<AbilityEntry>,
        filters: AbilityFilterState,
        scopeSlugs: Set<String>? = null,
    ): List<AbilityEntry> {
        var list = all
        if (scopeSlugs != null) list = list.filter { it.slug in scopeSlugs }
        if (filters.triggers.isNotEmpty()) list = list.filter { it.trigger in filters.triggers }
        if (filters.priorities.isNotEmpty()) list = list.filter { it.priority in filters.priorities }
        if (filters.targets.isNotEmpty()) list = list.filter { it.targetNames.containsAll(filters.targets) }
        if (filters.actions.isNotEmpty()) list = list.filter { it.actions.containsAll(filters.actions) }
        val q = filters.query.trim()
        if (q.isNotEmpty()) {
            val strict = q.length >= 2 &&
                ((q.startsWith('"') && q.endsWith('"')) || (q.startsWith('\'') && q.endsWith('\'')))
            list = if (strict) {
                val phrase = q.drop(1).dropLast(1).lowercase()
                list.filter { it.searchText.contains(phrase) }
            } else {
                val words = q.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
                list.filter { e -> words.all { e.searchText.contains(it) } }
            }
        }
        return list
    }

    private val byName = compareBy<AbilityEntry>({ it.name.lowercase() }, { it.threatened })

    /** Name order by default; a stat sort groups under divider bars, valueless entries last. */
    fun sort(entries: List<AbilityEntry>, mode: AbilitySortMode?): List<AbilityListItem> {
        if (mode == null) return entries.sortedWith(byName).map { AbilityListItem.Item(it) }

        val (withValue, without) = entries.partition { mode in it.stats }
        val ordered = withValue.sortedWith(
            (if (mode.ascending) compareBy<AbilityEntry> { it.stats.getValue(mode) }
            else compareByDescending { it.stats.getValue(mode) }).then(byName)
        )
        val items = ArrayList<AbilityListItem>(entries.size + 16)
        var header: String? = null
        for (e in ordered) {
            val h = mode.format(e.stats.getValue(mode))
            if (h != header) {
                header = h
                items += AbilityListItem.Header(h)
            }
            items += AbilityListItem.Item(e)
        }
        if (without.isNotEmpty()) {
            items += AbilityListItem.Header("No ${mode.label}")
            without.sortedWith(byName).forEach { items += AbilityListItem.Item(it) }
        }
        return items
    }
}
