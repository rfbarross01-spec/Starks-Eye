package com.lume.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,

    val tipoConteudo: String,
    val tituloTipo: String,
    val tituloEvocativo: String,
    val observacaoAguda: String,
    val valeAprofundar: Boolean,
    val razaoNaoAprofundar: String?,
    val ehTechHype: Boolean,
    val confiancaLayer1: String,

    val layer2Json: String?,

    val verdictJson: String?,
    val veredito: String?,
    val vereditoUmaLinha: String?,

    val imagePath: String?,

    val userQuestion: String?,
    val layer2Provider: String?,
    val tagsJson: String?,

    val starred: Boolean = false,
    val archived: Boolean = false
)
