package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.util.SypFormatter

interface BudgetNotifier {
    fun notifyThreshold(
        budgetId: Long,
        categoryNameAr: String,
        thresholdPercent: Int,
        spentAmount: Long,
        limitAmount: Long,
        actualPercent: Int
    )
}

class BudgetNotificationHelper(private val context: Context) : BudgetNotifier {

    companion object {
        const val CHANNEL_ID = "masroofy_budget_alerts"
    }

    fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.budget_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.budget_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun notifyThreshold(
        budgetId: Long,
        categoryNameAr: String,
        thresholdPercent: Int,
        spentAmount: Long,
        limitAmount: Long,
        actualPercent: Int
    ) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = if (thresholdPercent >= 100) {
            "تجاوزت ميزانية $categoryNameAr"
        } else {
            "اقتربت من ميزانية $categoryNameAr"
        }

        val body = if (thresholdPercent >= 100) {
            "تم استهلاك $actualPercent% (${SypFormatter.format(spentAmount)} من أصل ${SypFormatter.format(limitAmount)})"
        } else {
            "وصلت إلى $actualPercent% من الميزانية (${SypFormatter.format(spentAmount)} من ${SypFormatter.format(limitAmount)})"
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            budgetId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (budgetId * 1000L + thresholdPercent).toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
