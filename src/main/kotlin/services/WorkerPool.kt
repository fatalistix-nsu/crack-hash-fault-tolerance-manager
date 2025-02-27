package com.github.fatalistix.services

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.github.fatalistix.domain.exception.NoWorkersLeftException
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.util.generateId
import kotlinx.coroutines.channels.Channel

class WorkerPool {

    private val registeredWorkers = ConcurrentMutableMap<String, Worker>()
    private val workerChannel = Channel<Worker>(Channel.UNLIMITED)

    suspend fun take(): Worker {
        while (true) {
            val result = workerChannel.receiveCatching()
            if (result.isSuccess) {
                val worker = result.getOrThrow()
                if (worker.id in registeredWorkers) {
                    return worker
                }
            } else if (result.isClosed) {
                throw NoWorkersLeftException()
            }
        }
    }

    suspend fun release(worker: Worker) {
        if (worker.id in registeredWorkers) {
            workerChannel.send(worker)
        }
    }

    suspend fun register(address: String, port: UShort): String? {
        val worker = Worker(address, port)

        val result = registeredWorkers.putIfAbsent(worker.id, worker)
        if (result == null) {
            workerChannel.send(worker)
            return worker.id
        } else {
            return null
        }
    }

    fun deregister(worker: Worker) {
        registeredWorkers.remove(worker.id)
        if (registeredWorkers.isEmpty()) {
            workerChannel.close()
        }
    }

    fun get(workerId: String) = registeredWorkers[workerId]
}