package dev.lucasnlm.antimine.preferences

import android.view.ViewConfiguration
import dev.lucasnlm.antimine.preferences.models.ControlStyle
import dev.lucasnlm.antimine.preferences.models.Minefield

class PreferencesRepository(
    private val preferencesManager: IPreferencesManager,
    private val defaultLongPressTimeout: Int,
) : IPreferencesRepository {
    init {
        migrateOldPreferences()
    }

    private fun longPressTimeout() = ViewConfiguration.getLongPressTimeout()

    override fun hasCustomizations(): Boolean {
        return preferencesManager.run {
            getInt(PreferenceKeys.PREFERENCE_AREA_SIZE, 50) != 50 ||
                getInt(PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT, longPressTimeout()) != longPressTimeout() ||
                getInt(PreferenceKeys.PREFERENCE_SQUARE_RADIUS, 3) != 3
        }
    }

    override fun resetControls() {
        preferencesManager.apply {
            removeKey(PreferenceKeys.PREFERENCE_TOUCH_SENSIBILITY)
            removeKey(PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT)
        }
    }

    override fun reset() {
        preferencesManager.apply {
            removeKey(PreferenceKeys.PREFERENCE_ASSISTANT)
            removeKey(PreferenceKeys.PREFERENCE_VIBRATION)
            removeKey(PreferenceKeys.PREFERENCE_ANIMATION)
            removeKey(PreferenceKeys.PREFERENCE_QUESTION_MARK)
            removeKey(PreferenceKeys.PREFERENCE_SOUND_EFFECTS)
            removeKey(PreferenceKeys.PREFERENCE_SQUARE_RADIUS)
            removeKey(PreferenceKeys.PREFERENCE_AREA_SIZE)
        }
    }

    override fun customGameMode(): Minefield = with(preferencesManager) {
        Minefield(
            getInt(PreferenceKeys.PREFERENCE_CUSTOM_GAME_WIDTH, 9),
            getInt(PreferenceKeys.PREFERENCE_CUSTOM_GAME_HEIGHT, 9),
            getInt(PreferenceKeys.PREFERENCE_CUSTOM_GAME_MINES, 9)
        )
    }

    override fun updateCustomGameMode(minefield: Minefield) {
        preferencesManager.apply {
            putInt(PreferenceKeys.PREFERENCE_CUSTOM_GAME_WIDTH, minefield.width)
            putInt(PreferenceKeys.PREFERENCE_CUSTOM_GAME_HEIGHT, minefield.height)
            putInt(PreferenceKeys.PREFERENCE_CUSTOM_GAME_MINES, minefield.mines)
        }
    }

    override fun useFlagAssistant(): Boolean =
        preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_ASSISTANT, true)

    override fun setFlagAssistant(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_ASSISTANT, value)
    }

    override fun useHapticFeedback(): Boolean =
        preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_VIBRATION, true)

    override fun setHapticFeedback(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_VIBRATION, value)
    }

    override fun getHapticFeedbackLevel(): Int {
        return preferencesManager.getInt(PreferenceKeys.PREFERENCE_VIBRATION_LEVEL, 100)
    }

    override fun setHapticFeedbackLevel(value: Int) {
        val newValue = value.coerceIn(0, 200)
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_VIBRATION_LEVEL, newValue)
    }

    override fun resetHapticFeedbackLevel() {
        preferencesManager.removeKey(PreferenceKeys.PREFERENCE_VIBRATION_LEVEL)
    }

    override fun squareSize(): Int =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_AREA_SIZE, 50)

    override fun setSquareSize(value: Int?) {
        if (value == null) {
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_AREA_SIZE)
        } else {
            preferencesManager.putInt(PreferenceKeys.PREFERENCE_AREA_SIZE, value)
        }
    }

    override fun useAnimations(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_ANIMATION, true)
    }

    override fun setAnimations(enabled: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_ANIMATION, enabled)
    }

    override fun useQuestionMark(): Boolean =
        preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_QUESTION_MARK, false)

    override fun setQuestionMark(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_QUESTION_MARK, value)
    }

    override fun isSoundEffectsEnabled(): Boolean =
        preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_SOUND_EFFECTS, false)

    override fun setSoundEffectsEnabled(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_SOUND_EFFECTS, value)
    }

    override fun touchSensibility(): Int =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_TOUCH_SENSIBILITY, 5)

    override fun setTouchSensibility(sensibility: Int) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_TOUCH_SENSIBILITY, sensibility)
    }

    override fun setPreferredLocale(locale: String) {
        if (locale.isBlank()) {
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_LOCALE)
        } else {
            preferencesManager.putString(PreferenceKeys.PREFERENCE_LOCALE, locale)
        }
    }

    override fun getPreferredLocale(): String? {
        return preferencesManager.getString(PreferenceKeys.PREFERENCE_LOCALE)
    }

    override fun showWindowsWhenFinishGame(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_SHOW_WINDOWS, true)
    }

    override fun mustShowWindowsWhenFinishGame(enabled: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_SHOW_WINDOWS, enabled)
    }

    override fun userId(): String? {
        return preferencesManager.getString(PreferenceKeys.PREFERENCE_USER_ID)
    }

    override fun setUserId(userId: String) {
        if (userId.isBlank()) {
            preferencesManager.removeKey(userId)
        } else {
            preferencesManager.putString(PreferenceKeys.PREFERENCE_USER_ID, userId)
        }
    }

    override fun addUnlockedTheme(id: Int) {
        val themes = preferencesManager.getString(PreferenceKeys.PREFERENCE_UNLOCKED_THEMES) ?: ""
        val themesIt = themes.split(" ").mapNotNull { it.toIntOrNull() }
        if (!themesIt.contains(id)) {
            val newState = themesIt.toMutableList().run {
                add(id)
                joinToString(" ")
            }
            preferencesManager.putString(PreferenceKeys.PREFERENCE_UNLOCKED_THEMES, newState)
        }
    }

    override fun setUnlockedThemes(themes: String) {
        preferencesManager.putString(PreferenceKeys.PREFERENCE_UNLOCKED_THEMES, themes)
    }

    override fun getUnlockedThemes(): List<Int> {
        val themes = preferencesManager.getString(PreferenceKeys.PREFERENCE_UNLOCKED_THEMES) ?: ""
        return themes.split(" ").mapNotNull { it.toIntOrNull() }
    }

    override fun controlStyle(): ControlStyle {
        val index = preferencesManager.getInt(PreferenceKeys.PREFERENCE_CONTROL_STYLE, -1)
        return ControlStyle.values().getOrNull(index) ?: ControlStyle.Standard
    }

    override fun useControlStyle(controlStyle: ControlStyle) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_CONTROL_STYLE, controlStyle.ordinal)
    }

    override fun isFirstUse(): Boolean =
        preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_FIRST_USE, true)

    override fun completeFirstUse() {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_FIRST_USE, false)
    }

    override fun isTutorialCompleted(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_TUTORIAL_COMPLETED, false)
    }

    override fun setCompleteTutorial(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_TUTORIAL_COMPLETED, value)
    }

    override fun customLongPressTimeout(): Long =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT, longPressTimeout()).toLong()

    override fun setCustomLongPressTimeout(value: Long) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT, value.toInt())
    }

    override fun getDoubleClickTimeout(): Long {
        return preferencesManager.getInt(PreferenceKeys.PREFERENCE_DOUBLE_CLICK_TIMEOUT, 250).toLong()
    }

    override fun setDoubleClickTimeout(value: Long) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_DOUBLE_CLICK_TIMEOUT, value.toInt())
    }

    override fun themeId(): Long =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_CUSTOM_THEME, 0).toLong()

    override fun useTheme(themeId: Long) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_CUSTOM_THEME, themeId.toInt())
    }

    override fun updateStatsBase(statsBase: Int) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_STATS_BASE, statsBase)
    }

    override fun getStatsBase(): Int =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_STATS_BASE, 0)

    override fun getUseCount(): Int =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_USE_COUNT, 0)

    override fun incrementUseCount() {
        val current = preferencesManager.getInt(PreferenceKeys.PREFERENCE_USE_COUNT, 0)
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_USE_COUNT, current + 1)
    }

    override fun incrementProgressiveValue() {
        val value = preferencesManager.getInt(PreferenceKeys.PREFERENCE_PROGRESSIVE_VALUE, 0)
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_PROGRESSIVE_VALUE, value + 1)
    }

    override fun decrementProgressiveValue() {
        val value = preferencesManager.getInt(PreferenceKeys.PREFERENCE_PROGRESSIVE_VALUE, 0)
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_PROGRESSIVE_VALUE, (value - 1).coerceAtLeast(0))
    }

    override fun getProgressiveValue(): Int =
        preferencesManager.getInt(PreferenceKeys.PREFERENCE_PROGRESSIVE_VALUE, 0)

    override fun isRequestRatingEnabled(): Boolean =
        preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_REQUEST_RATING, true)

    override fun disableRequestRating() {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_REQUEST_RATING, false)
    }

    private fun migrateOldPreferences() {
        // Migrate Double Click to the new Control settings
        if (preferencesManager.contains(PreferenceKeys.PREFERENCE_OLD_DOUBLE_CLICK)) {
            if (preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_OLD_DOUBLE_CLICK, false)) {
                useControlStyle(ControlStyle.DoubleClick)
            }

            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_OLD_DOUBLE_CLICK)
        }

        // Migrate Large Area to Custom Area size
        if (preferencesManager.contains(PreferenceKeys.PREFERENCE_OLD_LARGE_AREA)) {
            if (preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_OLD_LARGE_AREA, false)) {
                preferencesManager.putInt(PreferenceKeys.PREFERENCE_AREA_SIZE, 63)
            } else {
                preferencesManager.putInt(PreferenceKeys.PREFERENCE_AREA_SIZE, 50)
            }
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_OLD_LARGE_AREA)
        }

        if (!preferencesManager.contains(PreferenceKeys.PREFERENCE_AREA_SIZE)) {
            preferencesManager.putInt(PreferenceKeys.PREFERENCE_AREA_SIZE, 50)
        }

        if (!preferencesManager.contains(PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT)) {
            preferencesManager.putInt(PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT, defaultLongPressTimeout)
        }

        if (preferencesManager.contains(PreferenceKeys.PREFERENCE_FIRST_USE)) {
            preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_TUTORIAL_COMPLETED, true)
        }

        // Remove old sizes
        if (preferencesManager.contains(PreferenceKeys.PREFERENCE_OLD_AREA_SIZES)) {
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_AREA_SIZE)
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_SQUARE_RADIUS)
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_SQUARE_DIVIDER)
            preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_OLD_AREA_SIZES, true)
        }
    }

    override fun setPremiumFeatures(status: Boolean) {
        if (!preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_PREMIUM_FEATURES, false)) {
            preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_PREMIUM_FEATURES, status)
        }
    }

    override fun setShowSupport(show: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_SHOW_SUPPORT, show)
    }

    override fun isPremiumEnabled(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_PREMIUM_FEATURES, false)
    }

    override fun showSupport(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_SHOW_SUPPORT, true)
    }

    override fun useHelp(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_USE_HELP, true)
    }

    override fun setHelp(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_USE_HELP, value)
    }

    override fun lastHelpUsed(): Long {
        return preferencesManager.getLong(PreferenceKeys.PREFERENCE_LAST_HELP_USED, 0L)
    }

    override fun refreshLastHelpUsed() {
        preferencesManager.putLong(PreferenceKeys.PREFERENCE_LAST_HELP_USED, System.currentTimeMillis())
    }

    override fun useSimonTathamAlgorithm(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_SIMON_TATHAM_ALGORITHM, true)
    }

    override fun setSimonTathamAlgorithm(enabled: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_SIMON_TATHAM_ALGORITHM, enabled)
    }

    override fun squareRadius(): Int {
        return preferencesManager.getInt(PreferenceKeys.PREFERENCE_SQUARE_RADIUS, 3)
    }

    override fun setSquareRadius(value: Int?) {
        if (value == null) {
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_SQUARE_RADIUS)
        } else {
            preferencesManager.putInt(PreferenceKeys.PREFERENCE_SQUARE_RADIUS, value)
        }
    }

    override fun getTips(): Int {
        return preferencesManager.getInt(PreferenceKeys.PREFERENCE_TIPS, 5)
    }

    override fun setTips(tips: Int) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_TIPS, tips)
    }

    override fun getExtraTips(): Int {
        return preferencesManager.getInt(PreferenceKeys.PREFERENCE_EXTRA_TIPS, 0)
    }

    override fun setExtraTips(tips: Int) {
        preferencesManager.putInt(PreferenceKeys.PREFERENCE_EXTRA_TIPS, tips)
    }

    override fun openUsingSwitchControl(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_USE_OPEN_SWITCH_CONTROL, true)
    }

    override fun setSwitchControl(useOpen: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_USE_OPEN_SWITCH_CONTROL, useOpen)
    }

    override fun openGameDirectly(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_OPEN_DIRECTLY, false)
    }

    override fun setOpenGameDirectly(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_OPEN_DIRECTLY, value)
    }

    override fun squareDivider(): Int {
        return preferencesManager.getInt(PreferenceKeys.PREFERENCE_SQUARE_DIVIDER, 0)
    }

    override fun setSquareDivider(value: Int?) {
        if (value == null) {
            preferencesManager.removeKey(PreferenceKeys.PREFERENCE_SQUARE_DIVIDER)
        } else {
            preferencesManager.putInt(PreferenceKeys.PREFERENCE_SQUARE_DIVIDER, value.coerceIn(0, 50))
        }
    }

    override fun showTutorialDialog(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_TUTORIAL_DIALOG, true)
    }

    override fun setTutorialDialog(show: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_TUTORIAL_DIALOG, show)
    }

    override fun allowTapOnNumbers(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_ALLOW_TAP_NUMBER, true)
    }

    override fun setAllowTapOnNumbers(allow: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_ALLOW_TAP_NUMBER, allow)
    }

    override fun setToggleButtonOnTopBar(enabled: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_SHOW_TOGGLE_ON_TOP_BAR, enabled)
    }

    override fun showToggleButtonOnTopBar(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_SHOW_TOGGLE_ON_TOP_BAR, false)
    }

    override fun showTutorialButton(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_SHOULD_SHOW_TUTORIAL_BUTTON, true)
    }

    override fun setShowTutorialButton(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_SHOULD_SHOW_TUTORIAL_BUTTON, value)
    }

    override fun dimNumbers(): Boolean {
        return preferencesManager.getBoolean(PreferenceKeys.PREFERENCE_DIM_NUMBERS, false)
    }

    override fun setDimNumbers(value: Boolean) {
        preferencesManager.putBoolean(PreferenceKeys.PREFERENCE_DIM_NUMBERS, value)
    }
}
