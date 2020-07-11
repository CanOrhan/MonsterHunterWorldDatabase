package com.gatheringhallstudios.mhworlddatabase.features.weapons.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.assets.AssetLoader
import com.gatheringhallstudios.mhworlddatabase.components.CheckableNotifier
import com.gatheringhallstudios.mhworlddatabase.components.CheckedImageButton
import com.gatheringhallstudios.mhworlddatabase.data.types.*
import com.gatheringhallstudios.mhworlddatabase.util.applyArguments
import kotlinx.android.synthetic.main.fragment_equipment_filter.*
import kotlinx.android.synthetic.main.fragment_weapon_filter_body.*

/**
 * Helper class to manage a collection of checkables, including updating and receiving
 * the selected value.
 */
class CheckedGroup<T>(val singleOnly: Boolean = false) {
    private val map = mutableMapOf<Checkable, T>()

    /**
     * Returns a read only map containing all binded views.
     */
    val views: Map<Checkable, T> get() = map

    fun uncheckAll() {
        for ((view, _) in map) {
            view.isChecked = false
        }
    }

    /**
     * Adds a binding to the list.
     * This version also updates the change listener.
     */
    fun addBinding(item: CheckableNotifier, value: T) {
        addBinding(item as Checkable, value)
        item.onCheckedChangeListener = ::notifyChanged
    }

    /**
     * Adds a binding to the list.
     * This version also updates the change listener.
     */
    fun addBinding(item: CompoundButton, value: T) {
        addBinding(item as Checkable, value)
        item.setOnCheckedChangeListener(::notifyChanged)
    }

    /**
     * Adds a binding to the list.
     * It is necessary to register the change event to notify the group with this version.
     */
    private fun addBinding(item: Checkable, value: T) {
        map[item] = value
    }

    /**
     * Notify that an item has changed. Required as the checkable interface
     * does not have an event register function.
     */
    fun notifyChanged(item: Checkable, isChecked: Boolean) {
        if (!isChecked || !singleOnly) {
            return
        }

        for (registered in map.keys) {
            if (registered != item && registered.isChecked) {
                registered.isChecked = false
            }
        }
    }

    /**
     * Returns the value of the checked item, or null if none are selected
     */
    fun getValue(): T? {
        for ((registered, value) in map) {
            if (registered.isChecked) {
                return value
            }
        }

        return null
    }

    /**
     * Returns the values of all checked items.
     */
    fun getValues(): List<T> {
        val results = mutableListOf<T>()
        for ((registered, value) in map) {
            if (registered.isChecked) {
                results.add(value)
            }
        }
        return results
    }

    /**
     * Updates all registered items to reflect the value (and only the value)
     */
    fun setValue(value: T?) {
        if (value == null) {
            uncheckAll()
        } else {
            setValues(listOf(value))
        }
    }

    /**
     * Updates the registered items to reflect the list of values
     */
    fun setValues(values: Iterable<T>) {
        val valuesTemp = mutableSetOf<T>()
        valuesTemp.addAll(values)

        for ((registered, registeredValue) in map) {
            registered.isChecked = (registeredValue in values)
        }
    }
}

/**
 * Main fragment that manages the weapon filter dialog.
 * Create a new object with setInstance, set the target fragment, and on an apply
 * it'll call back with a result.
 */
class WeaponFilterFragment : DialogFragment() {
    companion object {
        const val FILTER_WEAPON_TYPE = "FILTER_WEAPON_TYPE"
        const val FILTER_STATE = "FILTER_STATE"

        @JvmStatic fun newInstance(wtype: WeaponType, state: FilterState)
                = WeaponFilterFragment().applyArguments {
                    putSerializable(FILTER_WEAPON_TYPE, wtype)
                    putSerializable(FILTER_STATE, state)
                }
    }

    private lateinit var weaponType: WeaponType

    lateinit var elementGroup: CheckedGroup<ElementStatus>
    lateinit var phialGroupCB: CheckedGroup<PhialType>
    lateinit var phialGroupSWAXE: CheckedGroup<PhialType>
    lateinit var kinsectGroup: CheckedGroup<KinsectBonus>
    lateinit var shellingGroup: CheckedGroup<ShellingType>
    lateinit var shellingLevelGroup: CheckedGroup<Int>
    lateinit var coatingGroup: CheckedGroup<CoatingType>
    lateinit var specialAmmoGroup: CheckedGroup<SpecialAmmoType>
    lateinit var sortGroup: CheckedGroup<FilterSortCondition>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // makes the dialog into a full screen one
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_equipment_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scroll_body.layoutResource = R.layout.fragment_weapon_filter_body
        scroll_body.inflate()

        // NOTE FOR GROUPS: Only singleOnly groups need to be notified (to enable unselections)
        this.weaponType = arguments?.getSerializable(FILTER_WEAPON_TYPE) as WeaponType


        // define sort group
        sortGroup = CheckedGroup(singleOnly = true)
        sortGroup.addBinding(sort_attack_toggle, FilterSortCondition.ATTACK)
        sortGroup.addBinding(sort_affinity_toggle, FilterSortCondition.AFFINITY)
        sortGroup.addBinding(sort_element_toggle, FilterSortCondition.ELEMENT_STATUS)

        // define element group
        elementGroup = CheckedGroup()
        elementGroup.apply {
            addBinding(toggle_fire, ElementStatus.FIRE)
            addBinding(toggle_water, ElementStatus.WATER)
            addBinding(toggle_thunder, ElementStatus.THUNDER)
            addBinding(toggle_ice, ElementStatus.ICE)
            addBinding(toggle_dragon, ElementStatus.DRAGON)
            addBinding(toggle_poison, ElementStatus.POISON)
            addBinding(toggle_sleep, ElementStatus.SLEEP)
            addBinding(toggle_paralysis, ElementStatus.PARALYSIS)
            addBinding(toggle_blast, ElementStatus.BLAST)
            addBinding(toggle_non_elemental, ElementStatus.NON_ELEMENTAL)
        }

        // define phial group
        phialGroupCB = CheckedGroup()
        phialGroupCB.apply {
            addBinding(phial_toggle_impact, PhialType.IMPACT)
            addBinding(phial_toggle_power_element_cb, PhialType.POWER_ELEMENT)
        }
        
        phialGroupSWAXE = CheckedGroup()
        phialGroupSWAXE.apply {
            addBinding(phial_toggle_power, PhialType.POWER)
            addBinding(phial_toggle_power_element_swaxe, PhialType.POWER_ELEMENT)
            addBinding(phial_toggle_poison, PhialType.POISON)
            addBinding(phial_toggle_paralysis, PhialType.PARALYSIS)
            addBinding(phial_toggle_exhaust, PhialType.EXHAUST)
            addBinding(phial_toggle_dragon, PhialType.DRAGON)
        }

        kinsectGroup = CheckedGroup()
        kinsectGroup.apply {
            addBinding(kinsect_toggle_speed, KinsectBonus.SPEED)
            addBinding(kinsect_toggle_stamina, KinsectBonus.STAMINA)
            addBinding(kinsect_toggle_health, KinsectBonus.HEALTH)
            addBinding(kinsect_toggle_element, KinsectBonus.ELEMENT)
            addBinding(kinsect_toggle_sever, KinsectBonus.SEVER)
            addBinding(kinsect_toggle_blunt, KinsectBonus.BLUNT)
            addBinding(kinsect_toggle_spirit_strength, KinsectBonus.SPIRIT_STRENGTH)
            addBinding(kinsect_toggle_stamina_health, KinsectBonus.STAMINA_HEALTH)
        }

        shellingGroup = CheckedGroup()
        shellingGroup.apply {
            addBinding(shelling_toggle_normal, ShellingType.NORMAL)
            addBinding(shelling_toggle_long, ShellingType.LONG)
            addBinding(shelling_toggle_wide, ShellingType.WIDE)
        }

        shellingLevelGroup = CheckedGroup()
        shellingLevelGroup.apply {
            addBinding(shelling_toggle_level_1, 1)
            addBinding(shelling_toggle_level_2, 2)
            addBinding(shelling_toggle_level_3, 3)
            addBinding(shelling_toggle_level_4, 4)
            addBinding(shelling_toggle_level_5, 5)
            addBinding(shelling_toggle_level_6, 6)
            addBinding(shelling_toggle_level_7, 7)
        }

        coatingGroup = CheckedGroup()
        coatingGroup.apply {
            addBinding(coating_power, CoatingType.POWER)
            addBinding(coating_para, CoatingType.PARALYSIS)
            addBinding(coating_poison, CoatingType.POISON)
            addBinding(coating_sleep, CoatingType.SLEEP)
            addBinding(coating_blast, CoatingType.BLAST)
        }

        specialAmmoGroup = CheckedGroup(singleOnly = true)
        specialAmmoGroup.apply {
            addBinding(sammo_wyvernheart_toggle, SpecialAmmoType.WYVERNHEART)
            addBinding(sammo_wyvernsnipe_toggle, SpecialAmmoType.WYVERNSNIPE)
        }

        // Implement actions
        action_clear.setOnClickListener {
            applyState(FilterState.default)
        }
        action_cancel.setOnClickListener {
            dismiss()
        }
        action_apply.setOnClickListener {
            val data = Intent()
            data.putExtra(FILTER_STATE, calculateState())
            targetFragment?.onActivityResult(targetRequestCode, 0, data)
            dismiss()
        }

        // Enable visibility of elements based on weapon type
        element_toggles.isVisible = when (weaponType) {
            WeaponType.LIGHT_BOWGUN, WeaponType.HEAVY_BOWGUN -> false
            else -> true
        }

        phial_types_cb.isVisible = (weaponType == WeaponType.CHARGE_BLADE)
        phial_types_swaxe.isVisible = (weaponType == WeaponType.SWITCH_AXE)
        title_phials.isVisible = phial_types_cb.isVisible || phial_types_swaxe.isVisible

        title_kinsect.isVisible = (weaponType == WeaponType.INSECT_GLAIVE)
        kinsect_toggles.isVisible = (weaponType == WeaponType.INSECT_GLAIVE)

        title_shelling.isVisible = (weaponType == WeaponType.GUNLANCE)
        shelling_toggles.isVisible = (weaponType == WeaponType.GUNLANCE)

        title_coatings.isVisible = (weaponType == WeaponType.BOW)
        coating_toggles.isVisible = (weaponType == WeaponType.BOW)
        if (coating_toggles.isVisible) {
            for ((button, value) in coatingGroup.views) {
                val icon = AssetLoader.loadIconFor(value)
                (button as? CheckedImageButton)?.setImageDrawable(icon)
            }
        }

        title_ammo.isVisible = (weaponType == WeaponType.HEAVY_BOWGUN)
        special_ammo_toggles.isVisible = (weaponType == WeaponType.HEAVY_BOWGUN)

        // Apply and config state from bundle
        val state = arguments?.getSerializable(FILTER_STATE) as? FilterState
        if (state != null) {
            applyState(state)
        }
    }

    /**
     * Returns the current state, received by analyzing the current view state.
     */
    fun calculateState(): FilterState {
        val phials = when (weaponType) {
            WeaponType.CHARGE_BLADE -> phialGroupCB.getValues().toSet()
            WeaponType.SWITCH_AXE -> phialGroupSWAXE.getValues().toSet()
            else -> emptySet()
        }

        return FilterState(
                isFinalOnly = final_toggle.isChecked,
                sortBy = sortGroup.getValue() ?: FilterSortCondition.NONE,
                elements = elementGroup.getValues().toSet(),
                phials = phials,
                kinsectBonuses = kinsectGroup.getValues().toSet(),
                shellingTypes = shellingGroup.getValues().toSet(),
                shellingLevels = shellingLevelGroup.getValues().toSet(),
                coatingTypes = coatingGroup.getValues().toSet(),
                specialAmmo = specialAmmoGroup.getValue()
        )
    }

    /**
     * Applies a FilterState to the current UI.
     */
    fun applyState(state: FilterState) {
        // handle final
        final_toggle.isChecked = state.isFinalOnly

        // Set the basic group values
        sortGroup.setValue(state.sortBy)
        elementGroup.setValues(state.elements)
        phialGroupCB.setValues(state.phials)
        phialGroupSWAXE.setValues(state.phials)
        kinsectGroup.setValues(state.kinsectBonuses)
        shellingGroup.setValues(state.shellingTypes)
        shellingLevelGroup.setValues(state.shellingLevels)
        coatingGroup.setValues(state.coatingTypes)
        specialAmmoGroup.setValue(state.specialAmmo)
    }
}