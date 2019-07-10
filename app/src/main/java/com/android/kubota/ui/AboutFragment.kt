package com.android.kubota.ui

import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.kubota.R

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.about_fragment_title)
        val view = inflater.inflate(R.layout.fragment_about, null)

        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            val appName = getString(R.string.app_name)
            val unStyledText = getString(R.string.app_name_and_version, appName, versionName)
            val appNameIndex = unStyledText.indexOf(appName)
            val versionNameIndex = unStyledText.indexOf(versionName)
            val spannableString = SpannableString(unStyledText)
            spannableString.setSpan(TextAppearanceSpan(requireContext(), R.style.AboutTitleStyle), appNameIndex,
                appNameIndex + appName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(TextAppearanceSpan(requireContext(), R.style.AppVersionStyle), versionNameIndex,
                versionNameIndex + versionName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.findViewById<TextView>(R.id.about_title).text = spannableString
        } catch (e: NameNotFoundException) {
            val spanBuilder = SpannableStringBuilder()
            spanBuilder.append(getString(R.string.app_name), StyleSpan(R.style.AboutTitleStyle), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.findViewById<TextView>(R.id.about_title).text = spanBuilder
        }

        return view
    }
}