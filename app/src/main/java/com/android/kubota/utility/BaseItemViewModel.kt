package com.android.kubota.utility

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class ItemViewModel : NotifyPropertyChange {
    override val propertyChangeRegistry = PropertyChangeRegistry()
}

interface NotifyPropertyChange : Observable {
    val propertyChangeRegistry: PropertyChangeRegistry

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        propertyChangeRegistry.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        propertyChangeRegistry.remove(callback)
    }

    fun notifyPropertyChanged(propertyId: Int) {
        propertyChangeRegistry.notifyChange(this, propertyId)
    }
}

fun <T> notifyChange(initialValue: T, vararg ids: Int) =
    NotifyChangeDelegate(initialValue, ids)

class NotifyChangeDelegate<T>(
    initialValue: T,
    private val ids: IntArray
) : ReadWriteProperty<NotifyPropertyChange, T> {
    private var value = initialValue

    override fun getValue(thisRef: NotifyPropertyChange, property: KProperty<*>) = this.value

    override fun setValue(thisRef: NotifyPropertyChange, property: KProperty<*>, value: T) {
        this.value = value
        ids.forEach { thisRef.notifyPropertyChanged(it) }
    }
}
