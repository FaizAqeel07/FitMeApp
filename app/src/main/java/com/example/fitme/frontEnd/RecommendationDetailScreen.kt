package com.example.fitme.frontEnd

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.fitme.database.Recommendation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationDetailScreen(
    recommendation: Recommendation,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // GIF Section - Improved Layout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 350.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (recommendation.gifUrl.isNotEmpty()) {
                        AsyncImage(
                            model = recommendation.gifUrl,
                            contentDescription = recommendation.title,
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth // Biar pas secara lebar
                        )
                    } else {
                        Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Modern Stats Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DetailIconInfo(Icons.Default.FitnessCenter, "Target", recommendation.target)
                        VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.2f))
                        DetailIconInfo(Icons.Default.Layers, "Equipment", recommendation.equipment)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Instructions with better spacing
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DetailIconInfo(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(130.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}
