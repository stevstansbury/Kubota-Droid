//
//  KubotaEquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.*
import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentService
import com.kubota.service.api.SearchModelType
import com.kubota.service.api.caseInsensitiveSort
import com.kubota.service.domain.*
import com.kubota.service.internal.couchbase.DictionaryDecoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import com.squareup.moshi.JsonDataException
import java.net.URL
import java.util.*

private data class FaultCodes(
    val faultCodes: List<FaultCode>
)

private data class EquipmentModels(
    val models: List<EquipmentModelRaw>
)

private data class EquipmentModelRaw(
    val model: String,
    val searchModel: String?,
    val modelDescription: String?,
    val modelHeroUrl: String?,
    val modelFullUrl: String?,
    val modelIconUrl: String?,
    val category: String,
    val categoryHeroUrl: String?,
    val categoryFullUrl: String?,
    val categoryIconUrl: String?,
    val subcategory: String,
    val subcategoryHeroUrl: String?,
    val subcategoryFullUrl: String?,
    val subcategoryIconUrl: String?,
    val guideUrl: String?,
    val manualEntries: List<ManualInfo>?,
    val warrantyUrl: URL?,
    val hasFaultCodes: Boolean,
    val hasMaintenanceSchedules: Boolean
)

private data class EquipmentCategoryDocument(
    val type: String,
    val parentCategory: String,
    val category: EquipmentCategory
)

private data class EquipmentModelDocument(
    val type: String,
    val name: String,
    val category: String,
    val model: EquipmentModel
)

private val SearchModelType.queryParams: QueryParameters
    get() {
        return when (this) {
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
    }

internal class KubotaEquipmentService(config: Config, private val couchbaseDb: Database?): HTTPService(config = config), EquipmentService {

    override fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>> {
        val params = queryParamsMultiValue(
            "code" to codes
        )
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.oneDay.interval)
        return service {
            // The compiler has a little hard time to infer the type in this case, so we have
            // to help it out by declaring a local val with the type.
            val p: Promise<FaultCodes> = this.get(
                route = "/api/faultCode/${model}",
                query = params,
                type = FaultCodes::class.java,
                cacheCriteria = criteria
            )
            return@service p.map { it.faultCodes }
        }
    }

    override fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>> {
        val criteria = CacheCriteria(policy = CachePolicy.useAgeReturnCacheIfError, age = CacheAge.oneDay.interval)

        val p = this.get(
            route = "/api/maintenanceSchedule/$model",
            query = null,
            type = Array<EquipmentMaintenance>::class.java,
            cacheCriteria = criteria
        )

        return p.map { it.toList() }
    }

    override fun getModel(model: String): Promise<EquipmentModel?> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
            val equipmentModel = this.couchbaseDb?.getModel(model = model)
            (
                if (equipmentModel != null) {
                    Promise.value(equipmentModel)
                } else {
                    this.getAllModels()
                        .map(on = DispatchExecutor.global) { rawModels ->
                            val documents = this.processRawModels(rawModels)
                            this.couchbaseDb?.saveEquipmentDocuments(documents)

                            val modelDocument = documents.second
                            modelDocument
                                .filter { it.name == model }
                                .map { it.model }
                                .firstOrNull()
                        }
                }
            ) as Promise<EquipmentModel?>
        }
    }

    override fun searchModels(type: SearchModelType): Promise<List<EquipmentModel>> {
        return service {
            this.get(route = "/api/models", query = type.queryParams, type = EquipmentModels::class.java)
                .map(on = DispatchExecutor.global) {
                    it.models.map { rawModel ->
                        EquipmentModel(
                            model = rawModel.model,
                            searchModel = rawModel.searchModel,
                            description = rawModel.modelDescription,
                            imageResources = this.createImageResources(
                                                    heroUrl = rawModel.modelHeroUrl,
                                                    iconUrl = rawModel.modelIconUrl,
                                                    fullUrl = rawModel.modelFullUrl
                                            ),
                            category = rawModel.category,
                            subcategory = rawModel.subcategory,
                            guideUrl = try { URL(rawModel.guideUrl) } catch(e: Throwable) { null },
                            manualInfo = rawModel.manualEntries ?: emptyList(),
                            warrantyUrl = rawModel.warrantyUrl,
                            hasFaultCodes = rawModel.hasFaultCodes,
                            hasMaintenanceSchedules =  rawModel.hasMaintenanceSchedules
                    )
                }
            }
        }
    }

    override fun scanSearchModels(type: SearchModelType): Promise<List<String>> {
        return service {
            this.get(route = "/api/models/scan", query = type.queryParams, type = Array<String>::class.java)
                .map(on = DispatchExecutor.global) { it.toList() }
        }
    }

    override fun getModels(category: String): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
            val models = this.couchbaseDb?.getModels(category = category)
            if (!models.isNullOrEmpty()) {
                Promise.value(models)
            } else {
                this.getAllModels()
                    .map(on = DispatchExecutor.global) { rawModels ->
                        val documents = this.processRawModels (rawModels)
                        this.couchbaseDb?.saveEquipmentDocuments(documents)

                        val modelDocuments = documents.second
                        modelDocuments
                                .filter { it.category == category }
                                .map { it.model }
                        }
            }
        }
        .map(on = DispatchExecutor.global) { it.caseInsensitiveSort { it.model } }
    }

    override fun getCategories(parentCategory: String?): Promise<List<EquipmentCategory>> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
                val categories = this.couchbaseDb?.getCategories(parentCategory = parentCategory)
                if (!categories.isNullOrEmpty()) {
                    Promise.value(categories)
                } else {
                    this.getAllModels()
                        .map(on = DispatchExecutor.global) { rawModels ->
                            val documents = this.processRawModels(rawModels)
                            this.couchbaseDb?.saveEquipmentDocuments(documents)

                            val categoryDocuments = documents.first
                            categoryDocuments
                                .filter { it.parentCategory == (parentCategory ?: "null") }
                                .map { it.category }
                        }
                }
            }
            .map(on = DispatchExecutor.global) { it.caseInsensitiveSort { it.category } }
    }

    private fun getAllModels(): Promise<List<EquipmentModelRaw>> {
        val p: Promise<EquipmentModels> = service {
            this.get(route = "/api/models", type = EquipmentModels::class.java)
        }
        return p.map(on = DispatchExecutor.global) { it.models }
    }

    private fun createImageResources(heroUrl: String?, iconUrl: String?, fullUrl: String?): ImageResources? {
        val heroURL = if (heroUrl.isNullOrEmpty()) null else try { URL(heroUrl) } catch (e: Throwable) { null }
        val iconURL = if (iconUrl.isNullOrEmpty()) null else try { URL(iconUrl) } catch (e: Throwable) { null }
        val fullURL = if (fullUrl.isNullOrEmpty()) null else try { URL(fullUrl) } catch (e: Throwable) { null }
        if (heroUrl == null && iconUrl == null && fullUrl == null) { return null }

        return ImageResources(heroUrl = heroURL, fullUrl = fullURL, iconUrl = iconURL)
    }

    private fun processRawModels(rawModels: List<EquipmentModelRaw>): Pair<List<EquipmentCategoryDocument>, List<EquipmentModelDocument>> {
        val categoryDocumentMap = mutableMapOf<String, EquipmentCategoryDocument>()
        val modelDocuments = mutableListOf<EquipmentModelDocument>()

        for (rawModel in rawModels) {
            if (!categoryDocumentMap.containsKey(rawModel.category)) {
                val categoryDocument = EquipmentCategoryDocument(
                        type = "EquipmentCategory",
                        parentCategory = "null",
                        category = EquipmentCategory(
                                        category = rawModel.category,
                                        parentCategory = null,
                                        hasSubCategories = true,
                                        imageResources = this.createImageResources(
                                            heroUrl = rawModel.categoryHeroUrl,
                                            iconUrl = rawModel.categoryIconUrl,
                                            fullUrl = rawModel.categoryFullUrl
                                        )
                                )
                    )
                categoryDocumentMap[rawModel.category] = categoryDocument
            }

            if (!categoryDocumentMap.containsKey(rawModel.subcategory)) {
                val categoryDocument = EquipmentCategoryDocument(
                        type = "EquipmentCategory",
                        parentCategory = rawModel.category,
                        category = EquipmentCategory(
                                        category = rawModel.subcategory,
                                        parentCategory = rawModel.category,
                                        hasSubCategories = false,
                                        imageResources = this.createImageResources(
                                            heroUrl = rawModel.subcategoryHeroUrl,
                                            iconUrl = rawModel.subcategoryIconUrl,
                                            fullUrl = rawModel.subcategoryFullUrl
                                        )
                                )
                    )
                categoryDocumentMap[rawModel.subcategory] = categoryDocument
            }

            val model = EquipmentModelDocument(
                            type = "EquipmentModel",
                            name = rawModel.model,
                            category = rawModel.subcategory,
                            model = EquipmentModel(
                                        model = rawModel.model,
                                        searchModel = rawModel.searchModel,
                                        description = rawModel.modelDescription,
                                        imageResources = this.createImageResources(
                                            heroUrl = rawModel.modelHeroUrl,
                                            iconUrl = rawModel.modelIconUrl,
                                            fullUrl = rawModel.modelFullUrl
                                        ),
                                        category = rawModel.category,
                                        subcategory = rawModel.subcategory,
                                        guideUrl = if (rawModel.guideUrl.isNullOrEmpty()) null else try { URL(rawModel.guideUrl) } catch (e: Throwable) { null },
                                        manualInfo = rawModel.manualEntries ?: emptyList(),
                                        warrantyUrl = rawModel.warrantyUrl,
                                        hasFaultCodes = rawModel.hasFaultCodes,
                                        hasMaintenanceSchedules =  rawModel.hasMaintenanceSchedules
                                    )
                        )

            modelDocuments.add(model)
        }

        return Pair(categoryDocumentMap.values.toList(), modelDocuments.toList())
    }

}

@Throws
private fun Database.getCategories(parentCategory: String?): List<EquipmentCategory> {
    val categories = mutableListOf<EquipmentCategory>()
    val query = QueryBuilder
            .select(SelectResult.property("category"))
            .from(DataSource.database(this))
            .where(
                Expression.property("type").equalTo(Expression.string("EquipmentCategory"))
                    .and(Expression.property("parentCategory").equalTo(Expression.string(parentCategory ?: "null")))
            )

    val decoder = DictionaryDecoder()
    for (result in query.execute()) {
        val dict = result.toMap()["category"] as Map<String, Any>
        val category = decoder.decode(EquipmentCategory::class.java, value = dict)
        if (category != null) {
            categories.add(category)
        }
    }
    return categories
}

@Throws
private fun Database.getModels(category: String): List<EquipmentModel> {
    val models = mutableListOf<EquipmentModel>()
    val query = QueryBuilder
            .select(SelectResult.property("model"))
            .from(DataSource.database(this))
            .where(
                Expression.property("type").equalTo(Expression.string("EquipmentModel"))
                    .and(Expression.property("category").equalTo(Expression.string(category)))
        )

    val decoder = DictionaryDecoder()
    for (result in query.execute()) {
        val dict = result.toMap()["model"] as Map<String, Any>
        try {
            val model = decoder.decode(EquipmentModel::class.java, value = dict)
            if (model != null) {
                models.add(model)
            }
        } catch (e: JsonDataException) {
            // return empty list so it re-caches model info
            return emptyList()
        }
    }
    return models
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
    for (result in query.execute()) {
        val dict = result.toMap()["model"] as Map<String, Any>
        return decoder.decode(EquipmentModel::class.java, value = dict)
    }
    return null
}

@Throws
private fun Database.saveEquipmentDocuments(documents: Pair<List<EquipmentCategoryDocument>, List<EquipmentModelDocument>>) {
    val categoryDocuments = documents.first
    val modelDocuments = documents.second
    val encoder = DictionaryEncoder()
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    val ttl = calendar.time

    this.inBatch {
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
