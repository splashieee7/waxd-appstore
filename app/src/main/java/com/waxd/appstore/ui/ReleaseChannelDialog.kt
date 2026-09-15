package com.waxd.appstore.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.waxd.appstore.core.ReleaseChannel
import com.waxd.appstore.PackageStates
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.waxd.appstore.R

class ReleaseChannelDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val pkgName = navArgs<ReleaseChannelDialogArgs>().value.pkgName
        val pkgState = PackageStates.getPackageState(pkgName)

        val channels = ReleaseChannel.entries.reversed()

        val channelNames = channels.map { resources.getText(it.uiName) }
        val curIndex = channels.indexOf(pkgState.preferredReleaseChannel())

        return MaterialAlertDialogBuilder(requireContext()).run {
            setTitle(R.string.release_channel)
            setSingleChoiceItems(channelNames.toTypedArray(), curIndex) { _, index ->
                PackageStates.setPreferredChannelOverride(pkgState, channels[index])
                findNavController().popBackStack()
            }
            create()
        }
    }
}
