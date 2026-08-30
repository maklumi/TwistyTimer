package com.aricneto.twistytimer.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.style.ImageSpan
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Theme

/**
 * Utility class to make theming easier.
 */
object ThemeUtils {
    const val THEME_INDIGO: String = "indigo"
    const val THEME_PURPLE: String = "purple"
    const val THEME_TEAL: String = "teal"
    const val THEME_PINK: String = "pink"
    const val THEME_RED: String = "red"
    const val THEME_BROWN: String = "brown"
    const val THEME_BLUE: String = "blue"
    const val THEME_CYAN: String = "cyan"
    const val THEME_LIGHT_BLUE: String = "light_blue"
    const val THEME_BLACK: String = "black"
    const val THEME_ORANGE: String = "orange"
    const val THEME_GREEN: String = "green"
    const val THEME_LIGHT_GREEN: String = "light_green"
    const val THEME_DEEPPURPLE: String = "deeppurple"
    const val THEME_BLUEGRAY: String = "bluegray"
    const val THEME_WHITE: String = "white"
    const val THEME_YELLOW: String = "yellow"
    const val THEME_WHITE_GREEN: String = "white_green"
    const val THEME_DAWN: String = "dawn"
    const val THEME_BLUY_GRAY: String = "bluy_gray"
    const val THEME_TURTLY_SEA: String = "turtly_sea"
    const val THEME_PIXIE_FALLS: String = "pixie_falls"
    const val THEME_WANDERING_DUSK: String = "wandering_dusk"
    const val THEME_SPOTTY_GUY: String = "spotty_guy"


    const val TEXT_DEFAULT: String = "default"
    const val TEXT_PESSOA: String = "pessoa"
    const val TEXT_BURGESS: String = "burgess"
    const val TEXT_LOU: String = "lou"
    const val TEXT_BOWIE: String = "bowie"
    const val TEXT_BRIE: String = "brie"
    const val TEXT_MATSSON: String = "matsson"
    const val TEXT_ISAKOV: String = "isakov"
    const val TEXT_ADAMS: String = "adams"
    const val TEXT_IRWIN: String = "irwin"
    const val TEXT_TARKOVSKY: String = "tarkovsky"
    const val TEXT_EBERT: String = "ebert"
    const val TEXT_TOLKIEN: String = "tolkien"
    const val TEXT_ASIMOV: String = "asimov"
    const val TEXT_KUBRICK: String = "kubrick"

    @JvmStatic
    val preferredTheme: Int
        /**
         * Gets the user's preferred theme. This is the theme that has been selected and saved to the
         * settings (or the default theme); it is not necessarily the same as the theme that is
         * currently applied to the user interface.
         * 
         * @return The user's chosen preferred theme.
         */
        get() = getThemeStyleRes(
            Prefs.getString(
                R.string.pk_theme,
                "indigo"
            )!!
        )

    val preferredTextStyle: Int
        get() = getThemeStyleRes(
            Prefs.getString(
                R.string.pk_text_style,
                "default"
            )!!
        )

    fun getThemeStyleRes(theme: String): Int {
        when (theme) {
            THEME_INDIGO -> return R.style.DefaultTheme
            THEME_PURPLE -> return R.style.PurpleTheme
            THEME_TEAL -> return R.style.TealTheme
            THEME_PINK -> return R.style.PinkTheme
            THEME_RED -> return R.style.RedTheme
            THEME_BROWN -> return R.style.BrownTheme
            THEME_BLUE -> return R.style.BlueTheme
            THEME_CYAN -> return R.style.CyanTheme
            THEME_LIGHT_BLUE -> return R.style.LightBlueTheme
            THEME_BLACK -> return R.style.BlackTheme
            THEME_ORANGE -> return R.style.OrangeTheme
            THEME_GREEN -> return R.style.GreenTheme
            THEME_LIGHT_GREEN -> return R.style.LightGreenTheme
            THEME_DEEPPURPLE -> return R.style.DeepPurpleTheme
            THEME_BLUEGRAY -> return R.style.BlueGrayTheme
            THEME_WHITE -> return R.style.WhiteTheme
            THEME_YELLOW -> return R.style.YellowTheme
            THEME_WHITE_GREEN -> return R.style.WhiteGreenTheme
            THEME_DAWN -> return R.style.DawnTheme
            THEME_BLUY_GRAY -> return R.style.BluyGray
            THEME_TURTLY_SEA -> return R.style.TurtlySea
            THEME_PIXIE_FALLS -> return R.style.PixieFalls
            THEME_WANDERING_DUSK -> return R.style.WanderingDusk
            THEME_SPOTTY_GUY -> return R.style.SpottyGuy
            TEXT_DEFAULT -> return preferredTheme
            TEXT_PESSOA -> return R.style.TextStylePessoa
            TEXT_BURGESS -> return R.style.TextStyleBurgess
            TEXT_LOU -> return R.style.TextStyleLou
            TEXT_BOWIE -> return R.style.TextStyleBowie
            TEXT_BRIE -> return R.style.TextStyleBrie
            TEXT_MATSSON -> return R.style.TextStyleMatsson
            TEXT_ISAKOV -> return R.style.TextStyleIsakov
            TEXT_ADAMS -> return R.style.TextStyleAdams
            TEXT_IRWIN -> return R.style.TextStyleIrwin
            TEXT_TARKOVSKY -> return R.style.TextStyleTarkovsky
            TEXT_EBERT -> return R.style.TextStyleEbert
            TEXT_TOLKIEN -> return R.style.TextStyleTolkien
            TEXT_ASIMOV -> return R.style.TextStyleAsimov
            TEXT_KUBRICK -> return R.style.TextStyleKubrick
            else -> return R.style.DefaultTheme
        }
    }

    val allThemes: Array<Theme?>
        /**
         * Used to populate theme select dialogs.
         * 
         * @return an array containing all available themes
         */
        get() {
            val themes =
                arrayOf<Theme?>(
                    Theme(
                        THEME_INDIGO,
                        "Hazy Blues"
                    ),
                    Theme(
                        THEME_GREEN,
                        "What... Green?"
                    ),
                    Theme(
                        THEME_SPOTTY_GUY,
                        "Spotty Guy"
                    ),
                    Theme(
                        THEME_BLACK,
                        "Simply Black"
                    ),
                    Theme(
                        THEME_WHITE,
                        "Simply White"
                    ),
                    Theme(
                        THEME_YELLOW,
                        "Notably Yellow"
                    ),
                    Theme(
                        THEME_BLUY_GRAY,
                        "Bluy Gray"
                    ),
                    Theme(
                        THEME_TURTLY_SEA,
                        "Turtly Sea"
                    ),
                    Theme(
                        THEME_PIXIE_FALLS,
                        "Pixie Falls"
                    ),
                    Theme(
                        THEME_RED,
                        "Oof Hot"
                    ),
                    Theme(
                        THEME_WANDERING_DUSK,
                        "Wandy Dusk"
                    ),
                    Theme(
                        THEME_DAWN,
                        "Relaxing Dawn"
                    ),
                    Theme(
                        THEME_DEEPPURPLE,
                        "Quite Purply"
                    ),
                    Theme(
                        THEME_BLUE,
                        "Even Purplier"
                    ),
                    Theme(
                        THEME_PURPLE,
                        "Definitely Purple"
                    ),
                    Theme(
                        THEME_ORANGE,
                        "Tantalizing Torange"
                    ),
                    Theme(
                        THEME_PINK,
                        "Pinky Promises"
                    ),
                    Theme(
                        THEME_BROWN,
                        "Delicious Brownie"
                    ),
                    Theme(
                        THEME_TEAL,
                        "Earthy Teal"
                    ),
                    Theme(
                        THEME_LIGHT_GREEN,
                        "Greeny Gorilla"
                    ),
                    Theme(
                        THEME_LIGHT_BLUE,
                        "Lightly Skyish"
                    ),
                    Theme(
                        THEME_CYAN,
                        "Cyanic Teal"
                    ),
                    Theme(
                        THEME_BLUEGRAY,
                        "Icy Hills"
                    ),
                    Theme(
                        THEME_WHITE_GREEN,
                        "Greeny Everest"
                    )
                )
            return themes
        }

    fun getAllTextStyles(context: Context): Array<Theme?> {
        val styles = arrayOf<Theme?>(
            Theme(TEXT_DEFAULT, context.getString(R.string.action_default)),
            Theme(TEXT_PESSOA, "Pessoa"),
            Theme(TEXT_LOU, "Lou"),
            Theme(TEXT_BURGESS, "Burgess"),
            Theme(TEXT_BOWIE, "Bowie"),
            Theme(TEXT_BRIE, "Brie"),
            Theme(TEXT_MATSSON, "Matsson"),
            Theme(TEXT_ISAKOV, "Isakov"),
            Theme(TEXT_ADAMS, "Adams"),
            Theme(TEXT_IRWIN, "Irwin"),
            Theme(TEXT_TARKOVSKY, "Tarkovsky"),
            Theme(TEXT_EBERT, "Ebert"),
            Theme(TEXT_TOLKIEN, "Tolkien"),
            Theme(TEXT_ASIMOV, "Asimov"),
            Theme(TEXT_KUBRICK, "Kubrick"),
        )
        return styles
    }

    /**
     * Returns a [GradientDrawable] containing a linear gradient with the given style's colors
     * 
     * @param context Context
     * @param style The style resource contaning the background color definition (colorMainGradient[Start|End])
     * @return [GradientDrawable] containing a linear gradient with the given style's colors
     */
    @JvmStatic
    fun fetchBackgroundGradient(context: Context, @StyleRes style: Int): GradientDrawable {
        val gradientColors = context.obtainStyledAttributes(style, R.styleable.BaseTwistyTheme)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                gradientColors.getColor(
                    R.styleable.BaseTwistyTheme_colorMainGradientStart,
                    Color.BLUE
                ),
                gradientColors.getColor(
                    R.styleable.BaseTwistyTheme_colorMainGradientEnd,
                    Color.BLUE
                )
            )
        )

        gradientColors.recycle()

        return gradientDrawable
    }

    /**
     * Fetches and returns a Styleable value
     * @param context
     * @param styleable The styleable to fetch
     * @param style Property of the styleable to fetch
     * @param defaultAttr default attr to return if styleable can't be fetched
     * @return
     */
    fun fetchStyleableAttr(
        context: Context,
        @StyleRes style: Int,
        @StyleableRes styleable: IntArray,
        @StyleableRes styleableStyle: Int,
        @AttrRes defaultAttr: Int
    ): Int {
        val textColors = context.obtainStyledAttributes(style, styleable)

        val color = textColors.getColor(styleableStyle, fetchAttrColor(context, defaultAttr))
        textColors.recycle()

        return color
    }

    /**
     * Gets a color from an attr resource value
     * 
     * @param context Context
     * @param attrRes The attribute resource (ex. R.attr.colorPrimary)
     * @return @ColorRes
     */
    @JvmStatic
    fun fetchAttrColor(context: Context, @AttrRes attrRes: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attrRes, value, true)
        return value.data
    }

    /**
     * Gets a boolean from an attr resource value
     * 
     * @param context Context
     * @return @ColorRes
     */
    fun fetchAttrBool(context: Context, @StyleRes style: Int, @StyleableRes boolRes: Int): Boolean {
        val bool = context.obtainStyledAttributes(style, R.styleable.BaseTwistyTheme)

        val finalBool = bool.getBoolean(boolRes, false)
        bool.recycle()

        return finalBool
    }

    fun fetchAttrDrawable(context: Context, @AttrRes attrRes: Int): Drawable? {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attrRes, outValue, true)
        return ContextCompat.getDrawable(context, outValue.resourceId)
    }

    fun dpToPix(context: Context, dp: Float): Int {
        return (TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics
        )).toInt()
    }

    /**
     * Fetches a drawable from a resource id (can be a vector drawable),
     * tints with {@param colorAttrRes} and returns it.
     * @param context [Context]
     * @param drawableRes resource id for the drawable
     * @param colorAttrRes attr res id for the tint
     * @return a tinted drawable
     */
    fun fetchTintedDrawable(
        context: Context,
        @DrawableRes drawableRes: Int,
        @AttrRes colorAttrRes: Int
    ): Drawable {
        val drawable =
            DrawableCompat.wrap(ContextCompat.getDrawable(context, drawableRes)!!).mutate()
        DrawableCompat.setTint(drawable, fetchAttrColor(context, colorAttrRes))
        drawable.invalidateSelf()
        return drawable
    }

    /**
     * Creates and returns a [GradientDrawable] shape with custom radius and colors.
     * @param context Current context
     * @param backgroundColors 2 or 3 int array with the shape background colors
     * @param strokeColor Color of the shape stroke
     * @param cornerRadius Radius of the shape in dp
     * @param strokeWidth Stroke width in dp
     * @return A [GradientDrawable] with the set arguments
     */
    fun createSquareDrawable(
        context: Context,
        backgroundColors: IntArray?,
        strokeColor: Int,
        cornerRadius: Int,
        strokeWidth: Float
    ): GradientDrawable {
        val gradientDrawable =
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, backgroundColors)
        gradientDrawable.setStroke(dpToPix(context, strokeWidth), strokeColor)
        gradientDrawable.cornerRadius = dpToPix(context, cornerRadius.toFloat()).toFloat()

        return gradientDrawable
    }

    /**
     * Creates and returns a [GradientDrawable] shape with custom radius and colors.
     * @param context Current context
     * @param backgroundColor Shape background color
     * @param strokeColor Color of the shape stroke
     * @param cornerRadius Radius of the shape in dp
     * @param strokeWidth Stroke width in dp
     * @return A [GradientDrawable] with the set arguments
     */
    fun createSquareDrawable(
        context: Context,
        backgroundColor: Int,
        strokeColor: Int,
        cornerRadius: Int,
        strokeWidth: Float
    ): GradientDrawable {
        val colors = intArrayOf(backgroundColor, backgroundColor)
        return createSquareDrawable(context, colors, strokeColor, cornerRadius, strokeWidth)
    }

    /**
     * Creates and returns a [GradientDrawable] shape with custom radius and colors.
     * Pass 0 to either of the color arguments to make it transparent
     * @param context Current context
     * @param backgroundColor Shape background color
     * @param strokeColor Color of the shape stroke
     * @param cornerRadius Radius of the shape in dp
     * @param strokeWidth Stroke width in dp
     * @return A [GradientDrawable] with the set arguments
     */
    fun createSquareDrawableAttr(
        context: Context,
        backgroundColor: Int,
        strokeColor: Int,
        cornerRadius: Int,
        strokeWidth: Float
    ): GradientDrawable {
        val color = if (backgroundColor == 0) Color.TRANSPARENT else fetchAttrColor(
            context,
            backgroundColor
        )
        val colors = intArrayOf(color, color)
        return createSquareDrawable(
            context, colors,
            if (strokeColor == 0) Color.TRANSPARENT else fetchAttrColor(context, strokeColor),
            cornerRadius, strokeWidth
        )
    }

    /**
     * Tints a drawable with {@param colorAttrRes} and returns it.
     * @param context [Context]
     * @param drawableRes drawableRes to be tinted
     * @param colorAttrRes attr res id for the tint
     * @return a tinted drawable
     */
    fun tintDrawable(
        context: Context,
        @DrawableRes drawableRes: Int,
        @AttrRes colorAttrRes: Int
    ): Drawable {
        val drawable = AppCompatResources.getDrawable(context, drawableRes)!!.mutate()
        val wrap = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(wrap, fetchAttrColor(context, colorAttrRes))
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN)

        return wrap
    }

    /**
     * @return A ImageSpan with the given size multiplier. Supports vector drawables
     */
    fun getIconSpan(context: Context, size: Float): ImageSpan {
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_history_off)
        drawable!!.setBounds(
            0,
            0,
            (drawable.intrinsicWidth * size).toInt(),
            (drawable.intrinsicHeight * size).toInt()
        )

        return ImageSpan(drawable)
    }

    fun spToPx(sp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        ).toInt()
    }
}
