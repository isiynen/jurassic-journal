package com.sufficienteffort.jurassicjournal.data.game.repository

import com.sufficienteffort.jurassicjournal.data.game.entity.Move
import com.sufficienteffort.jurassicjournal.data.model.MovePriorityType
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbilitySearchTest {

    private fun variant(
        priority: MovePriorityType = MovePriorityType.NORMAL,
        cooldown: Int = 0,
        vararg targets: Pair<String, List<String>>,
    ) = MoveVariant(priority, cooldown, 0, targets.map { ParsedTarget(it.first, it.second) })

    private fun move(slug: String, name: String, trigger: MoveTriggerType = MoveTriggerType.SELECTABLE) =
        Move(slug = slug, name = name, trigger = trigger, normalTargetsJson = "[]")

    private fun entries(
        slug: String,
        name: String,
        normal: MoveVariant,
        threatened: MoveVariant? = null,
        trigger: MoveTriggerType = MoveTriggerType.SELECTABLE,
    ) = AbilityEntries.fromMove(move(slug, name, trigger), normal, threatened)

    private val strike = entries(
        "strike", "Strike",
        variant(targets = arrayOf("Highest Damage" to listOf("Attack 1.5X ", "Bypass Armor"))),
    )
    private val shieldedStrike = entries(
        "shielded_strike", "Shielded Strike",
        variant(
            MovePriorityType.PRIORITY, 2,
            "Self" to listOf("Shield By 50.0% for 2 turn(s) , 2 attack(s) "),
            "Lowest HP" to listOf("Attack 1X "),
        ),
        threatened = variant(
            MovePriorityType.NORMAL, 1,
            "Lowest HP" to listOf("100.0% chance to Stun for 1 turn(s)", "Attack 3X "),
        ),
    )
    private val cleanse = entries(
        "cleanse", "Cleanse",
        variant(targets = arrayOf("Self" to listOf("Cleanse All Negative Effects", "Heal 25.0% of max HP"))),
        trigger = MoveTriggerType.REACTIVE,
    )
    private val all = strike + shieldedStrike + cleanse

    @Test fun `threatened variant becomes its own entry`() {
        assertEquals(listOf("Strike", "Shielded Strike", "Shielded Strike - Threatened", "Cleanse"), all.map { it.displayName })
    }

    @Test fun `actions are derived from effect text`() {
        assertEquals(setOf(AbilityAction.ATTACK, AbilityAction.BYPASS_ARMOR), strike[0].actions)
        assertTrue(AbilityAction.HEAL_PCT in cleanse[0].actions)
        assertFalse(AbilityAction.HEAL in cleanse[0].actions)
        assertTrue(AbilityAction.STUN in shieldedStrike[1].actions)
        assertFalse(AbilityAction.STUN in shieldedStrike[0].actions)
    }

    @Test fun `each variant filters independently`() {
        val priority = AbilitySearch.filter(all, AbilityFilterState(priorities = setOf(MovePriorityType.PRIORITY)))
        assertEquals(listOf("Shielded Strike"), priority.map { it.displayName })
        val normal = AbilitySearch.filter(all, AbilityFilterState(priorities = setOf(MovePriorityType.NORMAL)))
        assertTrue(normal.any { it.displayName == "Shielded Strike - Threatened" })
        assertFalse(normal.any { it.displayName == "Shielded Strike" })
    }

    @Test fun `trigger is OR, actions and targets are AND`() {
        val triggers = setOf(MoveTriggerType.REACTIVE, MoveTriggerType.ON_ESCAPE)
        assertEquals(listOf("Cleanse"), AbilitySearch.filter(all, AbilityFilterState(triggers = triggers)).map { it.name })

        val both = AbilitySearch.filter(all, AbilityFilterState(actions = setOf(AbilityAction.ATTACK, AbilityAction.BYPASS_ARMOR)))
        assertEquals(listOf("Strike"), both.map { it.displayName })

        val targets = AbilitySearch.filter(all, AbilityFilterState(targets = setOf("Self", "Lowest HP")))
        assertEquals(listOf("Shielded Strike"), targets.map { it.displayName })
    }

    @Test fun `query matches name and effects, quoted is a phrase`() {
        assertEquals(listOf("Cleanse"), AbilitySearch.filter(all, AbilityFilterState(query = "heal max")).map { it.name })
        assertEquals(listOf("Shielded Strike - Threatened"), AbilitySearch.filter(all, AbilityFilterState(query = "threatened")).map { it.displayName })
        assertEquals(0, AbilitySearch.filter(all, AbilityFilterState(query = "\"max heal\"")).size)
    }

    @Test fun `scope limits to slugs`() {
        val scoped = AbilitySearch.filter(all, AbilityFilterState(), scopeSlugs = setOf("strike"))
        assertEquals(listOf("Strike"), scoped.map { it.name })
    }

    @Test fun `default sort is by name with threatened after normal`() {
        val sorted = AbilitySearch.sort(all, null).filterIsInstance<AbilityListItem.Item>().map { it.entry.displayName }
        assertEquals(listOf("Cleanse", "Shielded Strike", "Shielded Strike - Threatened", "Strike"), sorted)
    }

    @Test fun `stat sort groups by value and drops valueless entries to the bottom`() {
        val items = AbilitySearch.sort(all, AbilitySortMode.ATTACK)
        val labels = items.map {
            when (it) {
                is AbilityListItem.Header -> "#${it.label}"
                is AbilityListItem.Item -> it.entry.displayName
            }
        }
        assertEquals(
            listOf(
                "#Attack 3X", "Shielded Strike - Threatened",
                "#Attack 1.5X", "Strike",
                "#Attack 1X", "Shielded Strike",
                "#No Attack", "Cleanse",
            ),
            labels,
        )
    }

    @Test fun `cooldown sorts ascending`() {
        val firstHeader = AbilitySearch.sort(all, AbilitySortMode.COOLDOWN).first() as AbilityListItem.Header
        assertEquals("Cooldown 0", firstHeader.label)
    }

    @Test fun `revenge effects are excluded from stats`() {
        val v = variant(targets = arrayOf("Self" to listOf("Attack 1X ", "On Revenge : Priority", "Attack 5X ")))
        assertEquals(1.0, AbilityEntries.computeStats(v)[AbilitySortMode.ATTACK]!!, 0.0)
    }

    @Test fun `dodge chance and amount are separate sort values`() {
        val v = variant(targets = arrayOf("Self" to listOf("75.0% chance to Dodge By 66.7% for 1 turn(s) , 1 attack(s) ")))
        val stats = AbilityEntries.computeStats(v)
        assertEquals(75.0, stats[AbilitySortMode.DODGE_CHANCE]!!, 0.0)
        assertEquals(66.7, stats[AbilitySortMode.DODGE_AMOUNT]!!, 0.0)
    }
}
