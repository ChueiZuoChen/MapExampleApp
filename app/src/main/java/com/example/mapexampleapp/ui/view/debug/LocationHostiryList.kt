package com.example.mapexampleapp.ui.view.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mapexampleapp.data.local.entity.LocationEntity

@Composable
fun LocationHistoryList(history: List<LocationEntity>) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Location store history"
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(text = "no record")
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(history) { item ->
                        LocationHistoryItem(item)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LocationHistoryItem(item: LocationEntity) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = item.timestamp.toString(),
            fontSize = 14.sp,
            color = Color.Gray
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "latitude: ${item.latitude}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "longitude: ${item.longitude}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
