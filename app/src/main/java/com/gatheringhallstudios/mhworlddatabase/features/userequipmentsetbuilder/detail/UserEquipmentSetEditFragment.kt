package com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.detail

import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.components.ExpandableCardView
import com.gatheringhallstudios.mhworlddatabase.data.models.*
import com.gatheringhallstudios.mhworlddatabase.data.types.ArmorType
import com.gatheringhallstudios.mhworlddatabase.data.types.DataType
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.UserEquipmentCard
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.list.UserEquipmentSetListViewModel
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.selectors.UserEquipmentSetSelectorListFragment.Companion
import com.gatheringhallstudios.mhworlddatabase.getRouter
import com.gatheringhallstudios.mhworlddatabase.setActivityTitle
import kotlinx.android.synthetic.main.fragment_user_equipment_set_editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserEquipmentSetEditFragment : androidx.fragment.app.Fragment(), RenameSetDialog.RenameDialogListener {
    fun showNoticeDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = RenameSetDialog()
        dialog.setTargetFragment(this, 0)
        dialog.show(fragmentManager!!, "RenameDialogFragment")
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: RenameSetDialog) {
        // User touched the dialog's positive button
        viewModel.renameEquipmentSet(dialog.resultName, viewModel.activeUserEquipmentSet.value!!.id)
        val buffer = ViewModelProviders.of(activity!!).get(UserEquipmentSetListViewModel::class.java)
        viewModel.activeUserEquipmentSet.value = buffer.getEquipmentSet(viewModel.activeUserEquipmentSet.value!!.id)
    }

    override fun onDialogNegativeClick(dialog: RenameSetDialog) {
        // User touched the dialog's negative button
    }

    private var isNewFragment = true

    /**
     * Returns the viewmodel owned by the parent fragment
     */
    private val viewModel: UserEquipmentSetDetailViewModel by lazy {
        ViewModelProviders.of(parentFragment!!).get(UserEquipmentSetDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_equipment_set_editor, parent, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.activeUserEquipmentSet.observe(this, Observer<UserEquipmentSet> {
            populateUserEquipment(it)
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isNewFragment) {
            val buffer = ViewModelProviders.of(activity!!).get(UserEquipmentSetListViewModel::class.java)
            viewModel.activeUserEquipmentSet.value = buffer.getEquipmentSet(viewModel.activeUserEquipmentSet.value!!.id)
        }

        isNewFragment = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_user_equipment_set_editor, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        super.onOptionsItemSelected(item)
        return when (id) {
            R.id.action_delete_set -> {
                viewModel.deleteEquipmentSet(viewModel.activeUserEquipmentSet.value!!.id)
                getRouter().goBack()
                true
            }
            R.id.action_rename_set -> {
                showNoticeDialog()
                true
            }
            else -> false
        }
    }

    private fun populateUserEquipment(userEquipmentSet: UserEquipmentSet) {
        setActivityTitle(userEquipmentSet.name)
        populateDefaults(userEquipmentSet.id)
        userEquipmentSet.equipment.forEach {
            when (it.type()) {
                DataType.WEAPON -> {
                    populateWeapon(it as UserWeapon, userEquipmentSet.id)
                }
                DataType.ARMOR -> {
                    populateArmor(it as UserArmorPiece, userEquipmentSet.id)
                }
                DataType.CHARM -> {
                    populateCharm(it as UserCharm, userEquipmentSet.id)
                }
                else -> {
                } //Skip
            }
        }
    }

    private fun populateArmor(userArmor: UserArmorPiece, userEquipmentSetId: Int) {
        val armor = userArmor.armor
        val layout: View
        when (armor.armor.armor_type) {
            ArmorType.HEAD -> {
                layout = user_equipment_head_slot
            }
            ArmorType.CHEST -> {
                layout = user_equipment_chest_slot
            }
            ArmorType.ARMS -> {
                layout = user_equipment_arms_slot
            }
            ArmorType.WAIST -> {
                layout = user_equipment_waist_slot
            }
            ArmorType.LEGS -> {
                layout = user_equipment_legs_slot
            }
        }

        val card = UserEquipmentCard(layout)
        card.bindArmor(userArmor)

        //Combine the skills from the armor piece and the decorations
        val skillsList = combineEquipmentSkillsWithDecorationSkills(armor.skills, userArmor.decorations.map {
            val skillLevel = SkillLevel(level = 1)
            skillLevel.skillTree = it.decoration.skillTree
            skillLevel
        })

        card.populateSkills(skillsList)
        card.populateSetBonuses(armor.setBonuses)
        populateDecorations(userArmor, userEquipmentSetId, layout)
        attachArmorOnClickListeners(userArmor, userEquipmentSetId, layout)
    }

    private fun populateCharm(userCharm: UserCharm, userEquipmentSetId: Int) {
        val card = UserEquipmentCard(user_equipment_charm_slot)
        card.bindCharm(userCharm)
        card.populateSkills(userCharm.charm.skills)
        card.populateSetBonuses(emptyList())
        populateDecorations(null, userEquipmentSetId, user_equipment_charm_slot)

        user_equipment_charm_slot.setOnClick {
            viewModel.setActiveUserEquipment(userCharm)
            getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.CHARM, userCharm, userEquipmentSetId, null, null)
        }
        user_equipment_charm_slot.setOnSwipeRight {
            viewModel.activeUserEquipmentSet.value?.equipment?.remove(userCharm)
            viewModel.deleteUserEquipment(userCharm.entityId(), userEquipmentSetId, userCharm.type())
            val currentFragment = this
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.detach(currentFragment)
            fragmentTransaction.attach(currentFragment)
            fragmentTransaction.commit()
        }
    }

    private fun populateWeapon(userWeapon: UserWeapon, userEquipmentSetId: Int) {
        val card = UserEquipmentCard(user_equipment_weapon_slot)
        card.bindWeapon(userWeapon)

        card.populateSkills(emptyList())
        card.populateSetBonuses(emptyList())
        populateDecorations(null, userEquipmentSetId, user_equipment_weapon_slot)

        val skillsList = combineEquipmentSkillsWithDecorationSkills(userWeapon.weapon.skills, userWeapon.decorations.map {
            val skillLevel = SkillLevel(level = 1)
            skillLevel.skillTree = it.decoration.skillTree
            skillLevel
        })

        card.populateSkills(skillsList)
        populateDecorations(userWeapon, userEquipmentSetId, user_equipment_weapon_slot)
        attachWeaponOnClickListeners(userWeapon, userEquipmentSetId, user_equipment_weapon_slot)
    }

    private fun populateDecorations(userEquipment: UserEquipment?, userEquipmentSetId: Int, layout: View) {
        val slots = if ((userEquipment as? UserArmorPiece) != null) {
            userEquipment.armor.armor.slots
        } else if ((userEquipment as? UserWeapon) != null) {
            userEquipment.weapon.weapon.slots
        } else {
            return
        }

        val decorations = if ((userEquipment as? UserArmorPiece) != null) {
            userEquipment.decorations
        } else if ((userEquipment as? UserWeapon) != null) {
            userEquipment.decorations
        } else {
            emptyList()
        }

        val card = UserEquipmentCard(layout as ExpandableCardView)
        card.populateDecorations(slots, decorations,
                onEmptyClick = { slotNumber ->
                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.DECORATION, null,
                            userEquipmentSetId, null,
                            Companion.DecorationsConfig(userEquipment.entityId(), slotNumber, userEquipment.type(), slots[slotNumber - 1]))
                },
                onClick = { slotNumber, userDecoration ->
                    viewModel.setActiveUserEquipment(userDecoration)
                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.DECORATION,
                            userDecoration, userEquipmentSetId, null,
                            Companion.DecorationsConfig(
                                    userEquipment.entityId(), userDecoration.slotNumber,
                                    userEquipment.type(), slots[slotNumber - 1]))

                },
                onDelete = { userDecoration ->
                    GlobalScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            viewModel.deleteDecorationForEquipment(userDecoration.decoration.id, userEquipment.entityId(), userDecoration.slotNumber, userEquipment.type(), userEquipmentSetId)
                        }
                        withContext(Dispatchers.Main) {
                            val buffer = ViewModelProviders.of(activity!!).get(UserEquipmentSetListViewModel::class.java)
                            viewModel.activeUserEquipmentSet.value = buffer.getEquipmentSet(viewModel.activeUserEquipmentSet.value!!.id)
                        }
                    }
                }
        )
    }

    private fun populateDefaults(userEquipmentSetId: Int) {
        with(UserEquipmentCard(user_equipment_weapon_slot)) {
            bindEmptyWeapon()
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.WEAPON, null, userEquipmentSetId, null, null)
            }
        }

        with(UserEquipmentCard(user_equipment_head_slot)) {
            bindEmptyArmor(ArmorType.HEAD)
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, null, userEquipmentSetId, ArmorType.HEAD, null)
            }
        }

        with(UserEquipmentCard(user_equipment_chest_slot)) {
            bindEmptyArmor(ArmorType.CHEST)
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, null, userEquipmentSetId, ArmorType.CHEST, null)
            }
        }

        with(UserEquipmentCard(user_equipment_arms_slot)) {
            bindEmptyArmor(ArmorType.ARMS)
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, null, userEquipmentSetId, ArmorType.ARMS, null)
            }
        }

        with(UserEquipmentCard(user_equipment_waist_slot)) {
            bindEmptyArmor(ArmorType.WAIST)
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, null, userEquipmentSetId, ArmorType.WAIST, null)
            }
        }

        with(UserEquipmentCard(user_equipment_legs_slot)) {
            bindEmptyArmor(ArmorType.LEGS)
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, null, userEquipmentSetId, ArmorType.LEGS, null)
            }
        }

        with(UserEquipmentCard(user_equipment_charm_slot)) {
            bindEmptyCharm()
            setOnClick {
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.CHARM, null, userEquipmentSetId, null, null)
            }
        }
    }

    private fun attachArmorOnClickListeners(armorPiece: UserArmorPiece, userEquipmentSetId: Int, layout: ExpandableCardView) {
        val armor = armorPiece.armor.armor
        layout.setOnClick {
            viewModel.setActiveUserEquipment(armorPiece)
            getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, armorPiece, userEquipmentSetId, armor.armor_type, null)
        }
        layout.setOnSwipeRight {
            viewModel.activeUserEquipmentSet.value?.equipment?.remove(armorPiece)
            viewModel.deleteUserEquipment(armorPiece.entityId(), userEquipmentSetId, armorPiece.type())
            val currentFragment = this
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.detach(currentFragment)
            fragmentTransaction.attach(currentFragment)
            fragmentTransaction.commit()
        }
    }

    private fun attachWeaponOnClickListeners(userWeapon: UserWeapon, userEquipmentSetId: Int, layout: ExpandableCardView) {
        layout.setOnClick {
            viewModel.setActiveUserEquipment(userWeapon)
            getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.WEAPON, userWeapon, userEquipmentSetId, null, null)
        }
        layout.setOnSwipeRight {
            viewModel.activeUserEquipmentSet.value?.equipment?.remove(userWeapon)
            viewModel.deleteUserEquipment(userWeapon.entityId(), userEquipmentSetId, userWeapon.type())
            val currentFragment = this
            val fragmentTransaction = fragmentManager!!.beginTransaction()
            fragmentTransaction.detach(currentFragment)
            fragmentTransaction.attach(currentFragment)
            fragmentTransaction.commit()
        }
    }

    private fun combineEquipmentSkillsWithDecorationSkills(armorSkills: List<SkillLevel>, decorationSkills: List<SkillLevel>): List<SkillLevel> {
        val skills = armorSkills.associateBy({ it.skillTree.id }, { it }).toMutableMap()
        for (skill in decorationSkills) {
            if (skills.containsKey(skill.skillTree.id)) {
                val level = skills.getValue(skill.skillTree.id).level + skill.level
                val skillLevel = SkillLevel(level)
                skillLevel.skillTree = skill.skillTree
                skills[skill.skillTree.id] = skillLevel
            } else {
                skills[skill.skillTree.id] = skill
            }
        }
        val result = skills.values.toMutableList()
        result.sortWith(compareByDescending<SkillLevel> { it.level }.thenBy { it.skillTree.id })
        return result
    }
}