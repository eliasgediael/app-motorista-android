package br.app.motorista.data

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface Codec<T> {
    fun toJson(item: T): JSONObject
    fun fromJson(o: JSONObject): T
    fun idOf(item: T): String
}

/**
 * Lista persistida em um arquivo JSON local (sem nuvem, sem telemetria). Gravação atômica
 * (arquivo temporário + renomear). Arquivo corrompido é preservado como .corrupt e a lista começa vazia.
 */
class Repo<T>(private val file: File, private val codec: Codec<T>) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    init {
        load()
    }

    @Synchronized
    fun upsert(item: T) {
        val id = codec.idOf(item)
        val current = _items.value
        val index = current.indexOfFirst { codec.idOf(it) == id }
        _items.value = if (index >= 0) current.toMutableList().also { it[index] = item } else current + item
        save()
    }

    @Synchronized
    fun remove(id: String) {
        _items.value = _items.value.filter { codec.idOf(it) != id }
        save()
    }

    @Synchronized
    fun get(id: String): T? = _items.value.firstOrNull { codec.idOf(it) == id }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        save()
    }

    @Synchronized
    fun exportArray(): JSONArray = JSONArray().also { arr -> _items.value.forEach { arr.put(codec.toJson(it)) } }

    /** Substitui todo o conteúdo. Lança exceção (sem alterar nada) se algum item for inválido. */
    @Synchronized
    fun importArray(arr: JSONArray) {
        val parsed = (0 until arr.length()).map { codec.fromJson(arr.getJSONObject(it)) }
        _items.value = parsed
        save()
    }

    private fun save() {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(exportArray().toString())
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val arr = JSONArray(file.readText())
            _items.value = (0 until arr.length()).map { codec.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            file.renameTo(File(file.parentFile, file.name + ".corrupt"))
            _items.value = emptyList()
        }
    }
}
