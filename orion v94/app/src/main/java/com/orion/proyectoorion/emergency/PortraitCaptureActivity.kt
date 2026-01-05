package com.orion.proyectoorion.emergency

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Activity de captura QR en modo vertical (portrait)
 * ZXing por defecto usa landscape, esta clase fuerza portrait
 */
class PortraitCaptureActivity : CaptureActivity()
