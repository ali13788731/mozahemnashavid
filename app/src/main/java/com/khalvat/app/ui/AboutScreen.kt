package com.khalvat.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    val transition = rememberInfiniteTransition(label = "aboutPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "aboutPulseValue"
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        KhalvatLogo(Modifier.size(150.dp), pulse = pulse)
        Text("خلوت", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "نسخه ${"1.0".faDigits()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GradientCard(Modifier.fillMaxWidth()) {
            Text(
                "طراحی شده توسط Mr.PC",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
        }

        AppCard(Modifier.fillMaxWidth()) {
            Text("چطور کار می‌کند؟", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "• تماس: برنامه به‌عنوان «غربالگر تماس» ثبت می‌شود و هنگام رسیدن تماس، قوانین را می‌سنجد. " +
                    "تماس بی‌صدا یا رد می‌شود، اعلانش پنهان می‌شود و طبق انتخاب شما از تاریخچه گوشی حذف یا در آرشیو ذخیره می‌شود.\n\n" +
                    "• پیامک: اعلان پیامک‌های انتخاب‌شده پنهان می‌شود و در صورت انتخاب، یک کپی در آرشیو رمزنگاری‌شده ذخیره می‌شود. " +
                    "خود پیامک در برنامه پیامک گوشی باقی می‌ماند.\n\n" +
                    "• همه اطلاعات روی همین گوشی و رمزنگاری‌شده می‌ماند و جایی ارسال نمی‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AppCard(Modifier.fillMaxWidth()) {
            Text("نکته‌های مهم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "• اپراتور ممکن است پیامک «تماس بی‌پاسخ» بفرستد که از کنترل برنامه خارج است.\n" +
                    "• ساعت هوشمند یا دستگاه‌های متصل ممکن است تماس را نشان بدهند.\n" +
                    "• هنگام پنهان شدن اعلان پیامک ممکن است صدای اعلان یک لحظه شنیده شود.\n" +
                    "• بعضی گوشی‌ها برنامه‌های پس‌زمینه را می‌بندند؛ بهینه‌سازی باتری را برای خلوت خاموش کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
