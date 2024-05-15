package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.media.MediaRecorder
import android.widget.TextView
import it.dii.unipi.masss.noisemapper.NoiseActivity.RecorderTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Timer

class NoiseMicrophone(
    context: Context,
    cacheDir: File,
    private val sound: TextView,
    private val recorderTask: RecorderTask,
){
    private val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_PERFORMANCE
    private val OUTPUT_FORMAT_AUDIO = MediaRecorder.OutputFormat.MPEG_4
    private val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
    private val AUDIO_ENCODING_BIT_RATE = context.resources.getInteger(R.integer.AUDIO_ENCODING_BIT_RATE)
    private val AUDIO_SAMPLING_RATE = context.resources.getInteger(R.integer.AUDIO_SAMPLING_RATE)
    private val REFRESH_RATE = context.resources.getInteger(R.integer.AUDIO_REFRESH_RATE)
    private val mRecorder: MediaRecorder
    private var timer: Timer? = null
    init {
        mRecorder = MediaRecorder()
        mRecorder.setAudioSource(AUDIO_SOURCE)
        mRecorder.setOutputFormat(OUTPUT_FORMAT_AUDIO)
        mRecorder.setAudioEncoder(AUDIO_ENCODER)
        mRecorder.setAudioEncodingBitRate(AUDIO_ENCODING_BIT_RATE);
        mRecorder.setAudioSamplingRate(AUDIO_SAMPLING_RATE);
        mRecorder.setOutputFile(FileOutputStream(File(cacheDir, "audio.mp3")).fd)

    }
    fun startListening() {
        try {
            mRecorder.prepare()
            mRecorder.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (timer == null) {
            // If timer is not initialized, create and start it
            timer = Timer()
            recorderTask.setup(mRecorder, sound)
            timer?.schedule(recorderTask, 0, REFRESH_RATE.toLong())
        }
    }

    fun stopListening() {
        timer?.cancel()
        timer = null
        mRecorder.stop()
        mRecorder.release()
    }

}
