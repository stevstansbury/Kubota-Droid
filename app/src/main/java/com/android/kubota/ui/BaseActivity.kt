package com.android.kubota.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.kubota_toolbar.*
import kotlinx.android.synthetic.main.kubota_toolbar_with_logo.*
import kotlinx.android.synthetic.main.toolbar_with_progress_bar.*

abstract class BaseActivity: AppCompatActivity(), ControlledActivity {

    companion object {
        private const val TOOLBAR_WITH_LOGO_VISIBLE = "toolbar_with_logo_visible"
        private const val TOOLBAR_PROGRESSBAR_VISIBLE = "toolbar_progressbar_visible"
        private const val TOOLBAR_DISPLAY_HOME_AS_UP = "toolbar_display_home_as_up"
    }

    private lateinit var toolbarController: ToolbarController

    protected open val rootTag: String? = null

    @LayoutRes
    abstract fun getLayOutResId(): Int

    @IdRes
    abstract fun getFragmentContainerId(): Int
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(getLayOutResId())
        setSupportActionBar(toolbar)
        toolbarController = ToolbarControllerFactory.createToolbarController(this)
        supportFragmentManager.addOnBackStackChangedListener(toolbarController.getOnBackStackChangedListener())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.let {
            if (it.getInt(TOOLBAR_WITH_LOGO_VISIBLE, View.GONE) == View.VISIBLE) {
                showKubotaLogoToolbar()
            } else {
                showRegularToolbar()
            }
            toolbarProgressBar.visibility = it.getInt(TOOLBAR_PROGRESSBAR_VISIBLE, View.VISIBLE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(TOOLBAR_WITH_LOGO_VISIBLE, toolbarWithLogo.visibility)
        outState?.putInt(TOOLBAR_PROGRESSBAR_VISIBLE, toolbarProgressBar.visibility)
        supportActionBar?.let {
            // Determine which display options are enabled
            val isHomeAsUpEnabled = (it.displayOptions and ActionBar.DISPLAY_HOME_AS_UP) !== 0
            outState?.putBoolean(TOOLBAR_DISPLAY_HOME_AS_UP, isHomeAsUpEnabled)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (item.itemId){
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun makeSnackbar(): Snackbar? {
        supportFragmentManager.findFragmentById(getFragmentContainerId())?.view?.let {
            return Snackbar.make(it, "", Snackbar.LENGTH_SHORT)
        }
        return null
    }

    // TODO: the below show methods depend on the activity_main.xml which is provided by an implementation of this class
    // TODO: refactor to find the toolbars by ID or provided them from BaseActivity

    override fun showKubotaLogoToolbar() {
        supportActionBar?.hide()
        toolbarWithLogo.visibility = View.VISIBLE
    }

    override fun showRegularToolbar() {
        toolbarWithLogo.visibility = View.GONE
        supportActionBar?.show()
    }

    override fun showProgressBar() {
        toolbarProgressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        toolbarProgressBar.visibility = View.INVISIBLE
    }

    override fun setDisplayHomeAsUp(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    override fun addFragmentToBackStack(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .replace(getFragmentContainerId(), fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun clearBackStack() {
        supportFragmentManager.popBackStackImmediate(rootTag, 0)
    }
}
