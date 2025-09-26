/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.uxr

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.example.nav3recipes.content.ContentBase
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import com.example.nav3recipes.ui.theme.Grey90
import com.example.nav3recipes.ui.theme.colors
import com.example.nav3recipes.uxr.TitledSinglePaneScene.Companion.TITLE_TEXT
import kotlinx.serialization.Serializable

/**
 * Scenes UXR Study Exercise
 */

@Serializable
object ConversationList : NavKey

@Serializable
data class ConversationDetail(val id: Int) : NavKey {
    val color: Color
        get() = colors[id % colors.size]
}


data class TitledSinglePaneScene<T : Any>(
    override val key: Any,
    val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)
    @OptIn(ExperimentalMaterial3Api::class)
    override val content: @Composable () -> Unit = {

        Column {
            Row {
                CenterAlignedTopAppBar(
                    title = {
                    Text(entry.metadata.getOrDefault(TITLE_TEXT, "").toString())
                })
            }
            Row {
                entry.Content()
            }
        }
    }

    companion object {
        internal const val TITLE_SCENE_KEY = "TitleScene"
        internal const val TITLE_TEXT = "TitleText"
        fun titleScene() = mapOf(TITLE_SCENE_KEY to true)
    }
}

class TitledSinglePaneSceneStrategy<T : Any> : SceneStrategy<T> {
    @Composable
    override fun calculateScene(entries: List<NavEntry<T>>, onBack: (Int) -> Unit): Scene<T>? {


        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

        // Return null if the current window size is larger than 600dp - so we show the two pane without the title
        // WIDTH_DP_MEDIUM_LOWER_BOUND (600dp).
        if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            return null
        }


        return TitledSinglePaneScene(
            key = entries.last().contentKey,
            entry = entries.last(),
            previousEntries = entries.dropLast(1)
        )

    }
}


class Exercise1Activity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        setContent {
            val backStack = rememberNavBackStack<NavKey>(ConversationList)

            val windowAdaptiveInfo = currentWindowAdaptiveInfo()
            val directive = remember(windowAdaptiveInfo) {
                calculatePaneScaffoldDirective(windowAdaptiveInfo)
                    .copy(horizontalPartitionSpacerSize = 0.dp)
            }
            val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
            val titledSinglePaneSceneStrategy = remember { TitledSinglePaneSceneStrategy<NavKey>() }

            Scaffold { paddingValues ->
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategy = listDetailStrategy then titledSinglePaneSceneStrategy ,
                    modifier = Modifier.padding(paddingValues),
                    entryProvider = entryProvider {
                        entry<ConversationList>(
                            metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        ContentBase(
                                            title = "Choose a conversation from the list",
                                            modifier = Modifier.background(Grey90)
                                        )
                                    }
                                ) + TitledSinglePaneScene.titleScene() + Pair(TITLE_TEXT, "Conversation List")
                        ) {
                            ConversationListScreen(
                                onConversationClicked = { conversationDetail ->
                                    // Pop any existing ConversationDetail screens
                                    backStack.removeIf { it is ConversationDetail }
                                    backStack.add(conversationDetail)
                                }
                            )
                        }
                        entry<ConversationDetail>(
                            metadata = ListDetailSceneStrategy.detailPane() + TitledSinglePaneScene.titleScene() + Pair(TITLE_TEXT, "Conversation Detail")
                        ) { key ->
                            ConversationDetailScreen(key)
                        }
                    }
                )
            }
        }
    }
}
