//
//  KubotaEquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.*
import com.couchbase.lite.Function
import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.*
import com.kubota.service.domain.*
import com.kubota.service.internal.couchbase.DictionaryDecoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import com.squareup.moshi.JsonDataException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Throws
import kotlin.Unit
import kotlin.also
import kotlin.let
import kotlin.to

data class CategoriesAndModels(
    val categories: List<CategoryRaw>,
    val models: List<ModelRaw>
)

data class ModelRaw(
    val model: String,
    val searchModel: String?,
    val type: String,
    val modelDescription: String?,
    val modelHeroUrl: URL?,
    val modelFullUrl: URL?,
    val modelIconUrl: URL?,
    val categoryId: Int,
    val compatibleAttachments: List<String>?,
    val manualEntries: List<ManualInfo>,
    val videoEntries: List<VideoInfo>,
    val guideUrl: URL?,
    val warrantyUrl: URL,
    val hasFaultCodes: Boolean,
    val hasMaintenanceSchedules: Boolean,
    val discontinuedDate: Date?
)

data class CategoryRaw(
    val id: Int,
    val parentId: Int?,
    val name: String,
    val heroUrl: URL?,
    val fullUrl: URL?,
    val iconUrl: URL?
)

private data class EquipmentCategoryDocument(
    val type: String = "EquipmentCategory",
    val categoryId: Int,
    val parentCategoryId: Int,
    val category: EquipmentCategory
)

private data class EquipmentModelDocument(
    val type: String = "EquipmentModel",
    val name: String,
    val categoryId: Int,
    val model: EquipmentModel
)

private data class MetadataDocument(
    val type: String,
    val metadata: DocumentMetadata
)

private data class DocumentMetadata(
    val etag: String?
)

data class MaintenanceHistoryUpdate(
    val id: String?,
    val intervalType: String,
    val intervalValue: Int,
    val completedEngineHours: Long,
    val notes: String,
    val updatedDate: String,
    val maintenanceCheckList: Map<String, Boolean>
)

private val SearchModelType.queryParams: QueryParameters
    get() = when (this) {
        is SearchModelType.PartialModelAndSerial -> {
            queryParams(
                "partialModel" to this.partialModel,
                "serial" to this.serial
            )
        }
        is SearchModelType.PartialModelAndPIN -> {
            queryParams(
                "partialModel" to this.partialModel,
                "pin" to this.pin
            )
        }
        is SearchModelType.PIN -> {
            queryParams(
                "pin" to this.pin
            )
        }
    }

internal class KubotaEquipmentService(
    config: Config,
    private val couchbaseDb: Database
) : HTTPService(config = config), EquipmentService {

    override fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>> {
        val criteria = CacheCriteria(
            policy = CachePolicy.useAgeReturnCacheIfError,
            age = CacheAge.oneDay.interval
        )

        val p = this.get(
            route = "/api/maintenanceSchedule/$model",
            type = Array<EquipmentMaintenance>::class.java,
            cacheCriteria = criteria
        )

        return p.map { it.toList() }
    }

    override fun getMaintenanceHistory(id: String): Promise<List<EquipmentMaintenanceHistoryEntry>> {
        val criteria = CacheCriteria(
            policy = CachePolicy.useAgeReturnCacheIfError,
            age = CacheAge.oneDay.interval
        )

        val p = this.get(
            route = "/api/user/equipment/$id/maintenanceHistory",
            type = Array<EquipmentMaintenanceHistoryEntry>::class.java,
            cacheCriteria = criteria
        )

        return p.map { it.toList() }
    }

    override fun updateMaintenanceHistory(
        unitId: String,
        update: MaintenanceHistoryUpdate
    ): Promise<Boolean> {
        return this.put(
            route = "/api/user/equipment/${unitId}/maintenanceHistory",
            body = UploadBody.Json(value = update)
        ).map {
            true
        }.recover {
            if (it is SocketTimeoutException || it is UnknownHostException || it is KubotaServiceError.NotConnectedToInternet || it is KubotaServiceError.NetworkConnectionLost || it is ConnectException) {

                retryUpdateMaintenanceHistory(unitId, update)
                Promise.value(false)
            } else {
                throw it
            }
        }
    }

    private fun retryUpdateMaintenanceHistory(
        unitId: String,
        update: MaintenanceHistoryUpdate
    ) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                updateMaintenanceHistory(unitId, update)
            }
        }, 1000 * 5)
    }

    override fun addMaintenanceEntry(
        id: String,
        entry: EquipmentMaintenanceHistoryEntry
    ): Promise<Unit> {
        return this.put(
            route = "/api/user/equipment/$id/maintenanceHistory",
            body = UploadBody.Json(value = entry)
        ).map {}
    }

    override fun getModel(model: String): Promise<EquipmentModel?> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
            this.couchbaseDb.getModel(model = model)
                ?.let { Promise.value(it) }
                ?: this.updateCategoriesAndModels()
                    .thenMap(on = DispatchExecutor.global) { getModel(model) }
        }
    }

    override fun searchModels(type: SearchModelType): Promise<List<EquipmentModel>> {
        return service {
            this.get(
                route = "/api/models",
                query = type.queryParams,
                type = CategoriesAndModels::class.java
            ).map(on = DispatchExecutor.global) { (categoriesRaw, modelsRaw) ->
                modelsRaw.map { raw ->
                    raw.toDbModelDocument { categoryId ->
                        categoriesRaw.first { it.id == categoryId }.name
                    }
                }
            }
        }
    }

    override fun scanSearchModels(type: SearchModelType): Promise<List<String>> {
        return service {
            this.get(
                route = "/api/models/scan",
                query = type.queryParams,
                type = Array<String>::class.java
            ).map(on = DispatchExecutor.global) { it.toList() }
        }
    }

    override fun getModels(category: String): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
            val models = this.couchbaseDb.getModels(category = category)
            when (models.isNullOrEmpty()) {
                true -> this.updateCategoriesAndModels().map(on = DispatchExecutor.global) {
                    this.couchbaseDb.getModels(category = category)
                }
                false -> Promise.value(models).also { updateModelStoreCacheForNextTime() }
            }
        }.map(on = DispatchExecutor.global) { it.caseInsensitiveSort { it.model } }
    }

    override fun getCompatibleMachines(model: String): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb.getCompatibleMachines(model)
        }
    }

    override fun getAvailableModels(): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb.getAvailableModels()
        }
    }

    override fun searchAttachments(type: SearchModelType.PartialModelAndSerial): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb.searchAttachments(type)
        }
    }

    override fun getEquipmentTree(
        modelFilters: List<String>,
        categoryFilters: List<String>
    ): Promise<List<EquipmentModelTree>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            // temp root for convenience
            val root = EquipmentModelTree.Category(
                category = EquipmentCategory(
                    category = "root",
                    parentCategory = null,
                    hasSubCategories = true,
                    imageResources = null
                ),
                items = this.couchbaseDb.getEquipmentModelTree()
            )

            listOf(root)
                .let {
                    if (modelFilters.isEmpty()) {
                        it
                    } else {
                        it.getModelFilteredSubTree(modelFilters)
                    }
                }
                .getCategoryFilteredSubTree(categoryFilters)
                .firstOrNull()?.let { it as EquipmentModelTree.Category }?.items
                ?: emptyList()// remove root
        }
    }

    private fun List<EquipmentModelTree>.getModelFilteredSubTree(
        modelFilters: List<String>
    ): List<EquipmentModelTree> {
        return this.mapNotNull { tree ->
            when (tree) {
                is EquipmentModelTree.Category -> {
                    val items = tree.items
                        .filter { it.containsModelWithName(modelFilters) }

                    when (items.isEmpty()) {
                        true -> null
                        false -> tree.copy(items = items.getModelFilteredSubTree(modelFilters))
                    }
                }
                is EquipmentModelTree.Model -> {
                    if (modelFilters.isEmpty() || tree.model.model in modelFilters) {
                        return@mapNotNull tree
                    }

                    null
                }
            }
        }
    }

    private fun List<EquipmentModelTree>.getCategoryFilteredSubTree(
        categoryFilters: List<String>
    ): List<EquipmentModelTree> {
        return this.mapNotNull { tree ->
            when (tree) {
                is EquipmentModelTree.Category -> {
                    if (categoryFilters.isEmpty()) {
                        return@mapNotNull tree
                    }

                    val items = tree.items
                        .filter { it.containsCategoryWithName(categoryFilters) }

                    val newCategoryFilters = categoryFilters - items
                        .mapNotNull { (it as? EquipmentModelTree.Category)?.category?.category }

                    when (items.isEmpty()) {
                        true -> null
                        false -> tree.copy(
                            items = items.getCategoryFilteredSubTree(newCategoryFilters)
                        )
                    }
                }
                is EquipmentModelTree.Model -> tree
            }
        }
    }

    private fun EquipmentModelTree.containsModelWithName(modelNames: List<String>): Boolean {
        when (this) {
            is EquipmentModelTree.Category -> {
                this.items.forEach {
                    if (it.containsModelWithName(modelNames)) {
                        return true
                    }
                }
                return false
            }
            is EquipmentModelTree.Model -> return this.model.model in modelNames
        }
    }

    private fun updateModelStoreCacheForNextTime() {
        Promise.value(Unit).map(on = DispatchExecutor.global) {
            val documentMetadata = this.couchbaseDb.getDocumentMetadata()
            this.updateCategoriesAndModels(documentMetadata?.etag)
        }.cauterize()
    }

    override fun getCategories(parentCategory: String?): Promise<List<EquipmentCategory>> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
            val categories = this.couchbaseDb.getCategories(parentCategory = parentCategory)
            when (categories.isNullOrEmpty()) {
                true -> this.updateCategoriesAndModels().map(on = DispatchExecutor.global) {
                    this.couchbaseDb.getCategories(parentCategory = parentCategory)
                }
                false -> Promise.value(categories).also { updateModelStoreCacheForNextTime() }
            }
        }.map(on = DispatchExecutor.global) { it.caseInsensitiveSort { it.category } }
    }

    private fun updateCategoriesAndModels(etag: String? = null): Promise<Unit> {
        return service {
            val additionalHeaders = mutableMapOf<String, String>()

            if (etag != null) {
                additionalHeaders["If-None-Match"] = etag
            }
            this.get(route = "/api/models", additionalHeaders = additionalHeaders)
                .map(on = DispatchExecutor.global) {
                    val aDecoder = this.decoder(it!!.mimeType)

                    // Expecting a value. Otherwise throw the exception that can be caught by the Promise
                    // If result is null it would have already thrown an exception
                    val requestResult = aDecoder!!.decode(
                        type = CategoriesAndModels::class.java,
                        value = it.body
                    )!!

                    val categoryDocs = requestResult.categories.toDbCategoryDocuments()
                    val modelDocs = requestResult.models.toDbModelDocuments { categoryId ->
                        requestResult.categories.first { it.id == categoryId }.name
                    }

                    this.couchbaseDb.saveEquipmentDocuments(
                        categoryDocuments = categoryDocs,
                        modelDocuments = modelDocs,
                        documentEtag = it.headers["ETag"]
                    )
                }
        }
    }

    private fun List<CategoryRaw>.toDbCategoryDocuments(): List<EquipmentCategoryDocument> {
        return this.map { raw ->
            EquipmentCategoryDocument(
                categoryId = raw.id,
                parentCategoryId = raw.parentId ?: 0, // zero for root
                category = EquipmentCategory(
                    category = raw.name,
                    parentCategory = this.firstOrNull { raw.parentId == it.id }?.name,
                    hasSubCategories = this.firstOrNull { it.parentId == raw.id } != null,
                    imageResources = ImageResources(
                        heroUrl = raw.heroUrl,
                        iconUrl = raw.iconUrl,
                        fullUrl = raw.fullUrl
                    )
                )
            )
        }
    }

    private fun List<ModelRaw>.toDbModelDocuments(getCategoryById: (Int) -> String): List<EquipmentModelDocument> {
        return this.map { raw ->
            EquipmentModelDocument(
                categoryId = raw.categoryId,
                name = raw.model,
                model = raw.toDbModelDocument(getCategoryById)
            )
        }
    }

    private fun ModelRaw.toDbModelDocument(getCategoryById: (Int) -> String): EquipmentModel {
        return EquipmentModel(
            model = this.model,
            searchModel = this.searchModel,
            type = EquipmentModel.Type.values().first {
                it.name.lowercase() == this.type.lowercase()
            },
            description = this.modelDescription,
            imageResources = ImageResources(
                heroUrl = this.modelHeroUrl,
                fullUrl = this.modelFullUrl,
                iconUrl = this.modelIconUrl
            ),
            category = getCategoryById(this.categoryId),
            guideUrl = this.guideUrl,
            manualEntries = this.manualEntries,
            videoEntries = this.videoEntries,
            warrantyUrl = this.warrantyUrl,
            hasFaultCodes = this.hasFaultCodes,
            hasMaintenanceSchedules = this.hasMaintenanceSchedules,
            compatibleAttachments = this.compatibleAttachments ?: emptyList(),
            discontinuedDate = this.discontinuedDate
        )
    }
}

fun EquipmentModelTree.containsCategoryWithName(categoryNames: List<String>): Boolean {
    when (this) {
        is EquipmentModelTree.Category -> {
            if (this.category.category in categoryNames) {
                return true
            } else {
                this.items.forEach {
                    if (it.containsCategoryWithName(categoryNames)) {
                        return true
                    }
                }
                return false
            }
        }
        is EquipmentModelTree.Model -> return false
    }
}

@Throws
private fun Database.getCategories(parentCategory: String?): List<EquipmentCategory> {
    // 0 for root
    val parentCategoryId = parentCategory?.let { getCategoryId(parentCategory) } ?: 0

    val query = QueryBuilder
        .select(SelectResult.property("category"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type")
                .equalTo(Expression.string("EquipmentCategory"))
                .and(
                    Expression.property("parentCategoryId")
                        .equalTo(Expression.number(parentCategoryId))
                )
        )

    val decoder = DictionaryDecoder()
    return query.execute().allResults().mapNotNull {
        val dict = it.toMap()["category"] as Map<String, Any>
        decoder.decode(EquipmentCategory::class.java, value = dict)
    }
}

@Throws
private fun Database.getDocumentMetadata(): DocumentMetadata? {
    val query = QueryBuilder
        .select(SelectResult.property("metadata"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("DocumentMetadata"))
        )
    val decoder = DictionaryDecoder()

    val dict = query.execute().next()?.toMap()?.get("metadata") as? Map<String, Any>
    return dict?.let { decoder.decode(DocumentMetadata::class.java, value = dict) }
}

@Throws
private fun Database.getModels(category: String): List<EquipmentModel> {
    val categoryId = getCategoryId(category)

    val query = QueryBuilder
        .select(SelectResult.property("model"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentModel"))
                .and(Expression.property("categoryId").equalTo(Expression.intValue(categoryId)))
        )

    val decoder = DictionaryDecoder()
    return query.execute().allResults().mapNotNull {
        val dict = it.toMap()["model"] as Map<String, Any>
        try {
            decoder.decode(EquipmentModel::class.java, value = dict)
        } catch (e: JsonDataException) {
            // return empty list so it re-caches model info
            return emptyList()
        }
    }
}

@Throws
private fun Database.getCompatibleMachines(model: String): List<EquipmentModel> {
    val query = QueryBuilder
        .select(SelectResult.property("model"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentModel"))
                .and(
                    ArrayFunction.contains(
                        Expression.property("model.compatibleAttachments"),
                        Expression.string(model)
                    )
                )
        )

    val decoder = DictionaryDecoder()
    return query.execute().allResults().mapNotNull {
        val dict = it.toMap()["model"] as Map<String, Any>
        decoder.decode(EquipmentModel::class.java, value = dict)
    }
}

@Throws
private fun Database.getAvailableModels(): List<EquipmentModel> {
    val query = QueryBuilder
        .select(SelectResult.property("model"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentModel"))
                .and(
                    Expression.property("model.discontinuedDate").isNullOrMissing
                        .or(
                            Expression.property("model.discontinuedDate")
                                .greaterThan(Expression.date(Date()))
                        )
                )
        )

    val decoder = DictionaryDecoder()
    return query.execute().allResults().mapNotNull {
        val dict = it.toMap()["model"] as Map<String, Any>
        decoder.decode(EquipmentModel::class.java, value = dict)
    }
}

@Throws
private fun Database.searchAttachments(type: SearchModelType.PartialModelAndSerial): List<EquipmentModel> {
    val query = QueryBuilder
        .select(SelectResult.property("model"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentModel"))
                .and(
                    Expression.property("model.type")
                        .equalTo(Expression.string(EquipmentModel.Type.Attachment.toString()))
                )
                .and(
                    Function.lower(Expression.property("name"))
                        .like(Expression.string("${type.partialModel.lowercase()}%"))
                )
        )

    val decoder = DictionaryDecoder()
    return query.execute().allResults().mapNotNull {
        val dict = it.toMap()["model"] as Map<String, Any>
        decoder.decode(EquipmentModel::class.java, value = dict)
    }
}

@Throws
private fun Database.getModel(model: String): EquipmentModel? {
    val query = QueryBuilder
        .select(SelectResult.property("model"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentModel"))
                .and(Expression.property("name").equalTo(Expression.string(model)))
        )

    val decoder = DictionaryDecoder()
    val dict = query.execute().next()?.toMap()?.get("model") as? Map<String, Any>
    return dict?.let { decoder.decode(EquipmentModel::class.java, value = dict) }
}

@Throws
private fun Database.getEquipmentModelTree(
    parentCategory: String? = null
): List<EquipmentModelTree> {
    val parentCategoryId = parentCategory?.let { getCategoryId(parentCategory) } ?: 0

    val query = QueryBuilder
        .select(SelectResult.property("category"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type")
                .equalTo(Expression.string("EquipmentCategory"))
                .and(
                    Expression.property("parentCategoryId")
                        .equalTo(Expression.number(parentCategoryId))
                )
        )

    val decoder = DictionaryDecoder()

    val categories = query.execute().allResults().mapNotNull {
        val dict = it.toMap()["category"] as Map<String, Any>
        decoder.decode(EquipmentCategory::class.java, value = dict)
    }

    return if (categories.isEmpty()) {
        emptyList()
    } else {
        categories.sortedBy { it.category }.mapNotNull {
            if (it.hasSubCategories) {
                val subcategories = getEquipmentModelTree(it.category)

                if (subcategories.isEmpty()) {
                    null
                } else {
                    EquipmentModelTree.Category(it, subcategories)
                }
            } else {
                val equipment = getModels(it.category)

                if (equipment.isEmpty()) {
                    null
                } else {
                    EquipmentModelTree.Category(
                        it,
                        equipment
                            .map { EquipmentModelTree.Model(it) }
                            .sortedBy { it.model.model } as List<EquipmentModelTree>
                    )
                }
            }
        }
    }
}

@Throws
private fun Database.saveEquipmentDocuments(
    categoryDocuments: List<EquipmentCategoryDocument>,
    modelDocuments: List<EquipmentModelDocument>,
    documentEtag: String?
) {
    val encoder = DictionaryEncoder()
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    val ttl = calendar.time

    this.inBatch {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id), SelectResult.all())
            .from(DataSource.database(this))

        for (result in query.execute()) {
            val id = result.getString(0)
            val doc: Document = this.getDocument(id)
            this.delete(doc)
        }

        val meta = encoder.encode(
            MetadataDocument(
                type = "DocumentMetadata",
                metadata = DocumentMetadata(documentEtag)
            )
        )
        val metaDoc = MutableDocument("DocumentMetadata", meta)
        this.save(metaDoc)
        this.setDocumentExpiration("DocumentMetadata", ttl)

        categoryDocuments.forEach { categoryDoc ->
            val data = encoder.encode(categoryDoc)
            val doc = MutableDocument(categoryDoc.category.category, data)
            this.save(doc)
            this.setDocumentExpiration(categoryDoc.category.category, ttl)
        }

        modelDocuments.forEach { modelDoc ->
            val data = encoder.encode(modelDoc)
            val doc = MutableDocument(modelDoc.name, data)
            this.save(doc)
            this.setDocumentExpiration(modelDoc.name, ttl)
        }
    }
}

private fun Database.getCategoryId(name: String): Int {
    return QueryBuilder
        .select(SelectResult.property("categoryId"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type")
                .equalTo(Expression.string("EquipmentCategory"))
                .and(
                    Expression.property("category.category")
                        .equalTo(Expression.string(name))
                )
        )
        .execute()
        .allResults()
        .first()
        .getInt("categoryId")
}
