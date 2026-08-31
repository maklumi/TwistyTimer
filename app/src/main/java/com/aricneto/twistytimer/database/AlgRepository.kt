package com.aricneto.twistytimer.database

import com.aricneto.twistytimer.items.Algorithm as DomainAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

class AlgRepository(private val queries: AlgorithmQueries) {

    fun getAlgorithms(subset: String): Flow<List<DomainAlgorithm>> =
        queries.selectBySubset(subset)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { sqAlg ->
                    DomainAlgorithm(
                        id = sqAlg._id,
                        subset = sqAlg.subset ?: "",
                        name = sqAlg.name ?: "",
                        state = sqAlg.state ?: "",
                        algs = sqAlg.algs ?: "",
                        progress = (sqAlg.progress ?: 0).toInt()
                    )
                }
            }

    suspend fun getAllAlgorithms() = withContext(Dispatchers.IO) {
        queries.selectAll().executeAsList().map { sqAlg ->
            DomainAlgorithm(
                id = sqAlg._id,
                subset = sqAlg.subset ?: "",
                name = sqAlg.name ?: "",
                state = sqAlg.state ?: "",
                algs = sqAlg.algs ?: "",
                progress = (sqAlg.progress ?: 0).toInt()
            )
        }
    }

    suspend fun insertAlgorithm(
        subset: String,
        name: String,
        state: String,
        algs: String,
        progress: Long
    ) = withContext(Dispatchers.IO) {
        queries.insertAlg(subset, name, state, algs, progress)
    }

    suspend fun updateAlgorithmAlg(id: Long, algs: String) = withContext(Dispatchers.IO) {
        queries.updateAlg(algs, id)
    }

    suspend fun updateAlgorithmProgress(id: Long, progress: Long) = withContext(Dispatchers.IO) {
        queries.updateProgress(progress, id)
    }
    
    suspend fun getAlgorithmById(id: Long) = withContext(Dispatchers.IO) {
        queries.selectById(id).executeAsOneOrNull()?.let { sqAlg ->
            DomainAlgorithm(
                id = sqAlg._id,
                subset = sqAlg.subset ?: "",
                name = sqAlg.name ?: "",
                state = sqAlg.state ?: "",
                algs = sqAlg.algs ?: "",
                progress = (sqAlg.progress ?: 0).toInt()
            )
        }
    }
}
