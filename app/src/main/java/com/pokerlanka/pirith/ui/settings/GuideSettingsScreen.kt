package com.pokerlanka.pirith.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideSettingsScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "භාවිත මාර්ගෝපදේශය (Guide)",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuideCard(
                icon = Icons.Default.OfflinePin,
                title = "100% නොබැඳි භාවිතය (Offline)",
                description = "මෙම යෙදුම සඳහා අන්තර්ජාල සම්බන්ධතාවයක් කිසිසේත් අවශ්‍ය නොවේ. සියලුම පිරිත් දේශනා, සූත්‍ර, ගාථා සහ ඒවායේ සම්පූර්ණ සිංහල තේරුම ඔබගේ දුරකථනය තුළම අන්තර්ගත SQLite දත්ත ගබඩාවක සුරක්ෂිතව තැන්පත් කර ඇත."
            )

            GuideCard(
                icon = Icons.Default.Search,
                title = "සෙවීම සහ වර්ගීකරණය (Search & Filter)",
                description = "ප්‍රධාන පිටුවේ ඇති සෙවුම් තීරුව (Search Bar) මගින් ඕනෑම පිරිතක නම සිංහලෙන් හෝ ඉංග්‍රීසියෙන් ලියා ක්ෂණිකව සොයාගත හැක. එසේම 'මහා පිරිත්', 'සෙත් පිරිත්', 'වන්දනා', 'පට්ඨාන', සහ 'විශේෂ සූත්‍ර' ආදී කාණ්ඩ අනුවද පෙරා බැලිය හැක."
            )

            GuideCard(
                icon = Icons.Default.Speed,
                title = "ස්වයංක්‍රීය අනුචලනය (Auto-scroll)",
                description = "පිරිත් කියවන අතරතුර පහළ ඇති Floating Play බොත්තම එබීමෙන් ස්වයංක්‍රීයව පහළට සෙමෙන් Scroll වීම ආරම්භ වේ. එහි වේගය 0.25x, 0.5x, 0.75x, 1.0x ආදී වශයෙන් ඔබට පහසු පරිදි වෙනස් කරගත හැක."
            )

            GuideCard(
                icon = Icons.Default.FontDownload,
                title = "අකුරු ප්‍රමාණය සහ පාලි/සිංහල තේරීම",
                description = "කියවීම් පිටුවේ ඉහළ ඇති A- / A+ මගින් ඕනෑම මොහොතක අකුරු ප්‍රමාණය සකසා ගත හැක. එසේම පාලි පමණක්, සිංහල පමණක් හෝ සමාන්තරව (Parallel) දෙකම පෙන්වන ආකාරයට වෙනස් කරගත හැක."
            )

            GuideCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "ප්‍රියතම පිරිත් ලැයිස්තුව (Favorites)",
                description = "ඔබ නිතර සජ්ඣායනා කරන හෝ ශ්‍රවණය කරන පිරිත් සූත්‍ර ඉදිරියේ ඇති හෘදයාකාර (Heart) ලකුණ click කිරීමෙන් ඔබගේ ප්‍රියතම ලැයිස්තුවට එක් කර පහසුවෙන් ලබාගත හැක."
            )
        }
    }
}

@Composable
private fun GuideCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.padding(start = 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }
    }
}
