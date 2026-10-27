package br.app.motorista.data

import br.app.motorista.domain.finance.Category
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.EntryKind
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.finance.TxSource
import java.io.File
import java.nio.file.Files
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoTest {
    private fun tempFile(): File = Files.createTempDirectory("repo").toFile().resolve("t.json")

    private val cat = Category("c1", "Corridas", CategoryKind.Income)

    @Test
    fun persistsAndReloads() {
        val f = tempFile()
        Repo(f, CategoryCodec).upsert(cat)
        val reloaded = Repo(f, CategoryCodec)
        assertEquals(listOf(cat), reloaded.items.value)
    }

    @Test
    fun upsertReplacesInPlaceAndRemoveDeletes() {
        val repo = Repo(tempFile(), CategoryCodec)
        repo.upsert(cat)
        repo.upsert(Category("c2", "Combustível", CategoryKind.Expense))
        repo.upsert(cat.copy(name = "Renomeada"))
        assertEquals(listOf("Renomeada", "Combustível"), repo.items.value.map { it.name })
        repo.remove("c1")
        assertNull(repo.get("c1"))
        assertNotNull(repo.get("c2"))
    }

    @Test
    fun corruptFileIsPreservedAndListStartsEmpty() {
        val f = tempFile()
        f.writeText("{nao eh json")
        val repo = Repo(f, CategoryCodec)
        assertTrue(repo.items.value.isEmpty())
        assertTrue(File(f.parentFile, "t.json.corrupt").exists())
    }

    @Test
    fun transactionCodecKeepsNullsAndSource() {
        val t = Transaction("t1", EntryKind.Income, 2480, 20_000, categoryId = null, source = TxSource.Trip, sourceRef = "trip-9", note = "x")
        val back = TransactionCodec.fromJson(TransactionCodec.toJson(t))
        assertEquals(t, back)
    }

    @Test
    fun invalidImportChangesNothing() {
        val repo = Repo(tempFile(), CategoryCodec)
        repo.upsert(cat)
        val bad = JSONArray("[{\"id\":\"x\"}]")
        try {
            repo.importArray(bad)
        } catch (e: Exception) {
            // esperado
        }
        assertEquals(listOf(cat), repo.items.value)
    }
}
