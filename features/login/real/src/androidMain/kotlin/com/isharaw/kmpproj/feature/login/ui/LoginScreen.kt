package com.isharaw.kmpproj.feature.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.isharaw.kmpproj.core.Session
import com.isharaw.kmpproj.feature.login.Authenticator
import com.isharaw.kmpproj.feature.login.LoginValidator

@Composable
fun LoginScreen(
    validator: LoginValidator,
    authenticator: Authenticator,
    onLoginSuccess: (Session) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val validationError = validator.validate(email, password)
                if (validationError == null) {
                    onLoginSuccess(authenticator.authenticate(email, password))
                } else {
                    error = validationError
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log in")
        }
    }
}
