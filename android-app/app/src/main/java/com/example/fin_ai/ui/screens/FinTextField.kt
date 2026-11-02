package com.example.fin_ai.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fin_ai.ui.theme.VioletPrimary

/** The dark, violet-focused field style shared by the login and ledger forms. */
@Composable
fun FinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    numeric: Boolean = false,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Decimal)
        } else {
            KeyboardOptions.Default
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.04f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
            focusedBorderColor = VioletPrimary.copy(alpha = 0.55f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.09f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = VioletPrimary,
            focusedPlaceholderColor = Color(0xFF52525B),
            unfocusedPlaceholderColor = Color(0xFF52525B),
        ),
        modifier = modifier.fillMaxWidth()
    )
}
