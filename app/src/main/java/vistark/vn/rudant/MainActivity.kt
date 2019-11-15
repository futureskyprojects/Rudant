package vistark.vn.rudant

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.ant_object_layout.*
import kotlinx.android.synthetic.main.overlay_layout.*
import kotlinx.android.synthetic.main.playing_layout.*
import java.util.*
import kotlin.random.Random
import android.os.StrictMode
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd


class MainActivity : AppCompatActivity() {
    private val MAX_TURN: Int = 30
    private val MAX_WAIT_FOR_JOKE = 10000
    private val HIGH_SCORE_KEY = "HIGH_SCORE_KEY"
    private val TIME_USED = "TIME_USED"
    private val HIGH_SCORE_COLOR = Color.parseColor("#f1c40f")
    private val NEW_HIGH_ANNOUCEMENT = "NEW HIGH"
    private val IS_CAN_PLAY_MUSIC_KEY = "IS_CAN_PLAY_MUSIC_KEY"
    private val ADS_UNIT_ID = "ca-app-pub-1930494588356648/1274417407"

    // Init once
    var isStarted = false
    var isPressStartButton = false
    var isGamePlaying = false
    var sWidth = -1
    var sHeight = -1
    var isDie = false
    var isJoking = false
    lateinit var gameIntroMusic: MediaPlayer
    lateinit var gameEndMusic: MediaPlayer
    lateinit var gameBackgroundMusic: MediaPlayer
    lateinit var gameSoundJoke: MediaPlayer
    lateinit var defaultTap: MediaPlayer
    lateinit var successTap: MediaPlayer
    lateinit var gameSoundFast: MediaPlayer
    private lateinit var vb: Vibrator
    private var timerMovingObject = Timer()
    private lateinit var startIconOverlayAnimation: Animator
    private var waringTurnLeftAnimation: Animator? = null
    private var antMovingAnimator: ObjectAnimator? = null


    // Dynamic
    private var highScore = 0
    private var timeUsedOfHighScore = ""
    private var score = 0
    var mDuration = 1000.toLong()
    var turnLeft = MAX_TURN
    var startMillis = (-1000).toLong()
    private var timerCountPlaying = Timer()
    var firstGraphicStartMillis = (-1000).toLong()
    var currentGraphics: Int = R.drawable.ant
    var lastPressMillis = (-1000).toLong()
    var timerJoke = Timer()
    var isCanPlayMusic = true

    // For ads
    private lateinit var mInterstitialAd: InterstitialAd

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAds()
        hackWays()
        initSound()
        getSettings()
        initVibrate()
        initScreenSize()
        initExitButton()
        initShareFacebookButton()
        initBackgroundTap()
        initAntTap()
        initTextViewEndGame()
        initOverlayScreen()
        initSoundControlTap()
        getHighScore()
        updateTurnLeft(MAX_TURN)
    }

    private fun initAds() {
        MobileAds.initialize(this) {}

        mInterstitialAd = InterstitialAd(this).apply {
            adUnitId = ADS_UNIT_ID
            adListener = (object : AdListener() {
                override fun onAdLoaded() {
                    ivStartIcon.isEnabled = false
                    if (mInterstitialAd.isLoaded && !isGamePlaying) {
                        mInterstitialAd.show()
                    }
                }

                override fun onAdFailedToLoad(p0: Int) {
                    ivStartIcon.isEnabled = true
                }

                override fun onAdClosed() {
                    ivStartIcon.isEnabled = true
                }
            })
        }
    }

    private fun hackWays() {
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun initShareFacebookButton() {
        ivShare.setOnClickListener {
            val path = Screenshot().take(this)
//            ivStartIcon.setImageBitmap(Screenshot().getScreenshotBitmap(path))
            ShareUtils().share(this, path, highScore, timeUsedOfHighScore)
        }
    }

    private fun initSoundControlTap() {
        ivSoundControl.setOnClickListener {
            isCanPlayMusic = !isCanPlayMusic
            println(isCanPlayMusic.toString())
            setSettings()
        }
    }

    private fun initExitButton() {
        ivExitButton.setOnClickListener {
            endGame()
            finish()
        }
    }

    private fun initTextViewEndGame() {
        lnLongPressToStop.setOnLongClickListener {
            showReplay()
            return@setOnLongClickListener true
        }
    }

    private fun startTimerJoke() {
        timerJoke = Timer()
        timerJoke.schedule(object : TimerTask() {
            override fun run() {
                if (lastPressMillis < 0) {
                    lastPressMillis = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - lastPressMillis >= Random.nextInt(
                        2000,
                        MAX_WAIT_FOR_JOKE
                    )
                ) {
                    isJoking = true
                    playJokeSound()
                }
            }
        }, 100, 100)
    }

    private fun startTurnWarningAnimation() {
        if ((waringTurnLeftAnimation != null && !waringTurnLeftAnimation!!.isRunning) || waringTurnLeftAnimation == null) {
            with(AnimatorInflater.loadAnimator(this, R.animator.shake_warning) as AnimatorSet)
            {
                waringTurnLeftAnimation = this
                setTarget(tvTurnLeft)
                duration = 400
                start()
            }
        }
    }

    private fun initOverlayScreen() {
        ivStartIcon.setOnClickListener {
            isPressStartButton = true
            isGamePlaying = true
            playTabSuccess()
            vibrate()
            startGame()
            startTimerJoke()
        }
        startIconOverlayAnimation =
            AnimatorInflater.loadAnimator(this, R.animator.jump_and_blink) as AnimatorSet
        startIconOverlayAnimation.setTarget(ivStartIcon)

        startIconOverlayAnimation.duration = 2000

        if (isCanPlayMusic) {
            gameIntroMusic.seekTo(0)
            gameIntroMusic.start()
        }
        startOverlayAnimation()
    }

    private fun startOverlayAnimation() {
        startIconOverlayAnimation.start()
    }

    private fun showReplay() {
        if (rlOverlay.visibility != View.VISIBLE) {
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
            endGame()
            showEndOverlayScreen(
                tvScore.text.toString().replace("\n", " "),
                "Time used: ${tvPlayingTime.text}"
            )
            clearGame()
        }

        if (isCanPlayMusic) {
            gameEndMusic.seekTo(0)
            gameEndMusic.start()
        }
    }

    private fun muteAllMusic() {
        if (gameBackgroundMusic.isPlaying)
            gameBackgroundMusic.pause()
        gameBackgroundMusic.seekTo(0)

        if (gameSoundFast.isPlaying)
            gameSoundFast.pause()
        gameSoundFast.seekTo(0)

        if (successTap.isPlaying)
            successTap.pause()
        successTap.seekTo(0)

        if (gameIntroMusic.isPlaying)
            gameIntroMusic.pause()
        gameIntroMusic.seekTo(0)

        if (defaultTap.isPlaying)
            defaultTap.pause()
        defaultTap.seekTo(0)

        if (gameSoundJoke.isPlaying)
            gameSoundJoke.pause()
        gameSoundJoke.seekTo(0)

        if (gameEndMusic.isPlaying)
            gameEndMusic.pause()
        gameEndMusic.seekTo(0)
    }

    private fun endGame() {
        isGamePlaying = false

        rlAnt.visibility = View.GONE

        if (gameBackgroundMusic.isPlaying)
            gameBackgroundMusic.pause()
        gameBackgroundMusic.seekTo(0)

        if (gameSoundFast.isPlaying)
            gameSoundFast.pause()
        gameSoundFast.seekTo(0)

        if (gameIntroMusic.isPlaying)
            gameIntroMusic.pause()
        gameIntroMusic.seekTo(0)
    }

    @SuppressLint("SetTextI18n")
    private fun showEndOverlayScreen(gotScore: String, time: String) {
        var gotScoreGet: String = gotScore.replace("\n", "").trim()
        if (gotScoreGet.contains("($NEW_HIGH_ANNOUCEMENT)")) {
            tvHighScore.setTextColor(HIGH_SCORE_COLOR)
            gotScoreGet = "NEW HIGH " + gotScoreGet.replace("($NEW_HIGH_ANNOUCEMENT)", "")
        } else {
            tvHighScore.setTextColor(Color.WHITE)
        }
        ivStartIcon.post {
            ivStartIcon.setImageResource(R.drawable.ant_again)
            tvHighScore.text = gotScoreGet
            tvTimeUsed.text = time
            rlOverlay.visibility = View.VISIBLE
            startOverlayAnimation()
        }
    }

    private fun hideOverlayScreen() {
        rlOverlay.visibility = View.GONE
        if (gameIntroMusic.isPlaying)
            gameIntroMusic.pause()

        if (gameEndMusic.isPlaying)
            gameEndMusic.pause()
    }

    private fun clearGame() {
        score = 0
        mDuration = 1000.toLong()
        startMillis = (-1000).toLong()
        lastPressMillis = (-1000).toLong()
        timerCountPlaying.cancel()
        timerMovingObject.cancel()
        timerJoke.cancel()

        muteAllMusic()

        rlAnt.post {
            rlAnt.layoutParams.height = convertDpToPx(this, 120F).toInt()
            rlAnt.layoutParams.width = rlAnt.layoutParams.height
        }
        updateScore()
        updatePlayingTimeCounter(0, 0, 0)
        updateTurnLeft(MAX_TURN)
    }

    private fun startGame() {
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        getHighScore()
        hideOverlayScreen()
        startMovingAnt()
        startBackgroundMusic()
        startCountPlayingTime()
        randomAntPos()
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vb.vibrate(VibrationEffect.createOneShot(300, -1))
        } else {
            vb.vibrate(300)
        }
    }

    private fun updateDieGraphics() {
        val randInt = Random.nextInt(4)
        rlAnt.setBackgroundResource(
            resources.getIdentifier("ant_die_$randInt", "drawable", packageName)
        )
        Timer().schedule(object : TimerTask() {
            override fun run() {
                rlAnt.post {
                    rlAnt.visibility = View.GONE
                    rlAnt.setBackgroundResource(R.drawable.ant)
                }
            }
        }, 300)
    }

    private fun initAntTap() {
        rlTap.setOnClickListener {
            if (!isGamePlaying)
                return@setOnClickListener
            it.isEnabled = false
            isDie = true
            if (gameSoundJoke.isPlaying)
                gameSoundJoke.pause()
            updateDieGraphics()
            score++
            playTabSuccess()
            mDuration -= (mDuration * 0.02).toInt()

            vibrate()

            if (rlAnt.layoutParams.height > convertDpToPx(this, 50.toFloat())) {
                rlAnt.layoutParams.height -= (rlAnt.layoutParams.height * 0.02).toInt()
                rlAnt.layoutParams.width = rlAnt.layoutParams.height
            }
            ValueAnimator.ofInt(1).apply {
                duration = 500
                addUpdateListener {
                    updateScore()
                }
                start()
            }
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    randomAntPos()
                }
            }, 500)
            tapCheck(false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScore() {
        tvScore.post {
            val sb = SpannableStringBuilder()
                .append("SCORE: $score")
            if (score > highScore) {
                startBackgroundFastMusic()
                setHighScore()
                with(sb) {
                    append("\n($NEW_HIGH_ANNOUCEMENT)")
                    setSpan(
                        AbsoluteSizeSpan(
                            convertDpToPx(this@MainActivity, 8F).toInt()
                            ,
                            true
                        ),
                        sb.indexOf("\n"),
                        sb.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE

                    )
                    setSpan(
                        ForegroundColorSpan(HIGH_SCORE_COLOR),
                        sb.indexOf("\n"),
                        sb.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }
            tvScore.text = sb
        }
    }

    private fun tapCheck(decreaseTurn: Boolean = true) {
        if (isGamePlaying && isStarted) {
            lastPressMillis = System.currentTimeMillis()
            if (decreaseTurn)
                updateTurnLeft(turnLeft - 1)
            if (turnLeft <= 0) {
                showReplay()
            }
        }
    }

    private fun initBackgroundTap() {
        lnLongPressToStop.setOnClickListener {
            rlRoot.performClick()
        }
        rlRoot.setOnClickListener {
            playTabDefault()
            if (isGamePlaying && !isDie) {
                isJoking = true
                tapCheck()
                playJokeSound()
            }
        }
    }

    private fun startCountPlayingTime() {
        timerCountPlaying = Timer()
        timerCountPlaying.schedule(object : TimerTask() {
            override fun run() {
                if (startMillis < 0) {
                    startMillis = System.currentTimeMillis()
                } else {
                    val milliseconds = System.currentTimeMillis() - startMillis
                    val seconds = (milliseconds / 1000).toInt() % 60
                    val minutes = (milliseconds / (1000 * 60) % 60).toInt()
                    val hours = (milliseconds / (1000 * 60 * 60) % 24).toInt()

                    updatePlayingTimeCounter(hours, minutes, seconds)
                }
            }
        }, 1000, 1000)
    }

    private fun updatePlayingTimeCounter(hours: Int, minutes: Int, seconds: Int) {
        tvPlayingTime.post {
            tvPlayingTime.text =
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private fun startBackgroundMusic() {
        if (isCanPlayMusic) {
            if (gameSoundFast.isPlaying) {
                gameSoundFast.pause()
            }
            if (!gameBackgroundMusic.isPlaying) {
                gameBackgroundMusic.seekTo(0)
                gameBackgroundMusic.start()
            }
        }
    }

    private fun startBackgroundFastMusic() {
        if (isCanPlayMusic) {
            if (gameBackgroundMusic.isPlaying) {
                gameBackgroundMusic.pause()
            }
            if (!gameSoundFast.isPlaying) {
                gameSoundFast.seekTo(0)
                gameSoundFast.start()
            }
        }
    }

    private fun startMovingAnt() {
        timerMovingObject = Timer()
        timerMovingObject.schedule(object : TimerTask() {
            override fun run() {
                if ((antMovingAnimator != null && !antMovingAnimator!!.isRunning && rlAnt.visibility == View.VISIBLE) || antMovingAnimator == null || (antMovingAnimator != null && !antMovingAnimator!!.isStarted)) {
                    val x = Random.nextInt(10)
                    val des: Int
                    if (x % 2 == 0) {
                        des = Random.nextInt(-rlAnt.width / 2, sWidth - rlAnt.width / 2)
                    } else {
                        des = Random.nextInt(-rlAnt.height / 2, sHeight - rlAnt.height / 2)
                    }
                    runOnUiThread {
                        antMovingAnimator = ObjectAnimator.ofFloat(
                            rlAnt,
                            if (x % 2 == 0) "translationX" else "translationY",
                            des.toFloat()
                        )
                            .apply {
                                duration = mDuration
                                start()
                            }
                    }
                }
                updateMovementGraphics()
            }
        }, 10, 10)
    }

    private fun updateMovementGraphics() {
        if (currentGraphics == R.drawable.ant_r_joke)
            isJoking = false
        if (System.currentTimeMillis() - firstGraphicStartMillis >= mDuration / 6 && !isDie) {
            rlAnt.post {
                if (!isJoking) {
                    currentGraphics = when (currentGraphics) {
                        R.drawable.ant -> R.drawable.ant_l
                        R.drawable.ant_l -> R.drawable.ant_n
                        R.drawable.ant_n -> R.drawable.ant_r
                        else -> R.drawable.ant
                    }
                } else {
                    currentGraphics = when (currentGraphics) {
                        R.drawable.ant_joke -> R.drawable.ant_l_joke
                        R.drawable.ant_l_joke -> R.drawable.ant_n_joke
                        R.drawable.ant_n_joke -> R.drawable.ant_r_joke
                        else -> R.drawable.ant_joke
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rlAnt.background = resources.getDrawable(currentGraphics, null)
                } else {
                    rlAnt.background = resources.getDrawable(currentGraphics)
                }
                firstGraphicStartMillis = System.currentTimeMillis()
            }
        }
    }

    private fun initScreenSize() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        sWidth = size.x
        sHeight = size.y
    }

    private fun initVibrate() {
        vb = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun playTabDefault() {
        if (defaultTap.isPlaying) {
            defaultTap.pause()
        }
        if (isCanPlayMusic) {
            defaultTap.seekTo(0)
            defaultTap.start()
        }
    }

    private fun playTabSuccess() {
        if (successTap.isPlaying) {
            successTap.pause()
        }
        if (isCanPlayMusic) {
            successTap.seekTo(0)
            successTap.start()
        }
    }

    private fun initSound() {
        gameBackgroundMusic = MediaPlayer.create(this, R.raw.game_sound)
        gameBackgroundMusic.isLooping = true
        defaultTap = MediaPlayer.create(this, R.raw.tap)
        successTap = MediaPlayer.create(this, R.raw.tap_success)
        gameIntroMusic = MediaPlayer.create(this, R.raw.game_sound_intro)
        gameIntroMusic.isLooping = true
        gameEndMusic = MediaPlayer.create(this, R.raw.game_sound_end)
        gameEndMusic.isLooping = true
        gameSoundJoke = MediaPlayer.create(this, R.raw.game_sound_joke)
        gameSoundFast = MediaPlayer.create(this, R.raw.game_sound_fast)
        gameSoundFast.isLooping = true
    }

    fun playJokeSound() {
        if (!gameSoundJoke.isPlaying && isGamePlaying && isCanPlayMusic) {
            gameSoundJoke.seekTo(0)
            gameSoundJoke.start()
        }
    }

    fun randomAntPos() {
        rlAnt.post {
            rlAnt.x = Random.nextInt(-rlAnt.width / 2, sWidth + rlAnt.width / 2).toFloat()
            rlAnt.y = Random.nextInt(-rlAnt.height / 2, sHeight + rlAnt.height / 2).toFloat()
            rlTap.isEnabled = true
            rlAnt.visibility = View.VISIBLE
            isDie = false
        }
    }

    private fun convertDpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    @SuppressLint("SetTextI18n")
    fun updateTurnLeft(value: Int) {
        turnLeft = value
        tvTurnLeft.post {
            if (turnLeft <= 10) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvTurnLeft.setTextColor(
                        resources.getColor(R.color.turn_warning_color, null)
                    )
                } else {
                    tvTurnLeft.setTextColor(
                        resources.getColor(R.color.turn_warning_color)
                    )
                }
                tvTurnLeft.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_tap_warning,
                    0,
                    0,
                    0
                )
                startTurnWarningAnimation()
            } else {
                if (waringTurnLeftAnimation != null && waringTurnLeftAnimation!!.isRunning) {
                    waringTurnLeftAnimation!!.cancel()
                }
                tvTurnLeft.setTextColor(Color.WHITE)
                tvTurnLeft.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_tap,
                    0,
                    0,
                    0
                )
            }
            tvTurnLeft.text = "Turn left: $turnLeft"
        }
    }

    override fun onStart() {
        isStarted = true
        gameVisible()
        super.onStart()
    }

    override fun onResume() {
        gameVisible()
        super.onResume()
    }

    private fun gameVisible() {
        if (isPressStartButton) {
            if (isCanPlayMusic) {
                if (isGamePlaying) {
                    startBackgroundMusic()
                    updateScore()
                } else {
                    showReplay()
                }
            }
        } else {
            if (!gameIntroMusic.isPlaying && isCanPlayMusic)
                gameIntroMusic.start()
        }
    }


    override fun onPause() {
        muteAllMusic()
        if (gameSoundJoke.isPlaying) {
            gameSoundJoke.pause()
        }
        super.onPause()
    }

    override fun onStop() {
        muteAllMusic()
        if (gameSoundJoke.isPlaying) {
            gameSoundJoke.pause()
        }
        super.onStop()
    }

    override fun onDestroy() {
        endGame()
        clearGame()
        super.onDestroy()
    }

    private fun setHighScore() {
        with(this.getPreferences(Context.MODE_PRIVATE).edit()) {
            putInt(HIGH_SCORE_KEY, score)
            putString(TIME_USED, tvPlayingTime.text.toString())
            apply()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getHighScore() {
        with(this.getPreferences(Context.MODE_PRIVATE)) {
            highScore = getInt(HIGH_SCORE_KEY, 0)
            timeUsedOfHighScore = getString(TIME_USED, "00:00:00")!!
            tvHighScore.setTextColor(HIGH_SCORE_COLOR)
            tvHighScore.text = "HIGH SCORE: $highScore"
            tvTimeUsed.text = "Time used: $timeUsedOfHighScore"
        }
    }

    private fun setSettings() {
        with(this.getPreferences(Context.MODE_PRIVATE).edit()) {
            putBoolean(IS_CAN_PLAY_MUSIC_KEY, isCanPlayMusic)
            if (!isCanPlayMusic) {
                muteAllMusic()
                ivSoundControl.post {
                    ivSoundControl.setImageResource(R.drawable.ic_sound_off)
                }
            } else {
                ivSoundControl.post {
                    ivSoundControl.setImageResource(R.drawable.ic_sound_on)
                }
                gameVisible()
            }
            apply()
        }
    }

    private fun getSettings() {
        with(this.getPreferences(Context.MODE_PRIVATE)) {
            isCanPlayMusic = getBoolean(IS_CAN_PLAY_MUSIC_KEY, true)
            if (!isCanPlayMusic) {
                muteAllMusic()
                ivSoundControl.post {
                    ivSoundControl.setImageResource(R.drawable.ic_sound_off)
                }
            } else {
                ivSoundControl.post {
                    ivSoundControl.setImageResource(R.drawable.ic_sound_on)
                }
            }
        }
    }
}
