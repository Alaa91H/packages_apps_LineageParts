/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.health;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import lineageos.health.HealthInterface;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

public class RechargeLevelPreference extends SliderPreference
        implements Slider.OnSliderTouchListener {
    private static final int MIN_RECHARGE_LEVEL = 20;
    private static final int MIN_RECHARGE_GAP = 1;
    private static final int MIN_CHARGING_LIMIT = 70;
    private static final int MAX_CHARGING_LIMIT = 100;
    private static final int FALLBACK_CHARGING_LIMIT = 100;

    private Slider mSlider;
    private TextView mRechargeLevelValue;
    private int mChargingLimit = -1;

    private final HealthInterface mHealthInterface;

    public RechargeLevelPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mHealthInterface = HealthInterface.getInstance(context);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mRechargeLevelValue = (TextView) holder.findViewById(android.R.id.summary);
        mRechargeLevelValue.setVisibility(View.VISIBLE);

        final int chargingLimit = getChargingLimit();
        final int maxRechargeLevel = getMaxRechargeLevel(chargingLimit);
        final int rechargeLevel = getSetting();

        mSlider = (Slider) holder.findViewById(R.id.slider);
        mSlider.removeOnSliderTouchListener(this);
        mSlider.addOnSliderTouchListener(this);
        mSlider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);
        mSlider.setStepSize(1);
        mSlider.setTickVisible(false);
        mSlider.setValueFrom(MIN_RECHARGE_LEVEL);
        mSlider.setValueTo(maxRechargeLevel);
        mSlider.setValue(rechargeLevel);

        updateValue(rechargeLevel);
    }

    @Override
    public void onStartTrackingTouch(final Slider slider) {
    }

    @Override
    public void onStopTrackingTouch(final Slider slider) {
        final int newRechargeLevel = (int) slider.getValue();
        if (!callChangeListener(newRechargeLevel) || !setSetting(newRechargeLevel)) {
            setValue(getSetting());
            return;
        }
        updateValue(newRechargeLevel);
    }

    public void setChargingLimit(final int chargingLimit) {
        final int previousChargingLimit = mChargingLimit;
        mChargingLimit = sanitizeChargingLimit(chargingLimit);

        final int defaultLevel = getMaxRechargeLevel(mChargingLimit);
        final int storedLevel = LineageSettings.System.getInt(
                getContext().getContentResolver(),
                LineageSettings.System.CHARGING_CONTROL_RECHARGE_LEVEL,
                defaultLevel);
        final int clampedLevel = clamp(storedLevel, mChargingLimit);
        final boolean corrected = storedLevel != clampedLevel;

        if (corrected) {
            setSetting(clampedLevel);
        }
        if (previousChargingLimit != mChargingLimit || corrected) {
            notifyChanged();
        }
    }

    public void setValue(final int value) {
        final int chargingLimit = getChargingLimit();
        final int maxRechargeLevel = getMaxRechargeLevel(chargingLimit);
        final int clampedValue = clamp(value, chargingLimit);
        if (mSlider != null) {
            if (maxRechargeLevel < mSlider.getValueTo()) {
                mSlider.setValue(clampedValue);
                mSlider.setValueTo(maxRechargeLevel);
            } else {
                mSlider.setValueTo(maxRechargeLevel);
                mSlider.setValue(clampedValue);
            }
        }
        updateValue(clampedValue);
    }

    protected int getSetting() {
        final int chargingLimit = getChargingLimit();
        final int defaultLevel = getMaxRechargeLevel(chargingLimit);
        final int value = LineageSettings.System.getInt(getContext().getContentResolver(),
                LineageSettings.System.CHARGING_CONTROL_RECHARGE_LEVEL, defaultLevel);
        return clamp(value, chargingLimit);
    }

    private boolean setSetting(final int rechargeLevel) {
        return LineageSettings.System.putInt(getContext().getContentResolver(),
                LineageSettings.System.CHARGING_CONTROL_RECHARGE_LEVEL,
                clamp(rechargeLevel, getChargingLimit()));
    }

    private int getChargingLimit() {
        return mChargingLimit > 0
                ? mChargingLimit
                : sanitizeChargingLimit(mHealthInterface.getLimit());
    }

    private int sanitizeChargingLimit(final int value) {
        return value >= MIN_CHARGING_LIMIT && value <= MAX_CHARGING_LIMIT
                ? value : FALLBACK_CHARGING_LIMIT;
    }

    private int getMaxRechargeLevel(final int chargingLimit) {
        return Math.max(MIN_RECHARGE_LEVEL, chargingLimit - MIN_RECHARGE_GAP);
    }

    private int clamp(final int value, final int chargingLimit) {
        return Math.max(MIN_RECHARGE_LEVEL,
                Math.min(value, getMaxRechargeLevel(chargingLimit)));
    }

    private void updateValue(final int value) {
        if (mRechargeLevelValue != null) {
            mRechargeLevelValue.setText(getContext().getString(
                    R.string.charging_control_recharge_level_summary, value));
        }
    }
}
