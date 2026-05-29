package com.nibbli.nibbligo.core.mobileactions

data class ContactParams(
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
)

data class EmailParams(
    val to: String = "",
    val subject: String = "",
    val body: String = "",
)

data class MapParams(
    val location: String = "",
)

data class CalendarParams(
    val datetime: String = "",
    val title: String = "",
)
