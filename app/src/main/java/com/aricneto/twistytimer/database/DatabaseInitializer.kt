package com.aricneto.twistytimer.database

import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.utils.Prefs.getPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.core.content.edit
import com.aricneto.twistytimer.utils.AlgUtils

object DatabaseInitializer {

    @Serializable
    data class AlgorithmData(
        val subset: String,
        val name: String,
        val state: String,
        val algs: String
    )

    suspend fun initialize(repository: AlgRepository, solveRepository: SolveRepository) = withContext(Dispatchers.IO) {
        initializeAlgorithms(repository)
        cleanupOldAlgorithms(repository)
        migrateTrainingMode(solveRepository)
    }

    private suspend fun cleanupOldAlgorithms(repository: AlgRepository) {
        val allAlgs = repository.getAllAlgorithms()
        val validCmllNames = AlgUtils.subsetCasesCMLL.toSet()
        
        allAlgs.filter { it.subset == "CMLL" }.forEach { alg ->
            if (!validCmllNames.contains(alg.name)) {
                repository.deleteAlgorithm(alg.subset, alg.name)
            }
        }
    }

    private suspend fun initializeAlgorithms(repository: AlgRepository) {
        val existing = repository.getAllAlgorithms()
        val existingKeys = existing.map { "${it.subset}:${it.name}" }.toSet()

        val context = TwistyTimer.getAppContext()
        val jsonString = context.assets.open("algorithms.json").use {
            it.bufferedReader().readText()
        }
        val algorithms = Json.decodeFromString<List<AlgorithmData>>(jsonString)

        algorithms.forEach { data ->
            if (!existingKeys.contains("${data.subset}:${data.name}")) {
                repository.insertAlgorithm(
                    subset = data.subset,
                    name = data.name,
                    state = data.state,
                    algs = data.algs,
                    progress = 0
                )
            }
        }
    }

    private suspend fun migrateTrainingMode(solveRepository: SolveRepository) {
        val prefs = getPrefs()
        if (!prefs.getBoolean("MIGRATED_TRAINING_MODE", false)) {
            solveRepository.migrateTrainingMode()
            prefs.edit {
                putBoolean("MIGRATED_TRAINING_MODE", true)
            }
        }
    }
}
