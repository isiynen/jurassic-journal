package com.jurassicjournal.data.game.repository

import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.Rarity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DinoRepository @Inject constructor(private val dinoDao: DinoDao) {

    fun getDinos(
        nameQuery: String = "",
        rarity: Rarity? = null,
        dinoClass: DinoClass? = null,
    ): Flow<List<Dino>> = dinoDao.observeDinos(
        nameQuery = nameQuery,
        rarity = rarity?.name ?: "",
        dinoClass = dinoClass?.name ?: "",
    )
}
