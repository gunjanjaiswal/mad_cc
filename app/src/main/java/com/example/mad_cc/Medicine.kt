package com.example.mad_cc

import java.io.Serializable

data class Medicine(
    var id: String = "",
    var name: String = "",
    var hour: Int = 0,
    var minute: Int = 0,
    var userId: String = "",
    var weekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // 1=Sun, 2=Mon, ..., 7=Sat
) : Serializable
