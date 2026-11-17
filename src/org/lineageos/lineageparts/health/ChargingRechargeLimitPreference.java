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

public class ChargingRechargeLimitPreference extends SliderPreference
        implements Slider.OnSliderTouchListener {
    private static final int MIN_RECHARGE_GAP_WARNING = 5;

    private Slider mSlider;
    private TextView mRechargeLimitValue;

    private final HealthInterface mHealthInterface;

    public ChargingRechargeLimitPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        mHealthInterface = HealthInterface.getInstance(context);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mRechargeLimitValue = (TextView) holder.findViewById(android.R.id.summary);
        mRechargeLimitValue.setVisibility(View.VISIBLE);

        mSlider = (Slider) holder.findViewById(R.id.slider);
        mSlider.addOnSliderTouchListener(this);
        mSlider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);
        mSlider.setStepSize(1);
        mSlider.setTickVisible(false);
        mSlider.setValueFrom(HealthInterface.MIN_RECHARGE_LIMIT);

        final int limit = mHealthInterface.getLimit();
        final int max = Math.max(HealthInterface.MIN_RECHARGE_LIMIT, limit - 1);
        if (mSlider.getValue() > max) {
            mSlider.setValue(max);
        }
        mSlider.setValueTo(max);

        final int value = Math.max(HealthInterface.MIN_RECHARGE_LIMIT,
                Math.min(getSetting(), max));
        mSlider.setValue(value);
        updateValue(value);
    }

    @Override
    public void onStartTrackingTouch(final Slider slider) {
    }

    @Override
    public void onStopTrackingTouch(final Slider slider) {
        final int newLimit = (int) slider.getValue();

        setSetting(newLimit);
        updateValue(newLimit);
    }

    public void setValue(final int value) {
        final int limit = mHealthInterface.getLimit();
        final int max = Math.max(HealthInterface.MIN_RECHARGE_LIMIT, limit - 1);
        final int clamped = Math.max(HealthInterface.MIN_RECHARGE_LIMIT,
                Math.min(value, max));

        if (mSlider != null) {
            if (mSlider.getValue() > max) {
                mSlider.setValue(max);
            }
            mSlider.setValueTo(max);
            mSlider.setValue(clamped);
        }
        updateValue(clamped);
    }

    public void setMaxValue(final int limit) {
        final int max = Math.max(HealthInterface.MIN_RECHARGE_LIMIT, limit - 1);

        if (mSlider != null) {
            if (mSlider.getValue() > max) {
                mSlider.setValue(max);
            }
            mSlider.setValueTo(max);
            final int value = Math.min((int) mSlider.getValue(), max);
            mSlider.setValue(value);
            updateValue(value);
        }
    }

    private int getSetting() {
        return mHealthInterface.getRechargeLimit();
    }

    private void setSetting(final int rechargeLimit) {
        mHealthInterface.setRechargeLimit(rechargeLimit);
    }

    private void updateValue(final int value) {
        if (mRechargeLimitValue == null) {
            return;
        }

        if (mHealthInterface.getLimit() - value < MIN_RECHARGE_GAP_WARNING) {
            mRechargeLimitValue.setText(
                    R.string.charging_control_recharge_limit_narrow_warning);
        } else {
            mRechargeLimitValue.setText(getContext().getString(
                    R.string.charging_control_recharge_limit_summary, value));
        }
    }
}
