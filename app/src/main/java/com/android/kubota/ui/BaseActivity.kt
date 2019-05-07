package com.android.kubota.ui

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
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
            .replace(getFragmentContainerId(), fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun clearBackStack() {
        supportFragmentManager.popBackStackImmediate(rootTag, 0)
    }
}
