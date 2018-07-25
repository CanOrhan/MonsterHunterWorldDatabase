package com.gatheringhallstudios.mhworlddatabase.data.models

import android.arch.persistence.room.Embedded

/**
 * The base charm class.
 * As charms don't have any special data outside of providing an object to join to,
 * the base is the same as the normal load.
 */
open class Charm(
        val id: Int,
        val name: String?,
        val rarity: Int,
        val previous_id: Int?
)

/**
 * Contains the charm and any additional join data related to the charm
 */
class CharmFull(
        val charm: Charm,
        val skills: List<SkillLevel>,
        val components: List<ItemQuantity>
)
