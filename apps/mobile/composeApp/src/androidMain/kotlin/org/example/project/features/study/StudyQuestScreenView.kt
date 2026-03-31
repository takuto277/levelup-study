package org.example.project.features.study

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.features.study.StudyQuestViewModel
import org.example.project.features.study.StudyQuestUiState
import org.example.project.features.study.StudySessionStatus
import org.example.project.features.study.StudySessionType
import org.example.project.features.study.StudyQuestIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyQuestScreenView(
    initialStudyMinutes: Int,
    onDismiss: () -> Unit
) {
    val viewModel = remember { StudyQuestViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(StudyQuestIntent.StartQuest(initialStudyMinutes))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.type == StudySessionType.STUDY) "冒険中" else "休憩中", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onIntent(StudyQuestIntent.StopQuest)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState.status == StudySessionStatus.FINISHED,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_transition"
        ) { isFinished ->
            if (isFinished) {
                ResultScreen(
                    uiState = uiState,
                    onNext = { viewModel.onIntent(StudyQuestIntent.NextSession) },
                    onExit = {
                        viewModel.onIntent(StudyQuestIntent.StopQuest)
                        onDismiss()
                    }
                )
            } else {
                MainQuestView(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    onTogglePause = { viewModel.onIntent(StudyQuestIntent.TogglePause) },
                    onFinishSession = { viewModel.onIntent(StudyQuestIntent.FinishSession) },
                    onExit = {
                        viewModel.onIntent(StudyQuestIntent.StopQuest)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun MainQuestView(
    paddingValues: PaddingValues,
    uiState: StudyQuestUiState,
    onTogglePause: () -> Unit,
    onFinishSession: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // タイマー表示
        Spacer(modifier = Modifier.height(40.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (uiState.isOvertime) "限界突破中! (延長)" else (if (uiState.type == StudySessionType.STUDY) "今回の特訓は..." else "リフレッシュ中"), 
                fontSize = 14.sp, 
                color = if (uiState.isOvertime) Color(0xFF9333EA) else Color.Gray
            )
            Text(
                text = uiState.displayTime,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = when {
                    uiState.isOvertime -> Color(0xFFDB2777)
                    uiState.type == StudySessionType.STUDY -> Color.Black
                    else -> Color(0xFF10B981)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 冒険の描写エリア
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .shadow(10.dp, RoundedCornerShape(32.dp)),
            color = if (uiState.isOvertime) Color(0xFFF3E8FF) else Color(0xFFF1F5F9),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (uiState.type == StudySessionType.STUDY) "🧙‍♂️" else "🏕️", 
                            fontSize = if (uiState.isOvertime) 90.sp else 80.sp
                        )
                        Text(if (uiState.type == StudySessionType.STUDY) "プレイヤー" else "キャンプ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Text(if (uiState.type == StudySessionType.STUDY) "VS" else "&&", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (uiState.type == StudySessionType.STUDY) Color.Red else Color.Blue)
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (uiState.type == StudySessionType.STUDY) "👾" else "🍵", 
                            fontSize = if (uiState.isOvertime) 90.sp else 80.sp
                        )
                        Text(if (uiState.type == StudySessionType.STUDY) "モンスター" else "お茶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                // ログ表示
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    if (uiState.isOvertime) {
                        Text(">> 超集中モード発動中！ <<", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                    }
                    uiState.currentLog.forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 14.sp,
                            color = if (uiState.isOvertime) Color(0xFF9333EA) else Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 操作ボタン
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
            if (uiState.type == StudySessionType.STUDY) {
                Button(
                    onClick = onFinishSession,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("休憩する (結果を見る)", fontWeight = FontWeight.Bold)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.status == StudySessionStatus.RUNNING) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (uiState.status == StudySessionStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Toggle")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.status == StudySessionStatus.RUNNING) "一時停止" else "再開する", fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("中止する", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResultScreen(
    uiState: StudyQuestUiState,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (uiState.type == StudySessionType.STUDY) "クエスト達成！" else "休憩完了！",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (uiState.type == StudySessionType.STUDY) Color(0xFFEF4444) else Color(0xFF10B981)
        )
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Surface(
            modifier = Modifier.size(200.dp),
            shape = CircleShape,
            color = if (uiState.type == StudySessionType.STUDY) Color(0xFFFFEDD5) else Color(0xFFDCFCE7)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (uiState.type == StudySessionType.STUDY) "🏁" else "✨", fontSize = 100.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Text(
            text = if (uiState.type == StudySessionType.STUDY) 
                "25分間の猛特訓に成功した！\n経験値と素材を手に入れたぞ。" 
            else 
                "リフレッシュできたようだ。\n次の冒険の準備はいいか？",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.type == StudySessionType.STUDY) Color(0xFF10B981) else Color(0xFF3B82F6)
            )
        ) {
            Text(if (uiState.type == StudySessionType.STUDY) "休憩を開始する" else "次の冒険へ出発する", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onExit) {
            Text("街に戻る", color = Color.Gray)
        }
    }
}
