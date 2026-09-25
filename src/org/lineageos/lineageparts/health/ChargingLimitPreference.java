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

import org.lineageos.lineageparts.R;

public class ChargingLimitPreference extends SliderPreference
        implements Slider.OnSliderTouchListener {
    private static final String TAG = ChargingLimitPreference.class.getSimpleName();
    private static final int MIN_CHARGING_LIMIT = 70;
    private static final int MAX_CHARGING_LIMIT = 100;
    private static final int FALLBACK_CHARGING_LIMIT = 100;

    private Slider mSlider;

    private TextView mChargingLimitValue;

    private final HealthInterface mHealthInterface;

    public ChargingLimitPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        mHealthInterface = HealthInterface.getInstance(context);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mChargingLimitValue = (TextView) holder.findViewById(android.R.id.summary);
        mChargingLimitValue.setVisibility(View.VISIBLE);

        mSlider = (Slider) holder.findViewById(R.id.slider);
        mSlider.removeOnSliderTouchListener(this);
        mSlider.addOnSliderTouchListener(this);
        mSlider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);
        mSlider.setStepSize(1);
        mSlider.setTickVisible(false);
        mSlider.setValueFrom(MIN_CHARGING_LIMIT);
        mSlider.setValueTo(MAX_CHARGING_LIMIT);

        int currLimit = getSetting();
        mSlider.setValue(currLimit);
        updateValue(currLimit);
    }

    @Override
    public void onStartTrackingTouch(final Slider slider) {
    }

    @Override
    public void onStopTrackingTouch(final Slider slider) {
        final int newLimit = (int) slider.getValue();

        if (!callChangeListener(newLimit)) {
            setValue(getSetting());
            return;
        }

        setSetting(newLimit);
        updateValue(newLimit);
    }

    public void setValue(final int value) {
        final int safeValue = sanitizeChargingLimit(value);
        if (mSlider != null) {
            mSlider.setValue(safeValue);
        }
        updateValue(safeValue);
    }

    protected int getSetting() {
        return sanitizeChargingLimit(mHealthInterface.getLimit());
    }

    private int sanitizeChargingLimit(final int value) {
        return value >= MIN_CHARGING_LIMIT && value <= MAX_CHARGING_LIMIT
                ? value : FALLBACK_CHARGING_LIMIT;
    }

    protected boolean setSetting(final int chargingLimit) {
        return mHealthInterface.setLimit(chargingLimit);
    }

    private void updateValue(final int value) {
        if (value > 0 && mChargingLimitValue != null) {
            mChargingLimitValue.setText(
                    getContext().getString(R.string.charging_control_limit_summary, value));
        }
    }
}
