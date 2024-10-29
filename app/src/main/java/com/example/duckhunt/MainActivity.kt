package com.example.duckhunt

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var shotsLivesText: TextView
    private lateinit var reloadText: TextView
    private lateinit var highscoreText: TextView
    private lateinit var gameOverText: TextView
    private lateinit var youWinText: TextView
    private var shotsLeft: Int = 5
    private var highscore: Int = 0
    private var lives: Int = 3
    private var nfcTagIndex: Int = 0
    private val nfcTags = listOf("37:EA:63:43", "C7:B9:90:43", "97:7D:2A:4B", "37:D5:39:4B")
    private var isReloading = false
    private lateinit var alienList: MutableList<ImageView>
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var headerLayout: ConstraintLayout
    private var gameOver = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        shotsLivesText = findViewById(R.id.shotsLivesText)
        reloadText = findViewById(R.id.reloadText)
        highscoreText = findViewById(R.id.highscoreText)
        gameOverText = findViewById(R.id.gameOverText)
        youWinText = findViewById(R.id.youWinText)
        rootLayout = findViewById(R.id.rootLayout)
        headerLayout = findViewById(R.id.headerLayout)

        headerLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                headerLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                spawnAliens()
            }
        })

        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!gameOver) {
                    handleScreenTap()
                }
            }
            true
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported or disabled", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        } else {
            checkNfcEnabled()
        }
    }

    private fun checkNfcEnabled() {
        if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, "NFC is disabled! Please enable it.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }
    }

    private fun spawnAliens() {
        alienList = mutableListOf()
        val headerHeight = headerLayout.height

        // Set difficulty based on highscore
        val alienSize = when (highscore) {
            in 0..4 -> 180
            in 5..9 -> 150
            in 10..14 -> 120
            else -> 30
        }

        val alienSpeed = when (highscore) {
            in 0..4 -> 3000L
            in 5..9 -> 2500L
            in 10..14 -> 2000L
            else -> 500L
        }

        for (i in 1..5) {
            val alien = ImageView(this)
            alien.setImageResource(R.drawable.alien)
            alien.layoutParams = ConstraintLayout.LayoutParams(alienSize, alienSize)
            alien.x = Random.nextFloat() * (resources.displayMetrics.widthPixels - alienSize)
            alien.y = headerHeight + Random.nextFloat() * (resources.displayMetrics.heightPixels - headerHeight - 300)
            alien.setOnClickListener { shootAlien(alien) }
            alienList.add(alien)
            rootLayout.addView(alien)

            moveAlienRandomly(alien, alienSpeed)
        }
    }

    private fun moveAlienRandomly(alien: ImageView, speed: Long) {
        val alienAnimator = ObjectAnimator.ofFloat(
            alien,
            "translationX",
            alien.x,
            Random.nextFloat() * resources.displayMetrics.widthPixels
        )
        alienAnimator.duration = speed
        alienAnimator.repeatCount = ObjectAnimator.INFINITE
        alienAnimator.repeatMode = ObjectAnimator.REVERSE
        alienAnimator.start()
    }

    private fun shootAlien(view: ImageView) {
        if (shotsLeft > 0 && !gameOver) {
            shotsLeft -= 1
            updateShotsLivesText()

            rootLayout.removeView(view)
            alienList.remove(view)

            highscore += 1
            highscoreText.text = "Highscore: $highscore"

            if (highscore >= 20) {
                showYouWinText()
                gameOver = true
            }

            if (alienList.isEmpty()) {
                showReloadText()
            }
        }

        checkForGameOver()
    }

    private fun handleScreenTap() {
        if (shotsLeft > 0) {
            shotsLeft -= 1
            updateShotsLivesText()
        } else {
            Toast.makeText(this, "Out of shots! Reload required!", Toast.LENGTH_SHORT).show()
        }

        checkForGameOver()
    }

    private fun updateShotsLivesText() {
        shotsLivesText.text = "Shots: $shotsLeft | Lives: $lives"
    }

    private fun checkForGameOver() {
        if (shotsLeft == 0 && alienList.isNotEmpty()) {
            loseLives()
        }
    }

    private fun loseLives() {
        val remainingAliens = alienList.size
        lives -= remainingAliens
        Toast.makeText(this, "Lost $remainingAliens lives! Lives left: $lives", Toast.LENGTH_LONG).show()

        if (lives <= 0) {
            showGameOverText()
            gameOver = true
            alienList.clear()
            rootLayout.removeAllViews()
        } else {
            showReloadText()
        }
    }

    private fun resetGame() {
        lives = 3
        highscore = 0
        shotsLeft = 5
        gameOver = false // Reset the game over state
        updateShotsLivesText()
        highscoreText.text = "Highscore: $highscore"

        alienList.clear()
        rootLayout.removeAllViews()

        spawnAliens()
    }

    private fun showGameOverText() {
        gameOverText.visibility = View.VISIBLE
    }

    private fun showYouWinText() {
        youWinText.visibility = View.VISIBLE
    }

    private fun showReloadText() {
        reloadText.visibility = View.VISIBLE
        val flashAnimator = ObjectAnimator.ofFloat(reloadText, "alpha", 0f, 1f)
        flashAnimator.duration = 500
        flashAnimator.repeatCount = ValueAnimator.INFINITE
        flashAnimator.repeatMode = ObjectAnimator.REVERSE
        flashAnimator.start()
        isReloading = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (isReloading && !gameOver) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

            if (tag == null) {
                Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
                return
            }

            val tagId = tag.id.joinToString(":") { String.format("%02X", it) }

            if (tagId == nfcTags.getOrNull(nfcTagIndex)) {
                nfcTagIndex += 1
                shotsLeft = 5
                updateShotsLivesText()
                reloadText.visibility = View.GONE
                isReloading = false

                //respawn function
                spawnAliens()

                if (nfcTagIndex >= nfcTags.size) {
                    reloadText.text = "Congratulations! You scanned all NFC tags!"
                }
            } else {
                Toast.makeText(this, "Wrong tag! Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
}
