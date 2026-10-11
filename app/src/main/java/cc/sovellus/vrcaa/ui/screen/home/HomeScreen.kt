/*
 * Copyright (C) 2025. Nyabsi <nyabsi@sovellus.cc>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.sovellus.vrcaa.ui.screen.home

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cc.sovellus.vrcaa.App
import cc.sovellus.vrcaa.R
import cc.sovellus.vrcaa.extension.columnCountOption
import cc.sovellus.vrcaa.extension.fixedColumnSize
import cc.sovellus.vrcaa.manager.CacheManager
import cc.sovellus.vrcaa.ui.components.layout.GridItemWithFriends
import cc.sovellus.vrcaa.ui.components.layout.HorizontalRow
import cc.sovellus.vrcaa.ui.components.layout.RowItem
import cc.sovellus.vrcaa.ui.screen.home.HomeScreenModel.HomeState
import cc.sovellus.vrcaa.ui.screen.misc.LoadingIndicatorScreen
import cc.sovellus.vrcaa.ui.screen.world.WorldScreen

class HomeScreen : Screen {

    override val key = uniqueScreenKey
    private val preferences: SharedPreferences get() = App.getPreferences()
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = navigator.rememberNavigatorScreenModel { HomeScreenModel() }

        val state by model.state.collectAsState()

        when (state) {
            is HomeState.Loading -> {App.setLoadingText(R.string.cache_updating_recent)
                LoadingIndicatorScreen().Content()}
            is HomeState.Result -> ShowScreen(model)
            else -> {}
        }
    }

    @Composable
    fun ShowScreen(model: HomeScreenModel) {

        val navigator = LocalNavigator.currentOrThrow
        val friends = model.friendsList.collectAsState().value
        val recent = model.recentlyVisited.collectAsState().value
        val cachedWorlds = CacheManager.worldList.collectAsState().value

        val friendLocations = friends.filter { it.location.contains("wrld_") }
        val distinctLocations = friendLocations.distinctBy { it.location.split(':')[0] }

        LazyVerticalGrid(
            columns = when (preferences.columnCountOption) {
                0 -> GridCells.Adaptive(166.dp)
                else -> GridCells.Fixed(preferences.fixedColumnSize)
            },
            //contentPadding = PaddingValues(start = 10.dp, end = 10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            if (friendLocations.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HorizontalRow(
                        title = stringResource(R.string.home_friend_locations)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .height(190.dp)
                                    .fillParentMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = stringResource(R.string.result_not_found))
                            }
                        }
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.home_friend_locations),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 4.dp)
                            .clickable{ navigator.parent?.parent?.push(FriendLocationsScreen()) }
                    )
                }

                items(distinctLocations) { friend ->
                    val worldId = friend.location.split(':')[0]
                    val world = cachedWorlds.find { it.id == worldId }
                    GridItemWithFriends(
                        name = world?.name ?: "???",
                        url = world?.thumbnailUrl ?: "",
                        friends = friends.filter { it.location == friend.location },
                        onClick = { navigator.parent?.parent?.push(WorldScreen(worldId)) }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.padding(4.dp))
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (recent.isEmpty()) {
                    HorizontalRow(
                        title = stringResource(R.string.home_recently_visited)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .height(190.dp)
                                    .fillParentMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = stringResource(R.string.result_not_found))
                            }
                        }
                    }
                } else {
                    HorizontalRow(
                        title = stringResource(R.string.home_recently_visited),
                        onClick = { navigator.parent?.parent?.push(RecentlyVisitedScreen()) }
                    ) {
                        items(recent) { world ->
                            RowItem(
                                name = world.name,
                                url = world.thumbnailUrl,
                                onClick = { navigator.parent?.parent?.push(WorldScreen(world.id)) }
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.padding(4.dp))
            }
        }
    }
}