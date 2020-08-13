package com.specknet.pdiotapp.utils

data class RespeckData(val timestamp: Long, val accel_x: Float, val accel_y: Float,
                       val accel_z: Float, val accel_mag: Float, val breathingSignal: Float)