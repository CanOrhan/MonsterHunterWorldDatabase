package com.gatheringhallstudios.mhworlddatabase.features.items

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.drm.DrmConvertedStatus
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gatheringhallstudios.mhworlddatabase.R
import com.gatheringhallstudios.mhworlddatabase.common.Navigator
import com.gatheringhallstudios.mhworlddatabase.common.adapters.BasicListDelegationAdapter
import com.gatheringhallstudios.mhworlddatabase.common.adapters.SimpleListDelegate
import com.gatheringhallstudios.mhworlddatabase.data.Converters
import com.gatheringhallstudios.mhworlddatabase.data.MHWDatabase
import com.gatheringhallstudios.mhworlddatabase.data.dao.ItemDao
import com.gatheringhallstudios.mhworlddatabase.data.types.ItemCategory
import com.gatheringhallstudios.mhworlddatabase.data.views.ItemView
import com.gatheringhallstudios.mhworlddatabase.util.BundleBuilder
import kotlinx.android.synthetic.main.list_generic.*
import kotlinx.android.synthetic.main.listitem_monster.view.*

// move to adapters if more classes need to use it
class ItemListDelegate(private val onSelect: (ItemView) -> Unit) : SimpleListDelegate<ItemView>(ItemView::class) {
    // todo: create item listitem layout
    override fun getLayoutId() = R.layout.listitem_monster

    override fun bindListItem(v: View, item : ItemView) {
        v.monster_name.text = item.name

        v.setOnClickListener { onSelect(item) }
    }
}

class ItemListFragment : Fragment() {
    companion object {
        private val ARG_CAT = "CATEGORY"

        @JvmStatic
        fun newInstance(category: ItemCategory): ItemListFragment {
            val f = ItemListFragment()
            f.arguments = BundleBuilder().putSerializable(ARG_CAT, category).build()
            return f
        }
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this).get(ItemListFragment.ViewModel::class.java)
    }

    // Setup recycler list adapter and the on-selected
    private val adapter = BasicListDelegationAdapter(ItemListDelegate(onSelect={
        val nav = activity as Navigator
        nav.navigateTo(ItemDetailPagerFragment.newInstance(it.id))
    }))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.list_generic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler_view.adapter = adapter
        var category: ItemCategory = ItemCategory.MATERIAL
        val args = arguments;
        if(args != null) {
            category = args.getSerializable(ARG_CAT) as ItemCategory
        }

        viewModel.init(category)

        viewModel.items.observe(this, Observer({
            adapter.items = it
            adapter.notifyDataSetChanged()
        }))
    }

    // ViewModel class used by this Fragment
    class ViewModel(application : Application) : AndroidViewModel(application) {
        private val dao : ItemDao = MHWDatabase.getDatabase(application).itemDao()
        lateinit var items : LiveData<List<ItemView>> private set


        fun init(category: ItemCategory) {
            var converters = Converters()
            items = dao.getItems("en", converters.fromCategory(category))
        }
    }
}