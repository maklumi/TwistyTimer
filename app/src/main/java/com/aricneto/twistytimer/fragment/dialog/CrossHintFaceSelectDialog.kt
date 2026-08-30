package com.aricneto.twistytimer.fragment.dialog

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogCrossHintFaceSelectBinding
import com.aricneto.twistytimer.activity.MainActivity
import com.aricneto.twistytimer.utils.DefaultPrefs.getBoolean
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getBoolean

/**
 * Dialog that allows a user to select the faces where the cross hints will be shown
 */
class CrossHintFaceSelectDialog : DialogFragment() {
    private var binding: DialogCrossHintFaceSelectBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    // Number of faces that have hint enabled.
    // The code checks if it is >= 1 so the user can't have 0 faces enabled
    // (which would result in a counter-intuitive empty hint)
    var facesSelected: Int = 0

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        var isHintOn = true

        when (view.id) {
            R.id.top -> isHintOn = getBoolean(
                R.string.pk_cross_hint_top_enabled,
                getBoolean(R.bool.default_crossHintTopEnabled)
            )

            R.id.left -> isHintOn = getBoolean(
                R.string.pk_cross_hint_left_enabled,
                getBoolean(R.bool.default_crossHintLeftEnabled)
            )

            R.id.front -> isHintOn = getBoolean(
                R.string.pk_cross_hint_front_enabled, getBoolean(
                    R.bool.default_crossHintFrontEnabled
                )
            )

            R.id.right -> isHintOn = getBoolean(
                R.string.pk_cross_hint_right_enabled, getBoolean(
                    R.bool.default_crossHintRightEnabled
                )
            )

            R.id.back -> isHintOn = getBoolean(
                R.string.pk_cross_hint_back_enabled,
                getBoolean(R.bool.default_crossHintBackEnabled)
            )

            R.id.down -> isHintOn = getBoolean(
                R.string.pk_cross_hint_down_enabled,
                getBoolean(R.bool.default_crossHintDownEnabled)
            )
        }

        isHintOn = !isHintOn

        if (!(!isHintOn && facesSelected == 1)) {
            when (view.id) {
                R.id.top -> {
                    edit().putBoolean(R.string.pk_cross_hint_top_enabled, isHintOn).apply()
                    toggleSelected(binding!!.top, isHintOn)
                }

                R.id.left -> {
                    edit().putBoolean(R.string.pk_cross_hint_left_enabled, isHintOn).apply()
                    toggleSelected(binding!!.left, isHintOn)
                }

                R.id.front -> {
                    edit().putBoolean(R.string.pk_cross_hint_front_enabled, isHintOn).apply()
                    toggleSelected(binding!!.front, isHintOn)
                }

                R.id.right -> {
                    edit().putBoolean(R.string.pk_cross_hint_right_enabled, isHintOn).apply()
                    toggleSelected(binding!!.right, isHintOn)
                }

                R.id.back -> {
                    edit().putBoolean(R.string.pk_cross_hint_back_enabled, isHintOn).apply()
                    toggleSelected(binding!!.back, isHintOn)
                }

                R.id.down -> {
                    edit().putBoolean(R.string.pk_cross_hint_down_enabled, isHintOn).apply()
                    toggleSelected(binding!!.down, isHintOn)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCrossHintFaceSelectBinding.inflate(inflater, container, false)

        // Color cube
        setColor(
            binding!!.top,
            ("#" + Prefs.getString(R.string.pk_cube_top_color, "FFFFFF")).toColorInt()
        )
        setColor(
            binding!!.left,
            ("#" + Prefs.getString(R.string.pk_cube_left_color, "FF8B24")).toColorInt()
        )
        setColor(
            binding!!.front,
            ("#" + Prefs.getString(R.string.pk_cube_front_color, "02D040")).toColorInt()
        )
        setColor(
            binding!!.right,
            ("#" + Prefs.getString(R.string.pk_cube_right_color, "EC0000")).toColorInt()
        )
        setColor(
            binding!!.back,
            ("#" + Prefs.getString(R.string.pk_cube_back_color, "304FFE")).toColorInt()
        )
        setColor(
            binding!!.down,
            ("#" + Prefs.getString(R.string.pk_cube_down_color, "FDD835")).toColorInt()
        )

        // Set click listeners
        binding!!.top.setOnClickListener(clickListener)
        binding!!.left.setOnClickListener(clickListener)
        binding!!.front.setOnClickListener(clickListener)
        binding!!.right.setOnClickListener(clickListener)
        binding!!.back.setOnClickListener(clickListener)
        binding!!.down.setOnClickListener(clickListener)

        // Set face transparency based on current preference
        initSelected(
            binding!!.top,
            getBoolean(
                R.string.pk_cross_hint_top_enabled,
                getBoolean(R.bool.default_crossHintTopEnabled)
            )
        )
        initSelected(
            binding!!.left,
            getBoolean(
                R.string.pk_cross_hint_left_enabled,
                getBoolean(R.bool.default_crossHintLeftEnabled)
            )
        )
        initSelected(
            binding!!.front,
            getBoolean(
                R.string.pk_cross_hint_front_enabled,
                getBoolean(R.bool.default_crossHintFrontEnabled)
            )
        )
        initSelected(
            binding!!.right,
            getBoolean(
                R.string.pk_cross_hint_right_enabled,
                getBoolean(R.bool.default_crossHintRightEnabled)
            )
        )
        initSelected(
            binding!!.back,
            getBoolean(
                R.string.pk_cross_hint_back_enabled,
                getBoolean(R.bool.default_crossHintBackEnabled)
            )
        )
        initSelected(
            binding!!.down,
            getBoolean(
                R.string.pk_cross_hint_down_enabled,
                getBoolean(R.bool.default_crossHintDownEnabled)
            )
        )

        binding!!.buttonSave.setOnClickListener {
            if (activity is MainActivity) {
                (activity as MainActivity).onRecreateRequired()
            }
            dismiss()
        }

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding!!.getRoot()
    }

    /**
     * Initializes the selected faces and facesSelected count
     * Since facesSelected starts at 0, adding 1 to each face that has hints on will start
     * facesSelected on the correct number. If you were to initialize with toggleSelected, the count
     * will probably be wrong (or negative even)
     * @param face
     * The face to toggle alpha
     * @param isHintOn
     * True if face has hints enabled
     */
    private fun initSelected(face: View, isHintOn: Boolean) {
        if (isHintOn) {
            face.alpha = 1f
            facesSelected++
        } else {
            face.alpha = 0.2f
        }
    }

    /**
     * Toggles alpha value on face and changes faceSelected counter
     * 
     * @param face
     * The face to toggle alpha
     * @param isHintOn
     * True if face has hints enabled
     */
    private fun toggleSelected(face: View, isHintOn: Boolean) {
        if (isHintOn) {
            face.alpha = 1f
            facesSelected++
        } else {
            face.alpha = 0.2f
            facesSelected--
        }
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
        fun newInstance(): CrossHintFaceSelectDialog {
            return CrossHintFaceSelectDialog()
        }
    }
}
