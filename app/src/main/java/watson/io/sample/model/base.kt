package watson.io.sample.model

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface PreferenceStorage {
    var onboardingCompleted: Boolean
    var snackbarIsStopped: Boolean
    var observableSnackbarIsStopped: LiveData<Boolean>
    var selectedFilters: String?
}

class BooleanPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.edit { putBoolean(name, value) }
    }
}

class StringPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: String?
) : ReadWriteProperty<Any, String?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return preferences.getString(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.edit { putString(name, value) }
    }
}

class SharedPreferenceStorage(context: Context) : PreferenceStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val observableShowSnackbarResult = MutableLiveData<Boolean>()
    override var onboardingCompleted by BooleanPreference(prefs, PREF_ONBOARDING, false)


    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_SNACKBAR_IS_STOPPED) {
            observableShowSnackbarResult.value = snackbarIsStopped
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(changeListener)
    }

    override var snackbarIsStopped by BooleanPreference(prefs, PREF_SNACKBAR_IS_STOPPED, false)

    override var observableSnackbarIsStopped: LiveData<Boolean>
        get() {
            observableShowSnackbarResult.value = snackbarIsStopped
            return observableShowSnackbarResult
        }
        set(value) = throw IllegalAccessException("This property can't be changed")


    override var selectedFilters by StringPreference(prefs, PREF_SELECTED_FILTERS, null)

    companion object {
        const val PREFS_NAME = "iosched"
        const val PREF_ONBOARDING = "pref_onboarding"
        const val PREF_SNACKBAR_IS_STOPPED = "pref_snackbar_is_stopped"
        const val PREF_SELECTED_FILTERS = "pref_selected_filters"
    }

    fun registerOnPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
}