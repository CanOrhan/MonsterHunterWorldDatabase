package com.gatheringhallstudios.mhworlddatabase.data.dao

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.gatheringhallstudios.mhworlddatabase.data.types.Rank
import com.gatheringhallstudios.mhworlddatabase.data.models.*
import com.gatheringhallstudios.mhworlddatabase.util.createLiveData

/**
 * Created by Carlos on 3/21/2018.
 */
@Dao
abstract class ArmorDao {
    @Query("""
        SELECT a.*, at.name, ast.name armorset_name
        FROM armor a
            JOIN armor_text at USING (id)
            JOIN armorset_text ast
                ON ast.id = a.armorset_id
                AND ast.lang_id = at.lang_id
        WHERE at.lang_id = :langId
          AND (:rank IS NULL OR a.rank = :rank)
    """)
    abstract fun loadArmorList(langId: String, rank: Rank?): LiveData<List<Armor>>

    @Query("""
        SELECT a.*, at.name, ast.name armorset_name
        FROM armor a
            JOIN armor_text at USING (id)
            JOIN armorset_text ast
                ON ast.id = a.armorset_id
                AND ast.lang_id = at.lang_id
        WHERE at.lang_id = :langId
        AND a.id = :armorId""")
    abstract fun loadArmorSync(langId: String, armorId: Int): Armor


    fun loadArmor(langId: String, armorId: Int) = createLiveData {
        loadArmorSync(langId, armorId)
    }

    /**
     * Generates a list of ArmorSets with embedded ArmorViews
     * Equivalent to loadArmorList and then grouping
     */
    fun loadArmorSets(langId: String, rank: Rank?): LiveData<List<ArmorSet>> {
        // Load raw view of Armor with Armor Set info
        val armorSets = loadArmorList(langId, rank)

        return Transformations.map(armorSets) { data ->
            // Create a map of armorset_id -> Armor
            val setToArmorMap = data.groupBy { it.armorset_id }

            val armorSetList = mutableListOf<ArmorSet>()

            for (armorGroup in setToArmorMap) {
                val armorSetId = armorGroup.value.first().armorset_id
                val armorSetName = armorGroup.value.first().armorset_name
                val armorList = armorGroup.value

                armorSetList.add(ArmorSet(armorSetId, armorSetName, armorList))
            }

            armorSetList
        }
    }

    /**
     * Loads full information for a piece of armor as a composite object
     */
    fun loadArmorFull(langId: String, armorId: Int) = createLiveData {
        val armor = loadArmorSync(langId, armorId)
        ArmorFull(
                armor = armor,
                recipe = loadArmorComponentsSync(langId, armorId),
                skills = loadArmorSkillsSync(langId, armorId),
                setBonuses = when (armor.armorset_bonus_id) {
                    null -> emptyList()
                    else -> loadArmorSetBonusSync(langId, armor.armorset_bonus_id)
                })
    }

    @Query("""
        SELECT st.id as skilltree_id, stt.name as skilltree_name,
            stt.description as skilltree_description, st.icon_color as skilltree_icon_color,
            abs.setbonus_id as id, abt.name, abs.required
        FROM armorset_bonus_skill abs
            JOIN armorset_bonus_text abt
                ON abt.id = abs.setbonus_id
            JOIN skilltree st
                ON abs.skilltree_id = st.id
            JOIN skilltree_text stt
                ON abs.skilltree_id = stt.id
                AND abt.lang_id = stt.lang_id
        WHERE abt.lang_id = :langId
        AND abs.setbonus_id= :setBonusId""")
    abstract fun loadArmorSetBonusSync(langId: String, setBonusId: Int) : List<ArmorSetBonus>

    @Query("""
        SELECT i.id item_id, it.name item_name, i.icon_name item_icon_name,
            i.category item_category, i.icon_color item_icon_color, a.quantity
         FROM armor_recipe a
            JOIN item i
                ON a.item_id = i.id
            JOIN item_text it
                ON it.id = i.id
                AND it.lang_id = :langId
        WHERE it.lang_id = :langId
        AND a.armor_id= :armorId
        ORDER BY i.id
    """)
    abstract fun loadArmorComponentsSync(langId: String, armorId: Int) : List<ItemQuantity>

    @Query("""
        SELECT s.id skill_id, stt.name skill_name, s.icon_color skill_color, askill.level level
        FROM armor_skill askill
            JOIN armor a
                ON askill.armor_id = a.id
            JOIN skilltree s
                ON askill.skilltree_id = s.id
            JOIN skilltree_text stt
                ON askill.skilltree_id = stt.id
            WHERE stt.lang_id = :langId
               AND askill.armor_id = :armorId
            ORDER BY askill.skilltree_id ASC""")
    abstract fun loadArmorSkillsSync(langId: String, armorId: Int) : List<SkillLevel>
}
