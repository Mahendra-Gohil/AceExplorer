package com.siju.acexplorer.main.view

import com.siju.acexplorer.R
import com.siju.acexplorer.home.view.HomeScreenFragment
import com.siju.acexplorer.main.model.groups.Category
import com.siju.acexplorer.settings.SettingsFragment
import com.siju.acexplorer.storage.view.FileListFragment

object FragmentsFactory {

    fun createFragment(itemId: Int) =
            when (itemId) {
                R.id.navigation_home -> HomeScreenFragment.newInstance()
                R.id.navigation_tools -> FileListFragment.newInstance(null, Category.APP_MANAGER)
                R.id.navigation_settings -> SettingsFragment.newInstance()
                else ->  HomeScreenFragment.newInstance()
            }
}
