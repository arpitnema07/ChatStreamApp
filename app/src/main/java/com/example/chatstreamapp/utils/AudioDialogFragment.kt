package com.example.chatstreamapp.utils

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.chatstreamapp.R
import com.example.chatstreamapp.databinding.DialogAudioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AudioDialogFragment : DialogFragment() {

    private lateinit var binding:DialogAudioBinding
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var mediaPlayer: MediaPlayer

    private var isPlaying: Boolean = false
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var download = false
    private var listener: AudioDialogListener? = null

    private val isDownloaded = MutableLiveData(false)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Initialize the listener when the fragment is attached to the activity
        if (context is AudioDialogListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement AudioDialogListener")
        }
    }

    companion object{
        fun newInstance(uri: String = "0",download:Boolean = false): AudioDialogFragment {
            val args = Bundle()
            if (uri != "0"){
                args.putString("audio",uri)
            }
            if (download){
                args.putBoolean("download",true)
            }
            val fragment = AudioDialogFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        binding = DialogAudioBinding.inflate(layoutInflater)

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying) {
                    binding.seekBar.progress = mediaPlayer.currentPosition
                    handler.postDelayed(this, 100)
                }
            }
        }

        val uriSt = arguments?.getString("audio",null)
        download = arguments?.getBoolean("download",false) == true

        if (uriSt!=null && !download){
            binding.record.visibility = View.GONE
            binding.play.isEnabled = true
            val inputStream = requireContext().contentResolver.openInputStream(Uri.parse(uriSt.toString()))
            val file = File.createTempFile("audio", ".3gp", requireContext().cacheDir)
            file.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            inputStream?.close()
            outputFile = file
        }

        if (uriSt!=null && download){
            binding.send.visibility = View.GONE
            binding.recordingText.visibility = View.VISIBLE
            binding.record.visibility = View.GONE
            binding.recordingText.text = getString(R.string.downloading)
            lifecycleScope.launch (Dispatchers.IO){
                val url = uriSt.toString()
                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.doInput = true
                urlConnection.connect()
                val inputStream = urlConnection.inputStream
                outputFile = File.createTempFile("audio", ".3gp", requireContext().cacheDir)
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.close()
                download = false
                isDownloaded.postValue(true)
            }
        }

        binding.record.setOnClickListener {
            binding.recordingText.visibility = View.VISIBLE
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
            isRecording = !isRecording
        }

        binding.play.setOnClickListener {
            if (isPlaying) {
                stopPlaying()
            } else {
                startPlaying()
            }

        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.send.setOnClickListener {
            if (isPlaying) binding.play.callOnClick()
            listener?.onAudioRecorded(outputFile)
            dismiss()
        }


        isDownloaded.observe(this){
            it?.let { d -> if (d) updateView() }
        }

        val dialog = Dialog(requireActivity())
        dialog.setContentView(binding.root)
        return dialog
    }

    private fun updateView() {
        binding.recordingText.text = getString(R.string.download_complete)
        binding.play.isEnabled = true
    }

    private fun startRecording() {
        recorder = MediaRecorder()
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            outputFile = createOutputFile()
            setOutputFile(outputFile?.absolutePath)
            prepare()
            start()
        }

        binding.record.text = getString(R.string.stop)
        binding.recordingText.text = getString(R.string.recording)
        binding.play.isEnabled = false
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            reset()
            release()
        }
        recorder = null
        binding.record.text = getString(R.string.record)
        binding.recordingText.text = outputFile?.name ?: ""
        binding.play.isEnabled = true
    }

    private fun startPlaying(){
        if (download) return
        val uri: Uri = outputFile?.toUri() ?: Uri.parse("")
        binding.seekBar.visibility = View.VISIBLE
        mediaPlayer =
            MediaPlayer.create(requireContext(), uri )
        binding.seekBar.max = mediaPlayer.duration
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
            binding.play.text = getString(R.string.pause)
            isPlaying = true
            handler.post(runnable)
        }
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
            binding.play.text = getString(R.string.play)
            isPlaying = false
            handler.removeCallbacks(runnable)
            binding.seekBar.visibility = View.GONE
        }
    }

    private fun stopPlaying(){
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
        binding.seekBar.visibility = View.GONE
        isPlaying = false
        binding.play.text = getString(R.string.play)
    }
    override fun onDetach() {
        super.onDetach()
        // Clear the listener when the fragment is detached from the activity
        listener = null
        handler.removeCallbacks(runnable)
        if (isPlaying){
            mediaPlayer.stop()
            mediaPlayer.release()
        }

    }
    interface AudioDialogListener {
        fun onAudioRecorded(file: File?)
    }

    private fun createOutputFile(): File {
        val directory = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile("audio", ".3gp", directory)
    }
}