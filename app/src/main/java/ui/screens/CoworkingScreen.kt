package com.example.tsu_navigator.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsu_navigator.R

data class CoworkingSpace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

val coworkingSpaces = listOf(
    CoworkingSpace("1", "Коворкинг в библиотеке ТГУ", 56.467774, 84.949390),
    CoworkingSpace("2", "Отдых в ГК", 56.469471, 84.947940),
    CoworkingSpace("3", "VK зона", 56.468728, 84.945227),
    CoworkingSpace("4", "Коворкинги в четвертом корпусе", 56.469814, 84.942192),
    CoworkingSpace("5", "Новый коворкинг от Альфа Банка", 56.467727, 84.949390)
)

@Composable
fun CoworkingScreen(
    onSpaceSelected: (CoworkingSpace) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .padding(top = 40.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.coworking_title),
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(coworkingSpaces) { space ->
                CoworkingCard(space, onClick = { onSpaceSelected(space) })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0039A6),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back), color = Color.White)
        }
    }
}

@Composable
fun CoworkingCard(space: CoworkingSpace, onClick: () -> Unit) {
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
            Text(text = "🏢", fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))

            Text(
                text = space.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text("→", fontSize = 20.sp, color = Color.Gray)
        }
    }
}