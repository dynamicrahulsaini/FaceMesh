package com.example.facemesh

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.facemesh.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult

class MainActivity : AppCompatActivity() {

    private val _logTag: String = javaClass.name
    private lateinit var binding: ActivityMainBinding
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var faceMesh: FaceMesh
    private lateinit var cameraInput: CameraInput
    private lateinit var glSurfaceView: SolutionGlSurfaceView<FaceMeshResult>

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        checkPermission()
        checkCameraPermission()
        Log.i(
            _logTag,
            "Android SDK Version -> ${Build.VERSION.SDK_INT} / ${Build.VERSION.CODENAME}"
        )
        setup()
    }

    private fun setup() {
        Log.i(_logTag, "Starting setup...")
        faceMesh = FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(false)
                .setRefineLandmarks(true)
                .setRunOnGpu(true)
                .build()
        )
        Log.i(_logTag, "FaceMesh initialized...")
        faceMesh.setErrorListener { message, _ -> Log.e(_logTag, "Error MediaPipe Face Mesh -> $message") }
        Log.i(_logTag, "FaceMesh initialized... -> Done")

        Log.i(_logTag, "Camera check  initializing...")
        cameraInput = CameraInput(this)
        cameraInput.setNewFrameListener {
            faceMesh.send(it)
        }
        Log.i(_logTag, "Camera check  initializing... -> Done")

        Log.i(_logTag, "GlSurfaceView initializing...")
        glSurfaceView = SolutionGlSurfaceView(
            this,
            faceMesh.glContext,
            faceMesh.glMajorVersion
        )
        Log.i(_logTag, "GlSurfaceView initializing... -> Done")
        Log.i(_logTag, "Setting up GlSurfaceView...")
        glSurfaceView.setSolutionResultRenderer(FaceMeshResultGlRenderer())
        glSurfaceView.setRenderInputImage(true)
        faceMesh.setResultListener {
            glSurfaceView.setRenderData(it)
            glSurfaceView.requestRender()
        }
        Log.i(_logTag, "Setting up GlSurfaceView... -> Done")

        glSurfaceView.post {
            startCamera()
        }

        binding.frameLayout.addView(glSurfaceView)
        glSurfaceView.visibility = View.VISIBLE
        binding.frameLayout.requestLayout()
    }

    private fun startCamera() {
//        Log.i(_logTag, "${  } x ${  }")
        cameraInput.start(
            this,
            faceMesh.glContext,
            CameraInput.CameraFacing.FRONT,
            glSurfaceView.width,
            glSurfaceView.height
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) -> {
                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (!isGranted) {
                        Snackbar.make(
                            binding.root,
                            "Grant camera permission in order to use the app",
                            Snackbar.LENGTH_INDEFINITE
                        ).show()
                    }
                }
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }



    override fun onResume() {
        super.onResume()
        cameraInput = CameraInput(this)
        cameraInput.setNewFrameListener {
//            Log.i(_logTag, "${ it.height } x ${ it.width}")
            faceMesh.send(it)
        }
        glSurfaceView.post { startCamera() }
    }

    override fun onPause() {
        super.onPause()
        cameraInput.close()
    }
}