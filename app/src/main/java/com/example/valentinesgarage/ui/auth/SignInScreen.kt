package com.example.valentinesgarage.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SignInScreen(onSignIn: (String, String, (String?) -> Unit) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Valentine's Garage", style = MaterialTheme.typography.headlineLarge)
        Text("Sign in to your workshop dashboard", modifier = Modifier.padding(bottom = 24.dp))
        OutlinedTextField(email, { email = it; error = null }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { onSignIn(email, password) { error = it } }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp), enabled = email.isNotBlank() && password.isNotBlank()) { Text("Sign in") }
    }
}
