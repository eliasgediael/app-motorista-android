package br.app.motorista.data

import android.content.Context
import br.app.motorista.domain.finance.Category
import br.app.motorista.domain.finance.CategoryKind
import br.app.motorista.domain.finance.Bill
import br.app.motorista.domain.finance.BillPayment
import br.app.motorista.domain.finance.DayClose
import br.app.motorista.domain.finance.Goal
import br.app.motorista.domain.finance.ReserveEntry
import br.app.motorista.domain.finance.Setting
import br.app.motorista.domain.finance.Transaction
import br.app.motorista.domain.history.JourneyRecord
import br.app.motorista.domain.history.Profile
import br.app.motorista.domain.history.TripRecord
import br.app.motorista.domain.vehicle.CostComponent
import br.app.motorista.domain.vehicle.MaintenanceItem
import br.app.motorista.domain.vehicle.MaintenanceRecord
import br.app.motorista.domain.vehicle.Vehicle
import java.io.File
import java.util.UUID
import org.json.JSONObject

/**
 * Dados locais do usuário (arquivos JSON no armazenamento interno do app). Sem nuvem e sem telemetria.
 * Não guarda dados pessoais de passageiros.
 */
object AppData {
    private var initialized = false
    private lateinit var dir: File

    lateinit var categories: Repo<Category>
        private set
    lateinit var transactions: Repo<Transaction>
        private set
    lateinit var vehicles: Repo<Vehicle>
        private set
    lateinit var maintenanceItems: Repo<MaintenanceItem>
        private set
    lateinit var maintenanceRecords: Repo<MaintenanceRecord>
        private set
    lateinit var costComponents: Repo<CostComponent>
        private set
    lateinit var bills: Repo<Bill>
        private set
    lateinit var billPayments: Repo<BillPayment>
        private set
    lateinit var reserve: Repo<ReserveEntry>
        private set
    lateinit var goals: Repo<Goal>
        private set
    lateinit var dayCloses: Repo<DayClose>
        private set
    lateinit var settings: Repo<Setting>
        private set
    lateinit var trips: Repo<TripRecord>
        private set
    lateinit var journeys: Repo<JourneyRecord>
        private set
    lateinit var profiles: Repo<Profile>
        private set

    /** Repositórios por nome, para backup, restauração e apagar tudo. */
    private val registry = linkedMapOf<String, Repo<*>>()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        dir = File(context.applicationContext.filesDir, "data").also { it.mkdirs() }
        categories = register("categories", CategoryCodec)
        transactions = register("transactions", TransactionCodec)
        vehicles = register("vehicles", VehicleCodec)
        maintenanceItems = register("maintenanceItems", MaintenanceItemCodec)
        maintenanceRecords = register("maintenanceRecords", MaintenanceRecordCodec)
        costComponents = register("costComponents", CostComponentCodec)
        bills = register("bills", BillCodec)
        billPayments = register("billPayments", BillPaymentCodec)
        reserve = register("reserve", ReserveCodec)
        goals = register("goals", GoalCodec)
        dayCloses = register("dayCloses", DayCloseCodec)
        settings = register("settings", SettingCodec)
        trips = register("trips", TripCodec)
        journeys = register("journeys", JourneyRecordCodec)
        profiles = register("profiles", ProfileCodec)
        initialized = true
        seedCategoriesIfEmpty()
    }

    private fun <T> register(name: String, codec: Codec<T>): Repo<T> =
        Repo(File(dir, "$name.json"), codec).also { registry[name] = it }

    fun newId(): String = UUID.randomUUID().toString()

    fun setting(id: String): String? = settings.get(id)?.value

    fun putSetting(id: String, value: String) = settings.upsert(Setting(id, value))

    fun removeSetting(id: String) = settings.remove(id)

    /** Veículo ativo (id), se o usuário escolheu um e ele ainda existe e não está arquivado. */
    fun activeVehicle(): Vehicle? = setting("activeVehicle")?.let { id -> vehicles.get(id)?.takeIf { !it.archived } }

    /**
     * Nomes iniciais editáveis (sem valores). O usuário pode renomear, arquivar ou criar outros.
     * Plataformas refletem só os nomes dos aplicativos, não implicam integração.
     */
    private fun seedCategoriesIfEmpty() {
        if (categories.items.value.isNotEmpty()) return
        listOf(
            Category(newId(), "Corridas", CategoryKind.Income),
            Category(newId(), "Combustível", CategoryKind.Expense),
            Category(newId(), "Manutenção", CategoryKind.Expense),
            Category(newId(), "Alimentação", CategoryKind.Expense),
            Category(newId(), "99Pop", CategoryKind.Platform),
            Category(newId(), "Uber", CategoryKind.Platform),
        ).forEach { categories.upsert(it) }
    }

    // ---- backup e restauração ----

    const val BACKUP_VERSION = 1

    /** JSON com todos os dados. Gerado só por ação do usuário e entregue por ele a um arquivo. */
    fun exportBackup(extra: JSONObject = JSONObject()): String {
        val root = JSONObject().put("app", "No Ponto").put("version", BACKUP_VERSION).put("createdAt", System.currentTimeMillis())
        val data = JSONObject()
        registry.forEach { (name, repo) -> data.put(name, repo.exportArray()) }
        root.put("data", data).put("extra", extra)
        return root.toString()
    }

    /**
     * Valida e restaura. Nada é alterado se o arquivo for inválido ou de versão desconhecida.
     * @return o campo "extra" do backup (preferências), ou lança IllegalArgumentException.
     */
    @Synchronized
    fun importBackup(json: String): JSONObject {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Arquivo não é um backup válido")
        }
        require(root.optString("app") == "No Ponto") { "Arquivo não é um backup do No Ponto" }
        require(root.optInt("version", -1) == BACKUP_VERSION) { "Versão de backup desconhecida" }
        val data = root.getJSONObject("data")
        // Valida tudo antes de substituir qualquer coisa.
        val snapshot = registry.mapValues { (_, repo) -> repo.exportArray() }
        try {
            registry.forEach { (name, repo) ->
                if (data.has(name)) repo.importArray(data.getJSONArray(name)) else repo.importArray(org.json.JSONArray())
            }
        } catch (e: Exception) {
            registry.forEach { (name, repo) -> repo.importArray(snapshot.getValue(name)) }
            throw IllegalArgumentException("Backup corrompido: nenhum dado foi alterado")
        }
        return root.optJSONObject("extra") ?: JSONObject()
    }

    /** Apaga todos os dados locais registrados. Irreversível; a tela exige confirmação. */
    @Synchronized
    fun wipeAll() {
        registry.values.forEach { it.clear() }
        seedCategoriesIfEmpty()
    }
}
