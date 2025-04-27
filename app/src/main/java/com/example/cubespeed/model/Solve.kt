package com.example.cubespeed.model

import com.google.firebase.Timestamp

data class Solve(
    val id: String = "",
    val cube: CubeType,
    val tagId: String,
    val timestamp: Timestamp,
    val time: Long,
    val scramble: String,
    val status: SolveStatus,
    val comments: String = ""
)
