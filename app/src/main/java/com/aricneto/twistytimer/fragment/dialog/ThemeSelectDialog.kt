package com.aricneto.twistytimer.fragment.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.DialogThemeSelectBinding
import com.aricneto.twistytimer.items.Theme
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getString
import com.aricneto.twistytimer.utils.TTIntent.ACTION_CHANGED_THEME
import com.aricneto.twistytimer.utils.TTIntent.CATEGORY_UI_INTERACTIONS
import com.aricneto.twistytimer.utils.TTIntent.broadcast
import com.aricneto.twistytimer.utils.ThemeUtils
import com.aricneto.twistytimer.utils.ThemeUtils.allThemes
import com.aricneto.twistytimer.utils.ThemeUtils.dpToPix
import com.aricneto.twistytimer.utils.ThemeUtils.fetchAttrColor
import com.aricneto.twistytimer.utils.ThemeUtils.fetchBackgroundGradient
import com.aricneto.twistytimer.utils.ThemeUtils.fetchStyleableAttr
import com.google.android.material.color.MaterialColors

/**
 * Created by Ari on 09/02/2016.
 */
class ThemeSelectDialog : DialogFragment() {
    private var binding: DialogThemeSelectBinding? = null
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogThemeSelectBinding.inflate(inflater, container, false)

        mContext = context

        dialog!!.window!!.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        binding!!.list.setHasFixedSize(true)
        binding!!.list2.setHasFixedSize(true)

        val themeLayoutManager = GridLayoutManager(mContext, 4, GridLayoutManager.VERTICAL, false)
        val textLayoutManager = GridLayoutManager(mContext, 4, GridLayoutManager.VERTICAL, false)

        binding!!.list.setLayoutManager(themeLayoutManager)
        binding!!.list2.setLayoutManager(textLayoutManager)

        val themeListAdapter = ThemeListAdapter(allThemes, mContext!!)
        val textStyleListAdapter =
            TextStyleListAdapter(ThemeUtils.getAllTextStyles(mContext!!), mContext!!)
        binding!!.list.setAdapter(themeListAdapter)
        binding!!.list2.setAdapter(textStyleListAdapter)

        binding!!.dismiss.setOnClickListener { dismiss() }

        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): ThemeSelectDialog {
            return ThemeSelectDialog()
        }
    }
}

internal class ThemeListAdapter(
    private val themeSet: Array<Theme?>,
    private val mContext: Context
) : RecyclerView.Adapter<ThemeListAdapter.CardViewHolder?>() {
    private val cornerRadius: Int = dpToPix(mContext, 8f)
    private val strokeWidth: Int = dpToPix(mContext, 1f)

    var currentTheme: String? = getString(R.string.pk_theme, "indigo")

    internal class CardViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        var themeCard: View = view.findViewById(R.id.card)
        var themeTitle: TextView = view.findViewById(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme_select_card, parent, false)

        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        // Create gradient drawable
        val gradientDrawable = fetchBackgroundGradient(mContext, themeSet[position]!!.resId)
        gradientDrawable.cornerRadius = cornerRadius.toFloat()
        gradientDrawable.setStroke(strokeWidth, Color.BLACK)

        // Set card title and background
        holder.themeTitle.text = themeSet[position]!!.name
        holder.themeCard.background = gradientDrawable

        val textColor = MaterialColors.getColor(mContext, R.attr.colorOnSurface, Color.BLACK)

        if (themeSet[position]!!.prefName == currentTheme) {
            holder.themeTitle.setBackgroundResource(R.drawable.outline_background_card_warn)
            holder.themeTitle.setTextColor(Color.BLACK)
        } else {
            holder.themeTitle.background = null
            holder.themeTitle.setTextColor(textColor)
        }

        // Create onClickListener
        holder.themeCard.setOnClickListener {

            val newTheme = themeSet[position]!!.prefName

            if (newTheme != currentTheme) {
                edit().putString(R.string.pk_theme, newTheme).apply()
                // Reset text style
                edit().putString(R.string.pk_text_style, "default").apply()

                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_THEME)
            }
        }
    }

    override fun getItemCount(): Int {
        return themeSet.size
    }
}

internal class TextStyleListAdapter(
    private val themeSet: Array<Theme?>,
    private val mContext: Context
) : RecyclerView.Adapter<TextStyleListAdapter.CardViewHolder?>() {
    private val cornerRadius: Int = dpToPix(mContext, 8f)
    private val strokeWidth: Int = dpToPix(mContext, 1f)

    private val currentTextStyle = getString(R.string.pk_text_style, "default")

    val textColor = MaterialColors.getColor(mContext, R.attr.colorOnSurface, Color.BLACK)
    var colorPrimary: Int = textColor

    internal class CardViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        var themeCard: TextView = view.findViewById(R.id.card)
        var themeTitle: TextView = view.findViewById(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_text_style_card, parent, false)

        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        // Create gradient drawable
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.TRANSPARENT)
        gradientDrawable.cornerRadius = cornerRadius.toFloat()
        gradientDrawable.setStroke(strokeWidth, fetchAttrColor(mContext, R.attr.colorOnSurface))

        // Set card title and background
        holder.themeTitle.text = themeSet[position]!!.name
        holder.themeCard.background = gradientDrawable
        holder.themeCard.setTextColor(
            fetchStyleableAttr(
                mContext, themeSet[position]!!.resId,
                intArrayOf(androidx.appcompat.R.attr.colorPrimary),
                0,
                androidx.appcompat.R.attr.colorPrimary
            )
        )

        val textColor = MaterialColors.getColor(mContext, R.attr.colorOnSurface, Color.BLACK)

        if (themeSet[position]!!.prefName == currentTextStyle) {
            holder.themeTitle.setBackgroundResource(R.drawable.outline_background_card_warn)
            holder.themeTitle.setTextColor(Color.BLACK)
        } else {
            holder.themeTitle.background = null
            holder.themeTitle.setTextColor(textColor)
        }

        // Create onClickListener
        holder.themeCard.setOnClickListener {

            val newTheme = themeSet[position]!!.prefName

            if (newTheme != currentTextStyle) {
                edit().putString(R.string.pk_text_style, newTheme).apply()

                broadcast(CATEGORY_UI_INTERACTIONS, ACTION_CHANGED_THEME)
            }
        }
    }

    override fun getItemCount(): Int {
        return themeSet.size
    }
}

