package com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.detail

import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.data.models.*
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.UserEquipmentCard
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.UserEquipmentSetViewModel
import com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.selectors.UserEquipmentSetSelectorListFragment.Companion
import com.gatheringhallstudios.mhworlddatabase.getRouter
import com.gatheringhallstudios.mhworlddatabase.setActivityTitle
import kotlinx.android.synthetic.main.fragment_user_equipment_set_editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class UserEquipmentSetEditFragment : androidx.fragment.app.Fragment(), RenameSetDialog.RenameDialogListener {
    private val viewModel by lazy {
        ViewModelProviders.of(activity!!).get(UserEquipmentSetViewModel::class.java)
    }

    private lateinit var weaponCard: UserEquipmentCard
    private lateinit var headArmorCard: UserEquipmentCard
    private lateinit var armArmorCard: UserEquipmentCard
    private lateinit var chestArmorCard: UserEquipmentCard
    private lateinit var waistArmorCard: UserEquipmentCard
    private lateinit var legArmorCard: UserEquipmentCard
    private lateinit var charmCard: UserEquipmentCard

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
    }

    override fun onDialogNegativeClick(dialog: RenameSetDialog) {
        // User touched the dialog's negative button
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_equipment_set_editor, parent, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.activeUserEquipmentSet.observe(viewLifecycleOwner, Observer<UserEquipmentSet> {
            populateUserEquipmentSet(it)
        })
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.weaponCard = UserEquipmentCard(user_equipment_weapon_slot)
        this.headArmorCard = UserEquipmentCard(user_equipment_head_slot)
        this.armArmorCard = UserEquipmentCard(user_equipment_arms_slot)
        this.chestArmorCard = UserEquipmentCard(user_equipment_chest_slot)
        this.waistArmorCard = UserEquipmentCard(user_equipment_waist_slot)
        this.legArmorCard = UserEquipmentCard(user_equipment_legs_slot)
        this.charmCard = UserEquipmentCard(user_equipment_charm_slot)
    }

    private fun populateUserEquipmentSet(userEquipmentSet: UserEquipmentSet) {
        setActivityTitle(userEquipmentSet.name)

        val weapon = userEquipmentSet.getWeapon()
        weaponCard.bindWeapon(weapon, userEquipmentSet.id,
                onClick = {
                    viewModel.setActiveUserEquipment(weapon)
                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.WEAPON, weapon,
                            userEquipmentSet.id, null, null)
                },
                onSwipeRight = {
                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(weapon!!)
                    viewModel.deleteUserEquipment(weapon!!.entityId(), userEquipmentSet.id, weapon.type())
                    refreshFragment()
                })
        if (weapon != null) {
            populateDecorations(weapon, userEquipmentSet.id, weaponCard)
        }


//        val headArmor = userEquipmentSet.getHeadArmor()
//        headArmorCard.bindHeadArmor(headArmor, userEquipmentSet.id,
//                onClick = {
//                    viewModel.setActiveUserEquipment(headArmor)
//                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, headArmor,
//                            userEquipmentSet.id, headArmor!!.armor.armor.armor_type, null)
//                },
//                onSwipeRight = {
//                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(headArmor!!)
//                    viewModel.deleteUserEquipment(headArmor!!.entityId(), userEquipmentSet.id, headArmor!!.type())
//                    refreshFragment()
//                })
//        if (headArmor != null) {
//            populateDecorations(headArmor, userEquipmentSet.id, headArmorCard)
//        }

//        val armArmor = userEquipmentSet.getArmArmor()
//        armArmorCard.bindArmArmor(armArmor, userEquipmentSet.id,
//                onClick = {
//                    viewModel.setActiveUserEquipment(armArmor)
//                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, armArmor,
//                            userEquipmentSet.id, armArmor!!.armor.armor.armor_type, null)
//                },
//                onSwipeRight = {
//                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(armArmor!!)
//                    viewModel.deleteUserEquipment(armArmor!!.entityId(), userEquipmentSet.id, armArmor.type())
//                    refreshFragment()
//                })
//        if (armArmor != null) {
//            populateDecorations(armArmor, userEquipmentSet.id, armArmorCard)
//        }
//
//        val chestArmor = userEquipmentSet.getChestArmor()
//        chestArmorCard.bindChestArmor(chestArmor, userEquipmentSet.id,
//                onClick = {
//                    viewModel.setActiveUserEquipment(chestArmor)
//                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, chestArmor,
//                            userEquipmentSet.id, chestArmor!!.armor.armor.armor_type, null)
//                },
//                onSwipeRight = {
//                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(chestArmor!!)
//                    viewModel.deleteUserEquipment(chestArmor!!.entityId(), userEquipmentSet.id, chestArmor.type())
//                    refreshFragment()
//                })
//        if (chestArmor != null) {
//            populateDecorations(chestArmor, userEquipmentSet.id, chestArmorCard)
//        }
//
//        val legArmor = userEquipmentSet.getLegArmor()
//        legArmorCard.bindLegArmor(legArmor, userEquipmentSet.id,
//                onClick = {
//                    viewModel.setActiveUserEquipment(legArmor)
//                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, legArmor,
//                            userEquipmentSet.id, legArmor!!.armor.armor.armor_type, null)
//                },
//                onSwipeRight = {
//                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(legArmor!!)
//                    viewModel.deleteUserEquipment(legArmor!!.entityId(), userEquipmentSet.id, legArmor.type())
//                    refreshFragment()
//                })
//        if (legArmor != null) {
//            populateDecorations(legArmor, userEquipmentSet.id, legArmorCard)
//        }
//
//        val waistArmor = userEquipmentSet.getWaistArmor()
//        waistArmorCard.bindWaistArmor(waistArmor, userEquipmentSet.id,
//                onClick = {
//                    viewModel.setActiveUserEquipment(waistArmor)
//                    getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, waistArmor,
//                            userEquipmentSet.id, waistArmor!!.armor.armor.armor_type, null)
//                },
//                onSwipeRight = {
//                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(waistArmor!!)
//                    viewModel.deleteUserEquipment(waistArmor!!.entityId(), userEquipmentSet.id, waistArmor.type())
//                    refreshFragment()
//                })
//        if (waistArmor != null) {
//            populateDecorations(waistArmor, userEquipmentSet.id, waistArmorCard)
//        }

//        val charm = userEquipmentSet.getCharm()
//        charmCard.bindCharm(charm, userEquipmentSet.id,
//                onClick = {
//                    user_equipment_charm_slot.setOnClick {
//                        viewModel.setActiveUserEquipment(charm)
//                        getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.CHARM, charm, userEquipmentSet.id,
//                                null, null)
//                    }
//                },
//                onSwipeRight = {
//                    viewModel.activeUserEquipmentSet.value?.equipment?.remove(charm!!)
//                    viewModel.deleteUserEquipment(charm!!.entityId(), userEquipmentSet.id, charm.type())
//                    refreshFragment()
//                })
    }

    private fun populateDecorations(userEquipment: UserEquipment?, userEquipmentSetId: Int, card: UserEquipmentCard) {
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
            mutableListOf()
        }

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
                    viewModel.deleteDecorationForEquipment(userDecoration.decoration.id,
                            userEquipment.entityId(), userDecoration.slotNumber, userEquipment.type(), userEquipmentSetId)

                }
        )
    }

    private fun refreshFragment() {
        val currentFragment = this
        val fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.detach(currentFragment)
        fragmentTransaction.attach(currentFragment)
        fragmentTransaction.commit()
    }
}