package com.example.tsu_navigator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsu_navigator.EatPlace
import com.example.tsu_navigator.EatPlacesData
import com.example.tsu_navigator.PlaceType
import com.example.tsu_navigator.RatingRepository
import com.example.tsu_navigator.ml.AIClassifier
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun isOpenNow(workingHours: String): Boolean {
    return try {
        val parts = workingHours.split("-")
        if (parts.size != 2) return false

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val openTime = LocalTime.parse(parts[0], formatter)
        val closeTime = LocalTime.parse(parts[1], formatter)
        val now = LocalTime.now()
        if (closeTime.isBefore(openTime)) {
            now.isAfter(openTime) || now.isBefore(closeTime)
        } else {
            now.isAfter(openTime) && now.isBefore(closeTime)
        }
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhereEatScreen(
    onPlaceSelected: (EatPlace) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedType by remember { mutableStateOf<PlaceType?>(null) }
    var ratingTargetPlace by remember { mutableStateOf<EatPlace?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredPlaces = remember(selectedType) {
        if (selectedType != null) {
            EatPlacesData.places.filter { it.type == selectedType }
        } else {
            EatPlacesData.places
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Где поесть?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { selectedType = null },
                label = { Text("Все") }
            )
            FilterChip(
                selected = selectedType == PlaceType.CAFE,
                onClick = { selectedType = PlaceType.CAFE },
                label = { Text("Кафе") }
            )
            FilterChip(
                selected = selectedType == PlaceType.CANTEEN,
                onClick = { selectedType = PlaceType.CANTEEN },
                label = { Text("Столовые") }
            )
            FilterChip(
                selected = selectedType == PlaceType.SHOP,
                onClick = { selectedType = PlaceType.SHOP },
                label = { Text("Магазины") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Накидали подходящих вариков: ${filteredPlaces.size}",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPlaces) { place ->
                EatPlaceCard(
                    place = place,
                    onShowOnMap = { onPlaceSelected(place) },
                    onRate = { ratingTargetPlace = place }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("бэкап")
        }
    }

    if (ratingTargetPlace != null) {
        ModalBottomSheet(
            onDismissRequest = { ratingTargetPlace = null },
            sheetState = sheetState
        ) {
            RatingDrawerContent(
                placeName = ratingTargetPlace!!.name,
                onConfirm = { rating ->
                    ratingTargetPlace?.let { place ->
                        RatingRepository.addRating(place.id, rating)
                    }
                    ratingTargetPlace = null
                },
                onCancel = { ratingTargetPlace = null }
            )
        }
    }
}

@Composable
fun EatPlaceCard(place: EatPlace, onShowOnMap: () -> Unit, onRate: () -> Unit) {
    val isOpen = remember(place.workingHours) { isOpenNow(place.workingHours) }
    val avgRating = RatingRepository.getAverageRating(place.id)
    val ratingCount = RatingRepository.getRatingCount(place.id)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🍽️", fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(place.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    place.cuisine?.let { Text(it, fontSize = 14.sp, color = Color.Gray) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .then(
                                    if (isOpen) Modifier.background(Color(0xFF2E7D32), shape = RoundedCornerShape(5.dp))
                                    else Modifier.background(Color.Red, shape = RoundedCornerShape(5.dp))
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOpen) "Открыто сейчас" else "Закрыто сейчас",
                            fontSize = 12.sp,
                            color = if (isOpen) Color(0xFF2E7D32) else Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  •  🕐 ${place.workingHours}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", color = Color(0xFFFFD700), fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (ratingCount > 0) String.format("%.1f", avgRating) else "0.0",
                        fontWeight = FontWeight.Bold
                    )
                    if (ratingCount > 0) {
                        Text(
                            text = " ($ratingCount)",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Row {
                    TextButton(
                        onClick = onShowOnMap,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("На карте")
                    }
                    Button(
                        onClick = onRate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0039A6),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Оценить", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDrawerContent(
    placeName: String,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val aiClassifier = remember { AIClassifier(context) }
    val gridSize = 5
    val pixels = remember { Array(gridSize) { BooleanArray(gridSize) } }

    var recognizedDigit by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Оцените \"$placeName\"",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Нарисуйте цифру от 0 до 9",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.LightGray)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val position = change.position
                        val cellSize = size.width / gridSize
                        val col = (position.x / cellSize).toInt().coerceIn(0, gridSize - 1)
                        val row = (position.y / cellSize).toInt().coerceIn(0, gridSize - 1)
                        pixels[row][col] = true
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / gridSize
                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        if (pixels[row][col]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )
                        }
                    }
                }
                for (i in 1 until gridSize) {
                    val coord = i * cellSize
                    drawLine(
                        color = Color.Black,
                        start = Offset(coord, 0f),
                        end = Offset(coord, size.height),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, coord),
                        end = Offset(size.width, coord),
                        strokeWidth = 2f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recognizedDigit == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        for (row in 0 until gridSize) {
                            for (col in 0 until gridSize) {
                                pixels[row][col] = false
                            }
                        }
                    }
                ) {
                    Text("Очистить")
                }
                Button(
                    onClick = {
                        val predicted = aiClassifier.predict(pixels)
                        recognizedDigit = predicted
                    }
                ) {
                    Text("Распознать")
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Распознанная цифра: $recognizedDigit",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirm(recognizedDigit!!)
                            recognizedDigit = null
                        }
                    ) {
                        Text("Подтвердить")
                    }
                    OutlinedButton(
                        onClick = {
                            recognizedDigit = null
                        }
                    ) {
                        Text("Изменить")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Отмена")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}