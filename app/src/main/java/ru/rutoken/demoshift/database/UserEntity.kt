/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package ru.rutoken.demoshift.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.io.Serializable

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["certificateDerValue"], unique = true),
        Index(value = ["tokenSerialNumber"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = AUTOGENERATE,
    val certificateDerValue: ByteArray,
    val ckaId: ByteArray,
    @Expose
    val tokenSerialNumber: String,
    var userDefault: Boolean = false,
    var pin: String?
): Serializable