package com.gatheringhallstudios.mhworlddatabase.data.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query

import com.gatheringhallstudios.mhworlddatabase.data.models.WeaponTree
import com.gatheringhallstudios.mhworlddatabase.data.types.WeaponType


@Dao
abstract class WeaponDao {

//    /** Loads all of the weapons in the trees for a provided weapon type] */
//    @Query("""
//        WITH RECURSIVE upgrades_from(id, name, lang_id, weapon_type, rarity, level, path) AS
//        (SELECT w.id, wt.name, wt.lang_id, w.weapon_type, w.rarity, 0,  CAST(w.id AS TEXT)
//            FROM weapon w
//                JOIN weapon_text wt USING (id)
//            WHERE wt.lang_id = :langId
//              AND (w.weapon_type = :weaponType)
//			  AND w.previous_weapon_id IS NULL
//            UNION ALL
//            SELECT w.id, wt.name, wt.lang_id, w.weapon_type, w.rarity, level + 1, path || ">" || CAST(w.id AS TEXT)
//            FROM weapon w
//                JOIN weapon_text wt USING (id)
//                INNER JOIN upgrades_from
//                    ON w.previous_weapon_id = upgrades_from.id
//			WHERE wt.lang_id = :langId
//              AND (w.weapon_type = :weaponType)
//        )
//        SELECT *
//        FROM upgrades_from
//        ORDER BY level ASC
//          """)
//    abstract fun loadWeaponTrees(langId: String, weaponType: WeaponType): List<UpgradesFrom>

    /** Loads all of the weapons in the trees for a provided weapon type] */
    @Query("""
        SELECT w.id, w.element1, w.element1_attack, w.rarity, w.previous_weapon_id, w.weapon_type, w.attack, w.defense,
        w.slot_1, w.slot_2, w.slot_3, w.sharpness, w.sharpness_maxed, w.element2, w.element2_attack, w.element_hidden, wt.*
        FROM weapon w
            JOIN weapon_text wt USING (id)
        WHERE wt.lang_id = :langId
            AND w.weapon_type = :weaponType
        ORDER BY w.id ASC
          """)
    abstract fun loadWeaponTrees(langId: String, weaponType: WeaponType): List<WeaponTree>
}
