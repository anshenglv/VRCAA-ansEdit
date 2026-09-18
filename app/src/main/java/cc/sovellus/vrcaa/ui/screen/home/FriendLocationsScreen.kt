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

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cc.sovellus.vrcaa.App
import cc.sovellus.vrcaa.R
import cc.sovellus.vrcaa.extension.columnCountOption
import cc.sovellus.vrcaa.extension.fixedColumnSize
import cc.sovellus.vrcaa.manager.CacheManager
import cc.sovellus.vrcaa.manager.FriendManager
import cc.sovellus.vrcaa.ui.components.layout.GridItemWithFriends
import cc.sovellus.vrcaa.ui.screen.world.WorldScreen

class FriendLocationsScreen : Screen {

    override val key = uniqueScreenKey
    private val preferences: SharedPreferences = App.getContext().getSharedPreferences(App.PREFERENCES_NAME, MODE_PRIVATE)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val friends = FriendManager.friendsState.collectAsState().value
        val cachedWorlds = CacheManager.worldList.collectAsState().value

        val friendLocations = friends.filter { it.location.contains("wrld_") }
        val distinctLocations = friendLocations.distinctBy { it.location.split(':')[0] }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    title = { Text(text = stringResource(R.string.home_friend_locations)) }
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = when (preferences.columnCountOption) {
                    0 -> GridCells.Adaptive(166.dp)
                    else -> GridCells.Fixed(preferences.fixedColumnSize)},
                contentPadding = PaddingValues(
                    start = 12.dp, top = 16.dp, end = 16.dp, bottom = 16.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(distinctLocations) { friend ->
                    val worldId = friend.location.split(':')[0]
                    val world = cachedWorlds.find { it.id == worldId }
                    GridItemWithFriends(
                        name = world?.name ?: "???",
                        url = world?.thumbnailUrl ?: "",
                        friends = friends.filter { it.location == friend.location },
                        onClick = { navigator.push(WorldScreen(worldId)) }
                    )
                }
            }
        }
    }
}
