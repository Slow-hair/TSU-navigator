package com.example.tsu_navigator.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsu_navigator.EatPlace
import com.example.tsu_navigator.EatPlacesData
import com.example.tsu_navigator.PlaceType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
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
@Composable
fun WhereEatScreen(
    onPlaceSelected: (EatPlace) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedType by remember { mutableStateOf<PlaceType?>(null) }

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
        Text(
            text = "Где поесть???",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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
                EatPlaceCard(place, onClick = { onPlaceSelected(place) })
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
}

@Composable
fun EatPlaceCard(place: EatPlace, onClick: () -> Unit) {
    val isOpen = remember(place.workingHours) { isOpenNow(place.workingHours) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

            Text("→", fontSize = 20.sp, color = Color.Gray)
        }
    }
}