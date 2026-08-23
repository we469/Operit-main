package com.ai.assistance.operit.ui.main.layout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ai.assistance.operit.ui.common.NavItem
import com.ai.assistance.operit.ui.main.NavigationTransitionSource
import com.ai.assistance.operit.ui.main.TopBarTitleContent
import com.ai.assistance.operit.ui.main.navigation.NavigationEntrySpec
import com.ai.assistance.operit.ui.main.navigation.RouteEntry
import com.ai.assistance.operit.ui.main.components.AppContent
import com.ai.assistance.operit.ui.main.components.DrawerContent
import com.ai.assistance.operit.ui.main.components.rememberNavigationDrawerAppearance
import com.ai.assistance.operit.ui.main.screens.GestureStateHolder
import com.ai.assistance.operit.ui.main.screens.Screen
import com.ai.assistance.operit.ui.theme.waterGlass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Layout for phone devices with a modal navigation drawer */
@Composable
fun PhoneLayout(
        currentRouteEntry: RouteEntry,
        currentScreen: Screen,
        selectedItem: NavItem?,
        isLoading: Boolean,
        navItems: List<NavItem>,
        pluginSidebarEntries: List<NavigationEntrySpec>,
        selectedRouteId: String,
        isNetworkAvailable: Boolean,
        networkType: String,
        drawerWidth: Dp,
        navController: androidx.navigation.NavController,
        scope: CoroutineScope,
        drawerState: androidx.compose.material3.DrawerState,
        showFpsCounter: Boolean,
        enableNavigationAnimation: Boolean,
        navigationTransitionSource: NavigationTransitionSource,
        onScreenChange: (Screen) -> Unit,
        onDrawerItemSelected: (Screen) -> Unit,
        onNavigationEntrySelected: (NavigationEntrySpec) -> Unit,
        navigateToTokenConfig: () -> Unit,
        canGoBack: Boolean,
        onGoBack: () -> Unit,
        isNavigatingBack: Boolean = false,
        topBarActions: @Composable RowScope.() -> Unit = {},
        topBarTitleContent: TopBarTitleContent? = null,
        /** 当前导航栈中仍存活的路由 screenKey（路由级 ViewModelStore 清理依据）。 */
        aliveScreenKeys: Set<String>
) {
        // 使用 updateTransition 来创建更复杂的动画
        val transition = updateTransition(drawerState.targetValue, label = "drawer_transition")
        val drawerTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        val drawerProgress by
                transition.animateFloat(
                        label = "drawerProgress",
                        transitionSpec = {
                                if (targetState == DrawerValue.Open) {
                                        spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = 1000f
                                        )
                                } else {
                                        spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = 1000f
                                        )
                                }
                        }
                ) { state -> if (state == DrawerValue.Open) 1f else 0f }

        // 抽屉动画状态
        val isDrawerOpen =
                drawerState.currentValue == DrawerValue.Open ||
                        drawerState.targetValue == DrawerValue.Open

        // 侧边栏以覆盖层形式进入，主页面不参与平移、缩放、圆角或阴影动画。
        // 这样边缘手势不会破坏聊天页面的尺寸、位置与消息输入区布局。
        val drawerOffset = -drawerWidth * (1f - drawerProgress)
        val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f)

        // 侧边栏相关拖拽状态
        var currentDrag by remember { mutableStateOf(0f) }
        var verticalDrag by remember { mutableStateOf(0f) }
        val dragThreshold = 40f

        val drawerAppearance = rememberNavigationDrawerAppearance()
        // 抽屉是覆盖式侧边栏，使用矩形边界（0dp 圆角），避免出现卡片化圆角。
        val drawerShape = RoundedCornerShape(0.dp)

        // 拖拽状态 - 用于控制抽屉拉出和关闭
        val draggableState = rememberDraggableState { delta ->
                // 如果内部手势已被消费，则不处理全局拖拽
                if (!GestureStateHolder.isChatScreenGestureConsumed) {
                        currentDrag += delta

                        if (!isDrawerOpen &&
                                        currentDrag > dragThreshold &&
                                        Math.abs(currentDrag) > Math.abs(verticalDrag)
                        ) {
                                scope.launch {
                                        drawerState.open()
                                        currentDrag = 0f
                                        verticalDrag = 0f
                                }
                        }

                        if (isDrawerOpen && currentDrag < -dragThreshold) {
                                scope.launch {
                                        drawerState.close()
                                        currentDrag = 0f
                                        verticalDrag = 0f
                                }
                        }
                }
        }

        // 使用Box布局来手动控制抽屉和内容的位置关系
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .draggable(
                                        state = draggableState,
                                        orientation = Orientation.Horizontal,
                                        onDragStarted = {
                                                currentDrag = 0f
                                                verticalDrag = 0f
                                        },
                                        onDragStopped = {
                                                currentDrag = 0f
                                                verticalDrag = 0f
                                        }
                                )
                                .draggable(
                                        state =
                                                rememberDraggableState { delta ->
                                                        verticalDrag += delta
                                                },
                                        orientation = Orientation.Vertical,
                                        onDragStarted = { /* 不需要额外操作 */},
                                        onDragStopped = { /* 不需要额外操作 */}
                                )
        ) {
                // 主内容区域 - 使用自定义布局修饰符优化性能
                // 该修饰符只会影响布局，不会触发内容重组
                Surface(
                    // 聊天主页面保持满屏、直角和原始坐标，抽屉仅在其上方覆盖。
                    modifier = Modifier.fillMaxSize().zIndex(1f),
                    color = Color.Transparent,
                ) {
                    // 普通调用AppContent，但由于我们的优化，它不会在动画时重组
                    AppContent(
                        currentRouteEntry = currentRouteEntry,
                        currentScreen = currentScreen,
                        selectedItem = selectedItem,
                        useTabletLayout = false,
                        isTabletSidebarExpanded = false,
                        isLoading = isLoading,
                        navController = navController,
                        scope = scope,
                        drawerState = drawerState,
                        showFpsCounter = showFpsCounter,
                        enableNavigationAnimation = enableNavigationAnimation,
                        navigationTransitionSource = navigationTransitionSource,
                        onScreenChange = onScreenChange,
                        onToggleSidebar = { /* Not used in phone layout */},
                        navigateToTokenConfig = navigateToTokenConfig,
                        canGoBack = canGoBack,
                        onGoBack = onGoBack,
                        isNavigatingBack = isNavigatingBack,
                        actions = topBarActions,
                        titleContent = topBarTitleContent,
                        aliveScreenKeys = aliveScreenKeys
                    )
                }

                // // 添加一个小方块，填充圆角和工具栏之间的空隙
                // Box(
                //         modifier =
                //                 Modifier.width(16.dp)
                //                         .height(64.dp)
                //                         .offset(x = drawerOffset + drawerWidth - 16.dp)
                //                         .background(MaterialTheme.colorScheme.primary)
                //                         .zIndex(1f)
                // )

                // 抽屉内容，从左侧滑动进入 - 使用缓存内容
                Surface(
                        modifier =
                                Modifier.width(drawerWidth)
                                        // 侧栏白色背景延伸至状态栏下方，内容组件单独处理交互控件的 inset。
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                                // 仅保留横向滑入，侧边栏始终是完整的不透明矩形面板。
                                                translationX = drawerOffset.toPx()
                                        }
                                        .zIndex(2f),
                        shape = drawerShape,
                        // 覆盖式侧边栏固定使用不透明白底，避免玻璃效果形成视觉圆角或浮层感。
                        color = Color.White,
                        shadowElevation = 0.dp
                ) {
                        DrawerContent(
                                navItems = navItems,
                                pluginEntries = pluginSidebarEntries,
                                selectedItem = selectedItem,
                                selectedRouteId = selectedRouteId,
                                isNetworkAvailable = isNetworkAvailable,
                                networkType = networkType,
                                appearance = drawerAppearance,
                                topContentPadding = 0.dp,
                                scope = scope,
                                drawerState = drawerState,
                                onScreenSelected = onDrawerItemSelected,
                                onNavigationEntrySelected = onNavigationEntrySelected
                        )
                }

                // 在主内容上方放置遮罩层，阻止右侧内容继续响应点击
                if (isDrawerOpen) {
                        Box(
                                modifier = Modifier.fillMaxSize().zIndex(1.5f)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(start = drawerWidth)
                                                        .background(scrimColor)
                                                        .clickable(
                                                                interactionSource =
                                                                        remember {
                                                                                MutableInteractionSource()
                                                                        },
                                                                indication = null,
                                                                onClick = {
                                                                        scope.launch {
                                                                                drawerState.close()
                                                                        }
                                                                }
                                                        )
                                )
                        }
                }
        }
}
