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

package cc.sovellus.vrcaa.ui.screen.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cc.sovellus.vrcaa.R
import cc.sovellus.vrcaa.manager.FeedManager
import cc.sovellus.vrcaa.manager.FriendManager
import cc.sovellus.vrcaa.ui.components.layout.FeedItem
import cc.sovellus.vrcaa.ui.screen.profile.UserProfileScreen
import cc.sovellus.vrcaa.ui.screen.world.WorldScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollbarHeight = remember { mutableFloatStateOf(0f) }
    val alpha = remember { Animatable(0f) }
    val isDragging = remember { mutableStateOf(false) }

    val totalItems by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    val firstVisibleItem by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val visibleItems by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }
    }

    val thumbHeightFraction by remember {
        derivedStateOf {
            if (totalItems > 0) {
                (visibleItems.toFloat() / totalItems).coerceIn(0.05f, 1f)
            } else {
                1f
            }
        }
    }

    val thumbOffset by remember {
        derivedStateOf {
            if (totalItems > visibleItems && scrollbarHeight.floatValue > 0) {
                val trackRange = scrollbarHeight.floatValue * (1f - thumbHeightFraction)
                val scrollRange = (totalItems - visibleItems).coerceAtLeast(1)
                (firstVisibleItem.toFloat() / scrollRange) * trackRange
            } else {
                0f
            }
        }
    }

    LaunchedEffect(totalItems, visibleItems) {
        if (totalItems > visibleItems) {
            snapshotFlow {
                Triple(
                    listState.isScrollInProgress,
                    isDragging.value,  // 添加拖拽状态
                    listState.firstVisibleItemIndex  // 监听索引变化
                )
            }.collect { (isScrolling, isDraggingThumb, _) ->
                // 正在滚动或拖拽滚动条时，保持可见
                if (isScrolling || isDraggingThumb) {
                    alpha.animateTo(1f, animationSpec = tween(150))
                } else {
                    delay(2000)
                    alpha.animateTo(0f, animationSpec = tween(500))
                }
            }
        } else {
            alpha.snapTo(0f)
        }
    }

    val currentThumbOffset = rememberUpdatedState(thumbOffset)
    val currentThumbHeightFraction = rememberUpdatedState(thumbHeightFraction)
    val currentTotalItems = rememberUpdatedState(totalItems)
    val currentVisibleItems = rememberUpdatedState(visibleItems)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .alpha(alpha.value)
            .onGloballyPositioned { coordinates ->
                scrollbarHeight.floatValue = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging.value = true
                        coroutineScope.launch {
                            alpha.snapTo(1f)  // 放到协程中执行
                        }
                    },
                    onDragEnd = {
                        isDragging.value = false
                    },
                    onDragCancel = {
                        isDragging.value = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val trackRange = scrollbarHeight.floatValue * (1f - currentThumbHeightFraction.value)
                    if (trackRange > 0) {
                        val newOffset = (currentThumbOffset.value + dragAmount.y).coerceIn(0f, trackRange)
                        val scrollRange = (currentTotalItems.value - currentVisibleItems.value).coerceAtLeast(1)
                        val newIndex = ((newOffset / trackRange) * scrollRange).roundToInt()
                        coroutineScope.launch {
                            listState.scrollToItem(newIndex.coerceIn(0, currentTotalItems.value - 1))
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, thumbOffset.roundToInt()) }
                .align(Alignment.TopEnd)
                .width(13.dp)
                .fillMaxHeight(thumbHeightFraction)
                .padding(vertical = 2.dp, horizontal = 1.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
    }
}
@Composable
fun FeedList(feed: List<FeedManager.Feed>, filter: Boolean = false, hasMore: Boolean = false) {
    val navigator = LocalNavigator.currentOrThrow
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(1.dp),
            state = listState
        ) {
            items(feed)
            { item ->
                when (item.type) {
                    FeedManager.FeedType.FRIEND_FEED_ONLINE -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_online_text))
                            }
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_online_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_OFFLINE -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_offline_text))
                            }
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_offline_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_LOCATION -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_location_text))
                            }
                            append(" ")
                            append(item.travelDestination)
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_location_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(WorldScreen(item.worldId))
                                } else {
                                    navigator.parent?.parent?.push(WorldScreen(item.worldId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_STATUS -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_status_text))
                            }
                            append(" ")
                            append(item.friendStatus.toString())
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_status_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_ADDED -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_added_text))
                            }
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_added_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_REMOVED -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_removed_text))
                            }
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_removed_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_FRIEND_REQUEST -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_friend_request_text))
                            }
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_friend_request_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_AVATAR -> {
                        val text = buildAnnotatedString {
                            append(item.friendName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_friend_avatar_text))
                            }
                            append(" ")
                            append(item.avatarName)
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_friend_avatar_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }

                    FeedManager.FeedType.FRIEND_FEED_USERNAME_CHANGE -> {
                        val text = buildAnnotatedString {
                            append(FriendManager.getFriend(item.friendId)?.displayName)
                            append(" ")
                            withStyle(style = SpanStyle(color = Color.Gray)) {
                                append(stringResource(R.string.feed_friend_username_changed_text))
                            }
                            append(" ")
                            append(item.friendName)
                        }
                        FeedItem(
                            text = text,
                            friendPictureUrl = item.friendPictureUrl,
                            feedTimestamp = item.feedTimestamp,
                            resourceStringTitle = R.string.feed_friend_username_changed_label,
                            onClick = {
                                if (filter) {
                                    navigator.push(UserProfileScreen(item.friendId))
                                } else {
                                    navigator.parent?.parent?.push(UserProfileScreen(item.friendId))
                                }
                            }
                        )
                    }
                }
            }
            if (hasMore) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button({
                            FeedManager.loadFeed()
                        }) {
                            Text(stringResource(R.string.search_button_more))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = null)
            }
        }
        VerticalScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

class FeedScreen : Screen {

    override val key = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val model = navigator.rememberNavigatorScreenModel { FeedScreenModel() }

        //val state by model.state.collectAsState()

        ShowScreen(model)
    }

    @Composable
    fun ShowScreen(model: FeedScreenModel) {
        val feed = model.feed.collectAsState()
        val hasMore = model.hasMore.collectAsState()
        FeedList(feed.value, hasMore = hasMore.value)
    }
}