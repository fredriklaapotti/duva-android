package com.step84.duva

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.opencensus.stats.Aggregation
import kotlinx.android.synthetic.main.fragment_home.*
import org.w3c.dom.Text
import java.io.IOException
import java.lang.IllegalStateException

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HomeFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private val SOUND_RECORDING_MAX_LENGTH: Long = 25 * 1000
    private val SOUND_RECORDING_TICK: Long = 1 * 1000

    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private lateinit var auth: FirebaseAuth

    private lateinit var txt_username: TextView
    private lateinit var txt_currentZone: TextView
    private lateinit var txt_debug: TextView
    private lateinit var switch_toggleLarmButtons: Switch
    private lateinit var btn_larmRecord: ImageButton
    private lateinit var progress_soundRecording: ProgressBar

    enum class LarmState {
        Passive, Confirming, Larming
    }
    enum class TimerState {
        Stopped, Paused, Running
    }

    private var recording: Boolean = false
    private var larmState = LarmState.Passive
    private lateinit var timer: CountDownTimer
    private var timerState = TimerState.Stopped

    private val audioRecorder: MediaRecorder = MediaRecorder()
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private lateinit var audioOutputFile: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        txt_username = view.findViewById(R.id.txt_username)
        txt_currentZone = view.findViewById(R.id.txt_currentZone)
        txt_debug = view.findViewById(R.id.txt_debug)
        switch_toggleLarmButtons = view.findViewById(R.id.switch_toggleLarmButtons)
        btn_larmRecord = view.findViewById(R.id.btn_larmRecord)
        progress_soundRecording = view.findViewById(R.id.bar_progressSoundRecording)
        progress_soundRecording.visibility = View.INVISIBLE
        txt_debug.visibility = View.INVISIBLE

        switch_toggleLarmButtons.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                AlertDialog.Builder(context)
                    .setTitle(getText(R.string.dialog_larm_title))
                    .setMessage(R.string.dialog_larm_message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialogInterface, i ->
                        btn_larmRecord.visibility = View.VISIBLE
                    })
                    .setNegativeButton(android.R.string.no, DialogInterface.OnClickListener { dialogInterface, i ->
                        btn_larmRecord.visibility = View.INVISIBLE
                        switch_toggleLarmButtons.isChecked = false
                    }).show()
            } else {
                btn_larmRecord.visibility = View.INVISIBLE
            }
        }

        updateLocation(Globals.activeZoneId)

        /*
        btn_confirmLarm.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Title")
                .setMessage("Do you want to larm?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialogInterface, i ->
                    btn_larmRecord.visibility = View.VISIBLE
                })
                .setNegativeButton(android.R.string.no, DialogInterface.OnClickListener { dialogInterface, i ->
                    btn_larmRecord.visibility = View.INVISIBLE
                }).show()
        }
        */

        btn_larmRecord.setOnClickListener {
            when (recording) {
                true -> {
                    toggleLarmSoundRecording()
                }
                false -> {
                    toggleLarmSoundRecording()
                    val rotateAnimation: Animation = AnimationUtils.loadAnimation(context!!, R.anim.blink)
                    btn_larmRecord.startAnimation(rotateAnimation)
                }
            }
        }

        updateUI()

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onPause() {
        super.onPause()
        if(recording) {
            audioRecorder.stop()
            audioRecorder.reset()
            recording = false
            btn_larmRecord.clearAnimation()
            timer.cancel()
            progress_soundRecording.visibility = View.INVISIBLE
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun toggleLarmSoundRecording() {
        if(!recording) {
            // Start recording, catch audio
            Log.i(TAG, "duva: larm recording is true, starting audio recording")
            audioOutputFile = Environment.getExternalStorageDirectory().absolutePath + "/duva_audioOutputFile.3gp"
            audioRecorder.reset()
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            audioRecorder.setOutputFile(audioOutputFile)

            try {
                Log.i(TAG, "duva: larm calling prepare() and start() on audio")
                audioRecorder.prepare()
                audioRecorder.start()
                recording = true
                progress_soundRecording.visibility = View.VISIBLE
                progress_soundRecording.progress = 0
                startTimer()
            } catch (ise: IllegalStateException) {
                Log.d(TAG, "duva: larm failed calling prepare() and start() on audio", ise)
                return
            } catch (ioe: IOException) {
                Log.d(TAG, "duva: larm failed calling prepare() and start() on audio", ioe)
                return
            }
        } else {
            // Recording, toggle and upload
            Log.i(TAG, "duva: larm recording is false, uploading audio")
            audioRecorder.stop()
            audioRecorder.reset()
            recording = false
            btn_larmRecord.clearAnimation()
            timer.cancel()
            progress_soundRecording.visibility = View.INVISIBLE

            CloudStorage.upload(audioOutputFile, object: CloudStorageUploadListener {
                override fun onSuccess(url: String) {
                    Log.i(TAG, "duva: returned in listener onSuccess file uploaded: $url")

                    // TODO: continue here, fix system for keeping track of user with global variables?
                    val alarm = Alarm(Timestamp.now(), auth.uid.toString(), Globals.activeZoneId, url, "soundRecording")
                    Firestore.addObject("alarms", alarm, object: FirestoreCallback {
                        override fun onSuccess() {
                            Log.i(TAG, "duva: returned in listener onSuccess alarm added")
                            Toast.makeText(context, R.string.toast_alarmSent, Toast.LENGTH_LONG).show()
                            btn_larmRecord.visibility = View.INVISIBLE
                            switch_toggleLarmButtons.isChecked = false
                        }

                        override fun onFailed() {}
                    })
                }

                override fun onFailed() {
                    Log.d(TAG, "duva: returned in listener onFailed file upload")
                }
            })

            try {
                Log.i(TAG, "duva: larm audio playback setDatasource($audioOutputFile)")
                mediaPlayer.reset()
                mediaPlayer.setDataSource(audioOutputFile)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (ise: IllegalStateException) {
                Log.d(TAG, "duva: audio playback failed", ise)
                return
            } catch (ioe: IOException) {
                Log.d(TAG, "duva: audio playback failed", ioe)
                return
            }
        }
    }

    private fun startTimer() {
        timerState = TimerState.Running

        timer = object : CountDownTimer(SOUND_RECORDING_MAX_LENGTH, SOUND_RECORDING_TICK) {
            override fun onFinish() = onTimerFinished()
            override fun onTick(millisUntilFinished: Long) {
                progress_soundRecording.incrementProgressBy(4)
            }
        }.start()
    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped
        recording = true
        toggleLarmSoundRecording()
    }

    fun updateLocation(zoneid: String) {
        Log.i(TAG, "duva: geofence updating HomeFragment, zoneid = $zoneid, Globals.activeZoneId = ${Globals.activeZoneId}")
        //txt_currentZone.text = Globals.getZoneNameFromZoneId(zoneid).takeUnless { it == "unknown" } ?: getText(R.string.txt_currentZone)
        txt_currentZone.text = Globals.getZoneNameFromZoneId(zoneid).takeUnless { it == "exit" } ?: "no zone detected"
    }

    fun updateUI() {
        switch_toggleLarmButtons.visibility = View.INVISIBLE
        btn_larmRecord.visibility = View.INVISIBLE

        if(!Globals.geofencesAdded) {
            txt_debug.visibility = View.VISIBLE
            txt_debug.text = getString(R.string.alert_geofences)
        }

        Log.i(TAG, "duva: user just before check for logged in and able to set alarm")

        if(auth.currentUser != null) {
            if(auth.currentUser!!.isAnonymous) {
                txt_username.text = "anonymous (guest)"
            } else {
                txt_username.text = auth.currentUser!!.email
            }

            if(!auth.currentUser!!.isAnonymous && Globals.activeZoneId != "0") {
                Log.i(TAG, "duva: user is not anonymous and in a zone")
                switch_toggleLarmButtons.visibility = View.VISIBLE
                if(switch_toggleLarmButtons.isChecked) {
                    btn_larmRecord.visibility = View.VISIBLE
                }
            } else {
                Log.i(TAG, "duva: user is not anonymous or not in a zone")
            }
        } else {
            Log.i(TAG, "duva: user object is null")
            switch_toggleLarmButtons.visibility = View.INVISIBLE
            btn_larmRecord.visibility = View.INVISIBLE
        }
    }

    fun View.toggleVisibility() {
        if(visibility == View.VISIBLE) {
            visibility = View.INVISIBLE
        } else {
            visibility = View.VISIBLE
        }
    }
}
