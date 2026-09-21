/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.health;

import android.content.Context;
import android.util.AttributeSet;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

public class LimitEndTimePreference extends TimePreference {
    public LimitEndTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getSummaryResourceId() {
        return R.string.charging_control_limit_end_time_summary;
    }

    @Override
    protected int getTimeSetting() {
        return LineageSettings.System.getInt(getContext().getContentResolver(),
                LineageSettings.System.CHARGING_CONTROL_LIMIT_END_TIME,
                mHealthInterface.getTargetTime());
    }

    @Override
    protected void setTimeSetting(int secondOfDay) {
        LineageSettings.System.putInt(getContext().getContentResolver(),
                LineageSettings.System.CHARGING_CONTROL_LIMIT_END_TIME, secondOfDay);
    }
}
