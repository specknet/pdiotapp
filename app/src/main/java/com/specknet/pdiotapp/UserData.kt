package com.specknet.pdiotapp

import com.google.firebase.database.Exclude

data class UserData(val id:String? = null, val name:String? = null, val historicData:Map<String, List<Int>>? = null){

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "historicData" to historicData
        )
    }
}
