package com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.selectors

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gatheringhallstudios.mhworlddatabase.AppSettings
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.components.SpacesItemDecoration
import com.gatheringhallstudios.mhworlddatabase.data.models.*
import com.gatheringhallstudios.mhworlddatabase.data.types.ArmorType
import com.gatheringhallstudios.mhworlddatabase.data.types.DataType
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.UserEquipmentCard
import com.gatheringhallstudios.mhworlddatabase.features.weapons.list.WeaponTreePagerFragment.Companion.FILTER_RESULT_CODE
import com.gatheringhallstudios.mhworlddatabase.getRouter
import com.gatheringhallstudios.mhworlddatabase.setActivityTitle
import kotlinx.android.synthetic.main.fragment_user_equipment_set_selector.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class UserEquipmentSetSelectorListFragment : Fragment() {
    companion object {
        const val ARG_ACTIVE_EQUIPMENT = "ACTIVE_EQUIPMENT"
        const val ARG_SET_ID = "ACTIVE_SET_ID" //The equipment set that is currently being handled when in builder mode
        const val ARG_ARMOR_FILTER = "ACTIVE_ARMOR_FILTER" //What class armor to limit the selector to
        const val ARG_SELECTOR_MODE = "SELECTOR_MODE"
        const val ARG_DECORATION_CONFIG = "DECORATION_CONFIG"

        enum class SelectorMode {
            ARMOR,
            DECORATION,
            CHARM,
            WEAPON,
            NONE
        }

        class DecorationsConfig(val targetEquipmentId: Int, val targetEquipmentSlot: Int,
                                val targetEquipmentType: DataType, val decorationLevelFilter: Int) : Serializable
    }

    private val viewModel: UserEquipmentSetSelectorViewModel by lazy {
        ViewModelProviders.of(this).get(UserEquipmentSetSelectorViewModel::class.java)
    }

    private lateinit var card: UserEquipmentCard
    private var mode: SelectorMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_search).isVisible = false
        inflater.inflate(R.menu.menu_weapon_tree, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val filterIcon = menu.findItem(R.id.action_filter)
        viewModel.isFilterActive.observe(this, Observer { isFiltered ->
            filterIcon?.setIcon(when (isFiltered) {
                true -> R.drawable.ic_sys_filter_on
                false -> R.drawable.ic_sys_filter_off
            })
        })
    }

    /**
     * Handled when a menu item is clicked. True is returned if handled.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                val state = viewModel.filterState
                val filterFragment = EquipmentFilterFragment.newInstance(this.mode!!, state)
                filterFragment.setTargetFragment(this, FILTER_RESULT_CODE)
                filterFragment.show(fragmentManager!!, "Filter")
                true
            }

            // fallback to parent behavior if unhandled
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_equipment_set_selector, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.mode = arguments?.getSerializable(ARG_SELECTOR_MODE) as? SelectorMode
        val filter = arguments?.getSerializable(ARG_ARMOR_FILTER) as? ArmorType
        val activeEquipment = arguments?.getSerializable(ARG_ACTIVE_EQUIPMENT) as? UserEquipment
        val activeEquipmentSetId = arguments?.getInt(ARG_SET_ID)
        val decorationsConfig = arguments?.getSerializable(ARG_DECORATION_CONFIG) as? DecorationsConfig

        card = UserEquipmentCard(active_equipment_slot)

        when (mode) {
            SelectorMode.ARMOR -> initArmorSelector(filter, activeEquipment as? UserArmorPiece, activeEquipmentSetId)
            SelectorMode.CHARM -> initCharmSelector(activeEquipment as? UserCharm, activeEquipmentSetId)
            SelectorMode.DECORATION -> initDecorationSelector(activeEquipment as? UserDecoration, activeEquipmentSetId, decorationsConfig!!)
            SelectorMode.WEAPON -> initWeaponSelector(activeEquipment as? UserWeapon, activeEquipmentSetId)
        }
    }

    override fun onPause() {
        super.onPause()
        val listState = equipment_list.layoutManager?.onSaveInstanceState()
        if (listState != null) {
            viewModel.listState = listState
        }
    }

    /**
     * Receives a dialog result. Currently the only supported dialog is the filter fragment.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILTER_RESULT_CODE) {
            return
        }

        val state = data?.getSerializableExtra(EquipmentFilterFragment.FILTER_STATE) as? EquipmentFilterState
        if (state != null) {
            viewModel.filterState = state
        }
    }

    private fun initArmorSelector(armorType: ArmorType?, activeArmorPiece: UserArmorPiece?, activeEquipmentSetId: Int?) {
        setActivityTitle(getString(R.string.title_armor_set_armor_selector))

        if (armorType != null) {
            viewModel.loadArmor(AppSettings.dataLocale, armorType)
        }

        val adapter = UserEquipmentSetArmorSelectorAdapter {
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    viewModel.updateEquipmentForEquipmentSet(it.entityId, it.entityType, activeEquipmentSetId!!, activeArmorPiece?.armor?.entityId)
                }

                getRouter().goBack()
            }
        }

        //If this is going to be new piece of armor, do not populate the active armor piece
        if (activeArmorPiece != null) {
            populateActiveArmor(activeArmorPiece)
        } else {
            card.bindEmptyArmor(armorType)
        }

        equipment_list.adapter = adapter
        equipment_list.addItemDecoration(SpacesItemDecoration(32))
        viewModel.armor.observe(this, Observer {
            adapter.items = it
            if (viewModel.islistStateInitialized()) {
                equipment_list.layoutManager?.onRestoreInstanceState(viewModel.listState)
            }

            if (it.isEmpty()) {
                empty_view.visibility = View.VISIBLE
            } else {
                empty_view.visibility = View.GONE
            }
        })
    }

    private fun initCharmSelector(activeCharm: UserCharm?, activeEquipmentSetId: Int?) {
        setActivityTitle(getString(R.string.title_armor_set_charm_selector))
        viewModel.loadCharms(AppSettings.dataLocale)

        val adapter = UserEquipmentSetCharmSelectorAdapter {
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    viewModel.updateEquipmentForEquipmentSet(it.entityId, it.entityType, activeEquipmentSetId!!, activeCharm?.entityId())
                }

                getRouter().goBack()
            }
        }

        if (activeCharm != null) {
            populateActiveCharm(activeCharm)
        } else {
            card.bindEmptyCharm()
        }

        equipment_list.adapter = adapter
        equipment_list.addItemDecoration(SpacesItemDecoration(32))

        viewModel.charms.observe(this, Observer {
            adapter.items = it
            if (viewModel.islistStateInitialized()) {
                equipment_list.layoutManager?.onRestoreInstanceState(viewModel.listState)
            }

            if (it.isEmpty()) {
                empty_view.visibility = View.VISIBLE
            } else {
                empty_view.visibility = View.GONE
            }
        })
    }

    private fun initDecorationSelector(activeDecoration: UserDecoration?, activeEquipmentSetId: Int?, decorationsConfig: DecorationsConfig) {
        setActivityTitle(getString(R.string.title_armor_set_decoration_selector))
        viewModel.loadDecorations(AppSettings.dataLocale)

        val adapter = UserEquipmentSetDecorationSelectorAdapter {
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    viewModel.updateDecorationForEquipmentSet(it.id, decorationsConfig.targetEquipmentId,
                            decorationsConfig.targetEquipmentSlot, decorationsConfig.targetEquipmentType, activeEquipmentSetId!!, activeDecoration?.entityId())
                }
                getRouter().goBack()
            }
        }

        if (activeDecoration != null) {
            populateActiveDecoration(activeDecoration)
        } else {
            card.bindEmptyDecoration(decorationsConfig.decorationLevelFilter)
        }

        equipment_list.adapter = adapter
        equipment_list.addItemDecoration(SpacesItemDecoration(32))

        viewModel.decorations.observe(this, Observer {
            val filteredCollection = it.filter { decoration ->
                decoration.slot <= decorationsConfig.decorationLevelFilter
            }
            if (viewModel.islistStateInitialized()) {
                equipment_list.layoutManager?.onRestoreInstanceState(viewModel.listState)
            }

            adapter.items = filteredCollection
            if (filteredCollection.isEmpty()) {
                empty_view.visibility = View.VISIBLE
            } else {
                empty_view.visibility = View.GONE
            }
        })
    }

    private fun initWeaponSelector(activeWeapon: UserWeapon?, activeEquipmentSetId: Int?) {
        setActivityTitle(getString(R.string.title_armor_set_weapon_selector))
        viewModel.loadWeapons(AppSettings.dataLocale)

        val adapter = UserEquipmentSetWeaponSelectorAdapter {
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    viewModel.updateEquipmentForEquipmentSet(it.entityId, it.entityType, activeEquipmentSetId!!, activeWeapon?.entityId())
                }
                getRouter().goBack()
            }
        }

        if (activeWeapon != null) {
            populateActiveWeapon(activeWeapon)
        } else {
            card.bindEmptyWeapon()
        }

        equipment_list.adapter = adapter
        equipment_list.addItemDecoration(SpacesItemDecoration(32))

        viewModel.weapons.observe(this, Observer {
            adapter.items = it
            if (viewModel.islistStateInitialized()) {
                equipment_list.layoutManager?.onRestoreInstanceState(viewModel.listState)
            }

            if (it.isEmpty()) {
                empty_view.visibility = View.VISIBLE
            }
        })
    }

    private fun populateActiveWeapon(userWeapon: UserWeapon) {
        card.bindWeapon(userWeapon)
        card.populateSkills(userWeapon.weapon.skills)
        card.populateSetBonuses(emptyList())
        card.populateSlots(userWeapon.weapon.weapon.slots)
    }

    private fun populateActiveArmor(userArmor: UserArmorPiece) {
        when (userArmor.armor.armor.armor_type) {
            ArmorType.HEAD -> card.bindHeadArmor(userArmor)
            ArmorType.ARMS -> card.bindArmArmor(userArmor)
            ArmorType.CHEST -> card.bindChestArmor(userArmor)
            ArmorType.WAIST -> card.bindWaistArmor(userArmor)
            ArmorType.LEGS -> card.bindLegArmor(userArmor)
        }

        card.populateSlots(userArmor.armor.armor.slots)
    }

    private fun populateActiveCharm(userCharm: UserCharm) {
        card.bindCharm(userCharm)
        card.populateSkills(userCharm.charm.skills)
        card.populateSetBonuses(emptyList())
    }

    private fun populateActiveDecoration(userDecoration: UserDecoration) {
        val skill = SkillLevel(level = 1)
        skill.skillTree = userDecoration.decoration.skillTree

        card.bindDecoration(userDecoration)
        card.populateSkills(listOf(skill))
        card.populateSetBonuses(emptyList())
    }
}