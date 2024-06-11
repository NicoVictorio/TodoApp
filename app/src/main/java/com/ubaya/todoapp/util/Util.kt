package com.ubaya.todoapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ubaya.todoapp.model.TodoDatabase
import com.ubaya.todoapp.view.MainActivity
import com.ubaya.todoapp.R


val DB_NAME = "newtododb"

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE todo ADD COLUMN priority INTEGER DEFAULT 3 not null")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE todo ADD COLUMN is_done INTEGER DEFAULT 0 not null")
    }
}
//fun buildDb(context:Context):TodoDatabase{
//    val db = TodoDatabase.buildDatabase(context)
//    return db
//}

fun buildDb(context: Context):TodoDatabase {
    val db = Room.databaseBuilder(context,
        TodoDatabase::class.java, DB_NAME)
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .build()

    return db
}

class NotificationHelper(val context: Context){
    private val CHANNEL_ID = "todo_channel"
    private val NOTIFICATION_ID = 1

    companion object{
        val REQUEST_NOTIF = 100
    }

    private fun createNotificationChannel(){
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Channel to publish a notification when todo created"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun createNotification(title:String, message:String) {
        createNotificationChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context,0, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val icon = BitmapFactory.decodeResource(context.resources, R.drawable.todochar)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.checklist)
            .setLargeIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(icon)
                    .bigLargeIcon(null)
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            try {
                NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID, notification)
            } catch (e:SecurityException) {
                Log.e("error notif: ", e.toString())
            }
    }
}

class TodoWorker(context: Context, params:WorkerParameters): Worker(context,params) {
    override fun doWork(): Result {
        val title = inputData.getString("title").toString()
        val message = inputData.getString("message").toString()
        NotificationHelper(applicationContext).createNotification(title, message)
        return Result.success()
    }
}
