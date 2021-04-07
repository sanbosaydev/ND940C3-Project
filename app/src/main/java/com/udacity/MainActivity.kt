package com.udacity

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.udacity.utils.createChannel
import com.udacity.utils.sendNotification
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0
    private var selectedGitHubDescription: String? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var downloadManager: DownloadManager
    private lateinit var loadingButton: LoadingButton
    private var downloadState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        loadingButton = findViewById(R.id.loadingButton)
        loadingButton.setLoadingButtonState(ButtonState.Completed)

        loadingButton.setOnClickListener {
            when {
                rb_glide.isChecked -> {
                    selectedGitHubDescription = getString(R.string.glide_text)
                    displayToast(getString(R.string.glide_message))
                    downloadFile(URL_GLIDE)
                }
                rb_loadApp.isChecked -> {
                    selectedGitHubDescription = getString(R.string.load_app_text)
                    displayToast(getString(R.string.load_app_message))
                    downloadFile(URL_LOAD_APP)
                }
                rb_retrofit.isChecked -> {
                    selectedGitHubDescription = getString(R.string.retrofit_text)
                    displayToast(getString(R.string.retrofit_message))
                    downloadFile(URL_RETROFIT)
                }
                else -> {
                    loadingButton.setLoadingButtonState(ButtonState.Completed)
                    displayToast(getString(R.string.no_selected_item))
                }
            }

        }

        createChannel(
            getString(R.string.github_notification_channel_id),
            getString(R.string.github_notification_channel_name)
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Log.i("statusDownload", id.toString())


            val query = downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
            if (query.moveToFirst()) {
                val status: Int = query.getInt(query.getColumnIndex(DownloadManager.COLUMN_STATUS))
                Log.i("statusDownload", status.toString())

                when (status) {
                    DownloadManager.STATUS_FAILED -> {
                        Log.i("statusDownload", "Failed")
                        downloadState = false
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.i("statusDownload", "Finish")
                        downloadState = true
                    }
                }
                sendNotification()
                query.close()
            }
        }
    }

    private fun downloadFile(url: String) {
        loadingButton.setLoadingButtonState(ButtonState.Loading)

        val file = File(getExternalFilesDir(null), "/repos")

        if (!file.exists()) {
            file.mkdirs()
        }

        try {
            val request =
                DownloadManager.Request(Uri.parse(url))
                    .setTitle(getString(R.string.app_name))
                    .setDescription(getString(R.string.app_description))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "/repos/github_repository.zip"
                    )

            downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadID =
                downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        } catch (e: Exception) {
            downloadState = false
            sendNotification()
            Log.i("statusDownload", "FAILED")
        }
    }

    private fun sendNotification() {
        notificationManager = ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.sendNotification(
            getString(R.string.notification_description),
            applicationContext,
            selectedGitHubDescription.toString(),
            downloadState
        )
        radiogroup.clearCheck()
        loadingButton.setLoadingButtonState(ButtonState.Completed)
        Log.i("statusDownload", "Send notification")
    }


    private fun displayToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val URL_RETROFIT = "https://github.com/square/retrofit/archive/master.zip"
        private const val URL_LOAD_APP =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val URL_GLIDE = "https://github.com/bumptech/glide/archive/master.zip"
    }
}
