package com.quranwords.ui.surah

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.QuranStats
import com.quranwords.ui.surah.components.JuzTab
import com.quranwords.ui.surah.components.QuickJumpDialog
import com.quranwords.ui.surah.components.SurahsTab

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SurahIndexScreen(
    uiState: SurahIndexUiState,
    onEvent: (SurahIndexEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onJumpToPage: (Int) -> Unit = {},
    onJumpToSurahAyah: (Int, Int) -> Unit = { s, a -> onNavigateToSurahDetail(s, a) }
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showQuickJump by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Flat quran.com chrome: the bar, tabs and list share one surface
            // color — no tinted top area over a different container.
            Column {
                LargeTopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = stringResource(R.string.surah_index_title), fontWeight = FontWeight.Bold)
                            if (scrollBehavior.state.collapsedFraction < 0.5f) {
                                Text(
                                    text = stringResource(
                                        R.string.surah_index_subtitle,
                                        QuranStats.TOTAL_SURAHS, QuranStats.MECCAN_SURAHS, QuranStats.MEDINAN_SURAHS, QuranStats.TOTAL_JUZ
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showQuickJump = true }, modifier = Modifier.testTag("quick_jump_btn")) {
                            Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.quick_jump_title))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surface, divider = {}) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(text = stringResource(R.string.surah_index_tab_surahs, QuranStats.TOTAL_SURAHS), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_surahs")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(text = stringResource(R.string.surah_index_tab_juz, QuranStats.TOTAL_JUZ), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_juz")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("surah_index_screen")) {
                if (selectedTabIndex == 0) {
                    SurahsTab(uiState = uiState, onEvent = onEvent, onNavigateToSurahDetail = onNavigateToSurahDetail)
                } else {
                    JuzTab(onNavigateToSurahDetail = onNavigateToSurahDetail)
                }
            }
            if (showQuickJump) {
                QuickJumpDialog(
                    onJumpToPage = { page -> showQuickJump = false; onJumpToPage(page) },
                    onJumpToSurahAyah = { s, a -> showQuickJump = false; onJumpToSurahAyah(s, a) },
                    onDismiss = { showQuickJump = false }
                )
            }
        }
    }
}
