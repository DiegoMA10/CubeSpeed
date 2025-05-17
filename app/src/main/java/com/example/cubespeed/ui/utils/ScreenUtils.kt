package com.example.cubespeed.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * Utility class for screen-related operations.
 */
object ScreenUtils {
    /**
     * Minimum width in dp to consider a device as a tablet.
     */
    private const val TABLET_MIN_WIDTH_DP = 800

    /**
     * Returns true if the device is a tablet, false otherwise.
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= TABLET_MIN_WIDTH_DP
    }

    /**
     * Returns true if the device is in landscape orientation, false otherwise.
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }

    /**
     * Returns the screen width in dp.
     */
    @Composable
    fun screenWidthDp(): Int {
        return LocalConfiguration.current.screenWidthDp
    }

    /**
     * Returns the screen height in dp.
     */
    @Composable
    fun screenHeightDp(): Int {
        return LocalConfiguration.current.screenHeightDp
    }

    /**
     * Returns a size that is appropriate for the current device type.
     *
     * @param tabletSize The size to use on tablets
     * @param phoneSize The size to use on phones
     * @return The appropriate size for the current device
     */
    @Composable
    fun getResponsiveSize(tabletSize: Dp, phoneSize: Dp): Dp {
        return if (isTablet()) tabletSize else phoneSize
    }

    /**
     * Returns a size that is appropriate for the current device type and orientation.
     *
     * @param tabletLandscapeSize The size to use on tablets in landscape orientation
     * @param tabletPortraitSize The size to use on tablets in portrait orientation
     * @param phoneLandscapeSize The size to use on phones in landscape orientation
     * @param phonePortraitSize The size to use on phones in portrait orientation
     * @return The appropriate size for the current device and orientation
     */
    @Composable
    fun getResponsiveSize(
        tabletLandscapeSize: Dp,
        tabletPortraitSize: Dp,
        phoneLandscapeSize: Dp,
        phonePortraitSize: Dp
    ): Dp {
        val isTablet = isTablet()
        val isLandscape = isLandscape()

        return when {
            isTablet && isLandscape -> tabletLandscapeSize
            isTablet && !isLandscape -> tabletPortraitSize
            !isTablet && isLandscape -> phoneLandscapeSize
            else -> phonePortraitSize
        }
    }

    /**
     * Returns a text size that is appropriate for the current device type.
     *
     * @param tabletSize The text size to use on tablets
     * @param phoneSize The text size to use on phones
     * @return The appropriate text size for the current device
     */
    @Composable
    fun getResponsiveTextSize(tabletSize: TextUnit, phoneSize: TextUnit): TextUnit {
        return if (isTablet()) tabletSize else phoneSize
    }

    /**
     * Returns a text size that is appropriate for the current device type and orientation.
     *
     * @param tabletLandscapeSize The text size to use on tablets in landscape orientation
     * @param tabletPortraitSize The text size to use on tablets in portrait orientation
     * @param phoneLandscapeSize The text size to use on phones in landscape orientation
     * @param phonePortraitSize The text size to use on phones in portrait orientation
     * @return The appropriate text size for the current device and orientation
     */
    @Composable
    fun getResponsiveTextSize(
        tabletLandscapeSize: TextUnit,
        tabletPortraitSize: TextUnit,
        phoneLandscapeSize: TextUnit,
        phonePortraitSize: TextUnit
    ): TextUnit {
        val isTablet = isTablet()
        val isLandscape = isLandscape()

        return when {
            isTablet && isLandscape -> tabletLandscapeSize
            isTablet && !isLandscape -> tabletPortraitSize
            !isTablet && isLandscape -> phoneLandscapeSize
            else -> phonePortraitSize
        }
    }
}
