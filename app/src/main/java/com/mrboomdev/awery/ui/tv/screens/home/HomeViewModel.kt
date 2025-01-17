package com.mrboomdev.awery.ui.tv.screens.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import com.mrboomdev.awery.R
import com.mrboomdev.awery.app.App.Companion.getMoshi
import com.mrboomdev.awery.app.App.Companion.i18n
import com.mrboomdev.awery.app.ExtensionsManager.loadAll
import com.mrboomdev.awery.data.serializaton.SettingsSerializer
import com.mrboomdev.awery.ext.data.CatalogFeed
import com.mrboomdev.awery.generated.AwerySettings
import com.mrboomdev.awery.util.IconStateful
import com.mrboomdev.awery.util.exceptions.ZeroResultsException
import com.mrboomdev.awery.util.extensions.ensureSize
import com.mrboomdev.awery.util.io.FileUtil.readAssets
import com.squareup.moshi.Json
import com.squareup.moshi.adapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.io.Serial
import java.io.Serializable

// TODO: Remove after existing classes would be migrated to the new logic
@Suppress("ClassName")
class __TabsTemplate__ {
	var id: String? = null
	var title: String? = null
	var icon: String? = null
	var description: String? = null
	var tabs: List<__DBTab__>? = null
}

// TODO: Remove after existing classes would be migrated to the new logic
@Suppress("ClassName")
class __DBTab__: Comparable<__DBTab__>, Serializable {
	var id: String = System.currentTimeMillis().toString()
	var icon: String? = null
	lateinit var title: String
	var index: Int = 0
	@Json(name = "show_end")
	var showEnd: Boolean = true
	lateinit var feeds: List<CatalogFeed>

	override fun compareTo(other: __DBTab__): Int {
		return index.compareTo(other.index)
	}

	companion object {
		@Serial
		private val serialVersionUID: Long = 1
	}
}

class TabState(
	val feeds: List<CatalogFeed.Loaded>,
	val failedFeeds: List<CatalogFeed.Loaded>,
	val isLoading: MutableState<Boolean>
): Serializable

class HomeViewModel : ViewModel() {
	val tabsState = mutableStateListOf<TabState?>()
	val tabs = mutableStateListOf<Pair<__DBTab__, IconStateful>>()
	var currentTab = mutableIntStateOf(-1)
	var drawerState: DrawerState = DrawerState(DrawerValue.Closed)
	var listState: LazyListState = LazyListState()

	fun loadTabsList() {
		if(tabs.isEmpty()) {
			val template = AwerySettings.TABS_TEMPLATE.value

			val icons = getMoshi().adapter<Map<String, IconStateful>>()
				.fromJson(readAssets("icons.json"))!!

			val dbTabs = /*if(template == "custom") database.tabsDao.allTabs else*/ run {
				val selected = getMoshi(SettingsSerializer).adapter<List<__TabsTemplate__>>()
					.fromJson(readAssets("tabs_templates.json"))!!
					.find { it.id == template }

				if(selected != null) selected.tabs else {
					throw ZeroResultsException("No templates was found with such name ($template)! Maybe it was removed.")
				}
			}!!

			tabs.addAll(dbTabs.map {
				it.title = i18n<R.string>(it.title) ?: it.title
				it to (icons[it.icon] ?: IconStateful(activeId = R.drawable.ic_view_cozy))
			})

			tabs.add(__DBTab__().apply {
				title = i18n(R.string.settings)
				id = "settings"
			} to IconStateful(activeId = R.drawable.ic_settings_filled))

			currentTab.intValue = AwerySettings.DEFAULT_HOME_TAB.value?.let { defaultTab ->
				dbTabs.find { it.id == defaultTab }
			}?.let { dbTabs.indexOf(it) } ?: 0
		}
	}

	fun loadTabIfRequired(position: Int) {
		if(tabsState.getOrNull(position) == null) {
			val feeds = mutableStateListOf<CatalogFeed.Loaded>()
			val failedFeeds = mutableStateListOf<CatalogFeed.Loaded>()
			val isLoading = mutableStateOf(true)

			tabsState.ensureSize(position + 1)
			tabsState[position] = TabState(feeds, failedFeeds, isLoading)

			tabs[position].first.feeds.loadAll()
				.onEach {
					if(it.items.isNullOrEmpty() || it.throwable != null) {
						failedFeeds.add(0, it)
					} else {
						feeds.add(it)
					}
				}
				.onCompletion { isLoading.value = false }
				.launchIn(CoroutineScope(viewModelScope.coroutineContext + Dispatchers.IO))
		}
	}
}