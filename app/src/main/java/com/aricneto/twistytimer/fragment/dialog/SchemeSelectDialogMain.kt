package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogSchemeSelectMainBinding
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.spans.ChromaDialogFixed
import com.aricneto.twistytimer.spans.ChromaDialogFixed.ColorMode
import com.aricneto.twistytimer.spans.ChromaDialogFixed.IndicatorMode
import com.aricneto.twistytimer.spans.ChromaDialogFixed.OnColorSelectedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * Created by Ari on 09/02/2016.
 */
class SchemeSelectDialogMain : DialogFragment() {
    private var binding: DialogSchemeSelectMainBinding? = null
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    private val clickListener = View.OnClickListener { view ->
        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = sp.edit()
        var currentHex = "FFFFFF"
        when (view.id) {
            R.id.top -> currentHex = sp.getString("cubeTop", "FFFFFF")!!
            R.id.left -> currentHex = sp.getString("cubeLeft", "FF8B24")!!
            R.id.front -> currentHex = sp.getString("cubeFront", "02D040")!!
            R.id.right -> currentHex = sp.getString("cubeRight", "EC0000")!!
            R.id.back -> currentHex = sp.getString("cubeBack", "304FFE")!!
            R.id.down -> currentHex = sp.getString("cubeDown", "FDD835")!!
        }

        ChromaDialogFixed.Builder()
            .initialColor(("#$currentHex").toColorInt())
            .colorMode(ColorMode.RGB)
            .indicatorMode(IndicatorMode.HEX)
            .onColorSelected(object : OnColorSelectedListener {
                override fun onColorSelected(@ColorInt color: Int) {
                    val hexColor =
                        Integer.toHexString(color).uppercase(Locale.getDefault()).substring(2)
                    when (view.id) {
                        R.id.top -> {
                            setColor(binding!!.top, ("#$hexColor").toColorInt())
                            editor.putString("cubeTop", hexColor)
                        }

                        R.id.left -> {
                            setColor(binding!!.left, ("#$hexColor").toColorInt())
                            editor.putString("cubeLeft", hexColor)
                        }

                        R.id.front -> {
                            setColor(binding!!.front, ("#$hexColor").toColorInt())
                            editor.putString("cubeFront", hexColor)
                        }

                        R.id.right -> {
                            setColor(binding!!.right, ("#$hexColor").toColorInt())
                            editor.putString("cubeRight", hexColor)
                        }

                        R.id.back -> {
                            setColor(binding!!.back, ("#$hexColor").toColorInt())
                            editor.putString("cubeBack", hexColor)
                        }

                        R.id.down -> {
                            setColor(binding!!.down, ("#$hexColor").toColorInt())
                            editor.putString("cubeDown", hexColor)
                        }
                    }
                    editor.apply()
                }
            })
            .create()
            .show(parentFragmentManager, "ChromaDialog")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSchemeSelectMainBinding.inflate(inflater, container, false)

        mContext = context

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setColor(binding!!.top, ("#" + sp.getString("cubeTop", "FFFFFF")).toColorInt())
        setColor(binding!!.left, ("#" + sp.getString("cubeLeft", "FF8B24")).toColorInt())
        setColor(binding!!.front, ("#" + sp.getString("cubeFront", "02D040")).toColorInt())
        setColor(binding!!.right, ("#" + sp.getString("cubeRight", "EC0000")).toColorInt())
        setColor(binding!!.back, ("#" + sp.getString("cubeBack", "304FFE")).toColorInt())
        setColor(binding!!.down, ("#" + sp.getString("cubeDown", "FDD835")).toColorInt())

        binding!!.top.setOnClickListener(clickListener)
        binding!!.left.setOnClickListener(clickListener)
        binding!!.front.setOnClickListener(clickListener)
        binding!!.right.setOnClickListener(clickListener)
        binding!!.back.setOnClickListener(clickListener)
        binding!!.down.setOnClickListener(clickListener)

        binding!!.reset.setOnClickListener { _: View? ->
            MaterialAlertDialogBuilder(mContext!!)
                .setMessage(R.string.reset_colorscheme)
                .setPositiveButton(
                    R.string.action_reset_colorscheme
                ) { _: DialogInterface?, _: Int ->
                    sp.edit {
                        putString("cubeTop", "FFFFFF")
                        putString("cubeLeft", "EF6C00")
                        putString("cubeFront", "02D040")
                        putString("cubeRight", "EC0000")
                        putString("cubeBack", "304FFE")
                        putString("cubeDown", "FDD835")
                    }
                    setColor(binding!!.top, "#FFFFFF".toColorInt())
                    setColor(binding!!.left, "#EF6C00".toColorInt())
                    setColor(binding!!.front, "#02D040".toColorInt())
                    setColor(binding!!.right, "#EC0000".toColorInt())
                    setColor(binding!!.back, "#304FFE".toColorInt())
                    setColor(binding!!.down, "#FDD835".toColorInt())
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

        binding!!.done.setOnClickListener { _: View? ->
            if (activity is MainActivity) {
                (activity as MainActivity).onRecreateRequired()
            }
            dismiss()
        }

        return binding!!.root
    }

    private fun setColor(view: View, color: Int) {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.square)
        var wrap = DrawableCompat.wrap(drawable!!)
        DrawableCompat.setTint(wrap, color)
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY)
        wrap = wrap.mutate()
        view.background = wrap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): SchemeSelectDialogMain {
            return SchemeSelectDialogMain()
        }
    }
}
