package com.gatheringhallstudios.mhworlddatabase.features.userequipmentsetbuilder.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.gatheringhallstudios.mhworlddatabase.data.AppDatabase
import com.gatheringhallstudios.mhworlddatabase.data.models.UserEquipment
import com.gatheringhallstudios.mhworlddatabase.data.models.UserEquipmentSet
import com.gatheringhallstudios.mhworlddatabase.data.types.DataType
import kotlinx.coroutines.*

class UserEquipmentSetDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val appDao = AppDatabase.getAppDataBase(application)!!.userEquipmentSetDao()
    var activeUserEquipmentSet = MutableLiveData<UserEquipmentSet>()
    private var activeUserEquipment: UserEquipment? = null // The userEquipment model being edited via armor/weapon/charm/decoration selector

    fun setActiveUserEquipment(userEquipment: UserEquipment?) {
        this.activeUserEquipment = userEquipment
    }

    fun deleteDecorationForEquipment(decorationId: Int, targetDataId: Int, targetSlotNumber: Int, type: DataType, userEquipmentSetId: Int) {
        appDao.deleteUserEquipmentDecoration(userEquipmentSetId, targetDataId, type, decorationId, targetSlotNumber)
    }

    fun deleteEquipmentSet(userEquipmentSetId: Int) {
        runBlocking {
            withContext(Dispatchers.IO) {
                appDao.deleteUserEquipmentSet(userEquipmentSetId)
                appDao.deleteUserEquipmentSetEquipment(userEquipmentSetId)
                appDao.deleteUserEquipmentSetDecorations(userEquipmentSetId)
            }
        }
    }

    fun renameEquipmentSet(name: String, userEquipmentSetId: Int) {
        runBlocking {
            withContext(Dispatchers.IO) {
                appDao.renameUserEquipmentSet(name, userEquipmentSetId)
            }
        }
    }

    fun deleteUserEquipment(userEquipmentId: Int, userEquipmentSetId: Int, type: DataType) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                appDao.deleteUserEquipmentEquipment(userEquipmentId, type, userEquipmentSetId)
                appDao.deleteUserEquipmentDecorations(userEquipmentSetId, userEquipmentId, type)
            }
        }
    }
}
