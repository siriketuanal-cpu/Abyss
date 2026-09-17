package com.aistudio.abyss.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.abyss.model.AbyssItem
import com.aistudio.abyss.model.GroupItem
import com.aistudio.abyss.model.HeaderItem
import com.aistudio.abyss.model.IdleItem
import com.aistudio.abyss.model.RuleItem
import com.aistudio.abyss.model.StamItem
import com.aistudio.abyss.model.TimerCardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val now by viewModel.now.collectAsState()
    val pending40Id by viewModel.pending40Id.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                viewModel.cancelPendingActions()
            },
        containerColor = AbyssColors.Background,
        topBar = {
            TimerHeader(
                onRefresh = { viewModel.cancelPendingActions() },
                onAddClick = { viewModel.openAddMenu() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "＋ でタイマーを追加",
                        color = AbyssColors.Sub,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("empty_state_text")
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.id }) { entry ->
                        when (entry) {
                            is GroupItem -> {
                                GroupCard(
                                    item = entry,
                                    now = now,
                                    pending40Id = pending40Id,
                                    onLongClick = { viewModel.openActionMenu(entry) },
                                    onEditGroupName = {
                                        viewModel.openEditName(entry.id, "アカウント名の編集", entry.name)
                                    },
                                    onCardClick = { child ->
                                        when (child) {
                                            is AbyssItem -> viewModel.onAbyssCardTap(child)
                                            is IdleItem -> viewModel.onIdleCardTap(child)
                                            is StamItem -> {}
                                        }
                                    },
                                    onChildLongClick = { child ->
                                        viewModel.openActionMenu(child, parentGroupId = entry.id)
                                    },
                                    onEditChildNumber = { child, isCurrent ->
                                        when (child) {
                                            is StamItem -> {
                                                if (isCurrent) {
                                                    viewModel.openEditNumber(
                                                        id = child.id,
                                                        title = "スタミナ現在値の編集",
                                                        currentValue = child.current,
                                                        min = 0,
                                                        max = child.max,
                                                        onConfirm = { viewModel.updateStamCurrent(child.id, it) }
                                                    )
                                                } else {
                                                    viewModel.openEditNumber(
                                                        id = child.id,
                                                        title = "スタミナ上限値の編集",
                                                        currentValue = child.max,
                                                        min = 1,
                                                        max = 999,
                                                        onConfirm = { viewModel.updateStamMax(child.id, it) }
                                                    )
                                                }
                                            }
                                            is AbyssItem -> {
                                                if (isCurrent) {
                                                    viewModel.openEditNumber(
                                                        id = child.id,
                                                        title = "Abyssスタミナ現在値の編集",
                                                        currentValue = child.current,
                                                        min = 0,
                                                        max = child.max,
                                                        onConfirm = { viewModel.updateAbyssCurrent(child.id, it) }
                                                    )
                                                }
                                            }
                                            is IdleItem -> {}
                                        }
                                    },
                                    onEditChildRank = { child ->
                                        viewModel.openEditRank(child.id, child.rank)
                                    }
                                )
                            }
                            is HeaderItem -> {
                                HeaderCard(
                                    item = entry,
                                    onEditName = {
                                        viewModel.openEditName(entry.id, "見出しの編集", entry.name)
                                    },
                                    onLongClick = { viewModel.openActionMenu(entry) }
                                )
                            }
                            is RuleItem -> {
                                RuleCard(
                                    item = entry,
                                    onLongClick = { viewModel.openActionMenu(entry) }
                                )
                            }
                            is StamItem -> {
                                StamCard(
                                    item = entry,
                                    now = now,
                                    onLongClick = { viewModel.openActionMenu(entry) },
                                    onEditCurrent = {
                                        viewModel.openEditNumber(
                                            id = entry.id,
                                            title = "スタミナ現在値の編集",
                                            currentValue = entry.current,
                                            min = 0,
                                            max = entry.max,
                                            onConfirm = { viewModel.updateStamCurrent(entry.id, it) }
                                        )
                                    },
                                    onEditMax = {
                                        viewModel.openEditNumber(
                                            id = entry.id,
                                            title = "スタミナ上限値の編集",
                                            currentValue = entry.max,
                                            min = 1,
                                            max = 999,
                                            onConfirm = { viewModel.updateStamMax(entry.id, it) }
                                        )
                                    }
                                )
                            }
                            is AbyssItem -> {
                                AbyssCard(
                                    item = entry,
                                    now = now,
                                    isPending40 = pending40Id == entry.id,
                                    onCardClick = { viewModel.onAbyssCardTap(entry) },
                                    onLongClick = { viewModel.openActionMenu(entry) },
                                    onEditRank = { viewModel.openEditRank(entry.id, entry.rank) },
                                    onEditCurrent = {
                                        viewModel.openEditNumber(
                                            id = entry.id,
                                            title = "Abyssスタミナ現在値の編集",
                                            currentValue = entry.current,
                                            min = 0,
                                            max = entry.max,
                                            onConfirm = { viewModel.updateAbyssCurrent(entry.id, it) }
                                        )
                                    }
                                )
                            }
                            is IdleItem -> {
                                IdleCard(
                                    item = entry,
                                    now = now,
                                    onCardClick = { viewModel.onIdleCardTap(entry) },
                                    onLongClick = { viewModel.openActionMenu(entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog routing
    when (val dialog = dialogState) {
        is DialogState.AddPanel -> {
            AddPanelDialog(
                parentGroupId = dialog.parentGroupId,
                onSelectGroup = { viewModel.addGroup() },
                onSelectHeader = { viewModel.openSetupHeader() },
                onSelectRule = { viewModel.openSetupRule() },
                onSelectStam = { viewModel.openSetupTimer(dialog.parentGroupId, "stam") },
                onSelectAbyss = { viewModel.openSetupTimer(dialog.parentGroupId, "abyss") },
                onSelectIdle = { viewModel.openSetupTimer(dialog.parentGroupId, "idle") },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.SetupTimer -> {
            SetupTimerDialog(
                type = dialog.type,
                onConfirmStam = { interval -> viewModel.addStam(dialog.parentGroupId, interval) },
                onConfirmAbyss = { interval -> viewModel.addAbyss(dialog.parentGroupId, interval) },
                onConfirmIdle = { dur -> viewModel.addIdle(dialog.parentGroupId, dur) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.SetupHeader -> {
            HeaderColorDialog(
                initialColor = dialog.initialColor,
                onConfirm = { color -> viewModel.addHeader(color) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.SetupRule -> {
            RuleColorDialog(
                initialColor = dialog.initialColor,
                onConfirm = { color -> viewModel.addRule(color) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.EditName -> {
            EditNameDialog(
                title = dialog.title,
                currentName = dialog.currentName,
                onConfirm = { newName -> viewModel.updateName(dialog.id, newName) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.EditNumber -> {
            EditNumberDialog(
                title = dialog.title,
                currentValue = dialog.currentValue,
                min = dialog.min,
                max = dialog.max,
                onConfirm = { num ->
                    dialog.onConfirm(num)
                    viewModel.dismissDialog()
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.EditRank -> {
            EditRankDialog(
                currentRank = dialog.currentRank,
                onConfirm = { rank -> viewModel.updateAbyssRank(dialog.id, rank) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.EditHeaderColor -> {
            HeaderColorDialog(
                initialColor = dialog.currentColor,
                title = "文字色の変更",
                onConfirm = { color -> viewModel.updateHeaderColor(dialog.id, color) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogState.ActionMenu -> {
            ActionMenuDialog(
                item = dialog.item,
                parentGroupId = dialog.parentGroupId,
                onAddTimerToGroup = {
                    viewModel.dismissDialog()
                    viewModel.openAddMenu(dialog.item.id)
                },
                onChangeHeaderColor = {
                    if (dialog.item is HeaderItem) {
                        viewModel.openEditHeaderColor(dialog.item.id, dialog.item.color)
                    }
                },
                onEditName = {
                    val curName = when (dialog.item) {
                        is GroupItem -> dialog.item.name
                        is HeaderItem -> dialog.item.name
                        else -> ""
                    }
                    viewModel.openEditName(dialog.item.id, "名前を変更", curName)
                },
                onDelete = {
                    viewModel.deleteItem(dialog.item.id, dialog.parentGroupId)
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        null -> {}
    }
}

@Composable
fun TimerHeader(
    onRefresh: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AbyssColors.Background,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AbyssColors.Card2)
                    .testTag("btn_refresh")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "アプリを更新",
                    tint = AbyssColors.Text,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "タイマー",
                color = AbyssColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AbyssColors.Card2)
                    .testTag("btn_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "追加",
                    tint = AbyssColors.Text,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
