package com.gatheringhallstudios.mhworlddatabase.features.monsters;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import com.gatheringhallstudios.mhworlddatabase.R;
import com.gatheringhallstudios.mhworlddatabase.common.BasePagerFragment;
import com.gatheringhallstudios.mhworlddatabase.data.views.Monster;

import butterknife.BindString;
import butterknife.BindView;

/**
 * Monster Hub
 */

public class MonsterDetailPagerFragment extends BasePagerFragment {

    private final String TAG = getClass().getSimpleName();

    private Monster monster;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.pager_list)
    ViewPager viewPager;

    @BindString(R.string.monsters_detail_tab_summary)
    String tabTitleSummary;

    public static MonsterDetailPagerFragment getInstance(Monster monster){
        MonsterDetailPagerFragment fragment = new MonsterDetailPagerFragment();
        fragment.monster = monster;

        return fragment;
    }

    @Override
    public void onAddTabs(TabAdder tabs) {
        tabs.addTab(tabTitleSummary, () ->
                MonsterSummaryFragment.newInstance(monster.id)
        );

//        tabs.addTab(tabTitleSmall, () ->
//                MonsterListFragment.newInstance(MonsterListViewModel.Tab.SMALL)
//        );
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null)
            getActivity().setTitle(monster.name);
    }
}
