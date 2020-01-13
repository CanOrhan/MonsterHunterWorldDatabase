package com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.detail

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.components.ExpandableCardView
import com.gatheringhallstudios.mhworlddatabase.data.models.*
import com.gatheringhallstudios.mhworlddatabase.data.types.ArmorType
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

    private fun populateUserEquipmentSet(userEquipmentSet: UserEquipmentSet) {
        setActivityTitle(userEquipmentSet.name)

        val weapon = userEquipmentSet.getWeapon()
        var card = UserEquipmentCard(user_equipment_weapon_slot)
        card.bindWeapon(weapon, userEquipmentSet.id)
        if (weapon != null) {
            card.setOnClick {
                viewModel.setActiveUserEquipment(weapon)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.WEAPON, weapon,
                        userEquipmentSet.id, null, null)
            }
            card.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(weapon)
                viewModel.deleteUserEquipment(weapon.entityId(), userEquipmentSet.id, weapon.type())
                refreshFragment()
            }
            populateDecorations(weapon, userEquipmentSet.id, card)
        }

        val headArmor = userEquipmentSet.getHeadArmor()
        card = UserEquipmentCard(user_equipment_head_slot)
        card.bindHeadArmor(headArmor, userEquipmentSet.id)
        if (headArmor != null) {
            populateDecorations(headArmor, userEquipmentSet.id, card)
            card.setOnClick {
                viewModel.setActiveUserEquipment(headArmor)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, headArmor,
                        userEquipmentSet.id, headArmor.armor.armor.armor_type, null)
            }
            card.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(headArmor)
                viewModel.deleteUserEquipment(headArmor.entityId(), userEquipmentSet.id, headArmor.type())
                refreshFragment()
            }

            card.setOnSwipeLeft {
                val toast = Toast.makeText(context, "SWIPED LEFT!", Toast.LENGTH_LONG)
                toast.show()
            }
        }

        val armArmor = userEquipmentSet.getArmArmor()
        card = UserEquipmentCard(user_equipment_arms_slot)
        card.bindArmArmor(armArmor, userEquipmentSet.id)
        if (armArmor != null) {
            populateDecorations(armArmor, userEquipmentSet.id, card)
            card.setOnClick {
                viewModel.setActiveUserEquipment(armArmor)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, armArmor,
                        userEquipmentSet.id, armArmor.armor.armor.armor_type, null)
            }
            card.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(armArmor)
                viewModel.deleteUserEquipment(armArmor.entityId(), userEquipmentSet.id, armArmor.type())
                refreshFragment()
            }

            card.setOnSwipeLeft {
                val toast = Toast.makeText(context, "SWIPED LEFT!", Toast.LENGTH_LONG)
                toast.show()
            }
        }

        val chestArmor = userEquipmentSet.getChestArmor()
        card = UserEquipmentCard(user_equipment_chest_slot)
        card.bindChestArmor(chestArmor, userEquipmentSet.id)
        if (chestArmor != null) {
            populateDecorations(chestArmor, userEquipmentSet.id, card)
            card.setOnClick {
                viewModel.setActiveUserEquipment(chestArmor)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, chestArmor,
                        userEquipmentSet.id, chestArmor.armor.armor.armor_type, null)
            }
            card.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(chestArmor)
                viewModel.deleteUserEquipment(chestArmor.entityId(), userEquipmentSet.id, chestArmor.type())
                refreshFragment()
            }

            card.setOnSwipeLeft {
                val toast = Toast.makeText(context, "SWIPED LEFT!", Toast.LENGTH_LONG)
                toast.show()
            }
        }

        val legArmor = userEquipmentSet.getLegArmor()
        card = UserEquipmentCard(user_equipment_legs_slot)
        card.bindLegArmor(legArmor, userEquipmentSet.id)
        if (legArmor != null) {
            populateDecorations(legArmor, userEquipmentSet.id, card)
            card.setOnClick {
                viewModel.setActiveUserEquipment(legArmor)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, legArmor,
                        userEquipmentSet.id, legArmor.armor.armor.armor_type, null)
            }
            card.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(legArmor)
                viewModel.deleteUserEquipment(legArmor.entityId(), userEquipmentSet.id, legArmor.type())
                refreshFragment()
            }

            card.setOnSwipeLeft {
                val toast = Toast.makeText(context, "SWIPED LEFT!", Toast.LENGTH_LONG)
                toast.show()
            }
        }

        val waistArmor = userEquipmentSet.getWaistArmor()
        card = UserEquipmentCard(user_equipment_waist_slot)
        card.bindWaistArmor(waistArmor, userEquipmentSet.id)
        if (waistArmor != null) {
            populateDecorations(waistArmor, userEquipmentSet.id, card)
            card.setOnClick {
                viewModel.setActiveUserEquipment(waistArmor)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.ARMOR, waistArmor,
                        userEquipmentSet.id, waistArmor.armor.armor.armor_type, null)
            }
            card.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(waistArmor)
                viewModel.deleteUserEquipment(waistArmor.entityId(), userEquipmentSet.id, waistArmor.type())
                refreshFragment()
            }

            card.setOnSwipeLeft {
                val toast = Toast.makeText(context, "SWIPED LEFT!", Toast.LENGTH_LONG)
                toast.show()
            }
        }

        val charm = userEquipmentSet.getCharm()
        card = UserEquipmentCard(user_equipment_charm_slot)
        card.bindCharm(charm, userEquipmentSet.id)
        if (charm != null) {
            user_equipment_charm_slot.setOnClick {
                viewModel.setActiveUserEquipment(charm)
                getRouter().navigateUserEquipmentPieceSelector(Companion.SelectorMode.CHARM, charm, userEquipmentSet.id,
                        null, null)
            }
            user_equipment_charm_slot.setOnSwipeRight {
                viewModel.activeUserEquipmentSet.value?.equipment?.remove(charm)
                viewModel.deleteUserEquipment(charm.entityId(), userEquipmentSet.id, charm.type())
                refreshFragment()
            }
        }
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
            emptyList()
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
                    viewModel.deleteDecorationForEquipment(userDecoration.decoration.id, userEquipment.entityId(), userDecoration.slotNumber, userEquipment.type(), userEquipmentSetId)
                    val buffer = ViewModelProviders.of(activity!!).get(UserEquipmentSetListViewModel::class.java)
                    viewModel.activeUserEquipmentSet.value = buffer.getEquipmentSet(viewModel.activeUserEquipmentSet.value!!.id)
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