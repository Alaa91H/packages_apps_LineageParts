#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "src/org/lineageos/lineageparts/health/ChargingControlSettings.java"
RECHARGE = ROOT / "src/org/lineageos/lineageparts/health/RechargeLevelPreference.java"
LIMIT = ROOT / "src/org/lineageos/lineageparts/health/ChargingLimitPreference.java"
XML = ROOT / "res/xml/charging_control_settings.xml"

def read(path: Path) -> str:
    if not path.is_file():
        raise SystemExit(f"missing required file: {path}")
    return path.read_text(encoding="utf-8")

def require(text: str, needle: str, where: str) -> None:
    if needle not in text:
        raise SystemExit(f"{where}: missing invariant: {needle}")

settings = read(SETTINGS)
recharge = read(RECHARGE)
limit = read(LIMIT)
xml = read(XML)

keys = (
    "charging_control_recharge_level",
    "charging_control_limit_schedule_enabled",
    "charging_control_limit_start_time",
    "charging_control_limit_end_time",
)
for key in keys:
    require(xml, f'android:key="{key}"', "charging_control_settings.xml")

require(recharge, "private static final int MIN_RECHARGE_LEVEL = 20;", "RechargeLevelPreference")
require(recharge, "private static final int MIN_RECHARGE_GAP = 1;", "RechargeLevelPreference")
require(recharge, "private static final int MIN_CHARGING_LIMIT = 70;", "RechargeLevelPreference")
require(recharge, "private static final int MAX_CHARGING_LIMIT = 100;", "RechargeLevelPreference")
require(recharge, "sanitizeChargingLimit(mHealthInterface.getLimit())",
        "RechargeLevelPreference invalid limit guard")
require(recharge, "mChargingLimit = sanitizeChargingLimit(chargingLimit);",
        "RechargeLevelPreference external limit guard")
require(recharge, "mSlider.setStepSize(1);", "RechargeLevelPreference")
require(recharge, "mSlider.removeOnSliderTouchListener(this);", "RechargeLevelPreference lifecycle")
require(recharge, "!callChangeListener(newRechargeLevel) || !setSetting(newRechargeLevel)",
        "RechargeLevelPreference backend failure rollback")
require(recharge, "storedLevel != clampedLevel", "RechargeLevelPreference redundant-write guard")
require(recharge, "Math.min(value, getMaxRechargeLevel(chargingLimit))", "RechargeLevelPreference clamp")

for setting_key in (
    "CHARGING_CONTROL_RECHARGE_LEVEL",
    "CHARGING_CONTROL_LIMIT_SCHEDULE_ENABLED",
    "CHARGING_CONTROL_LIMIT_START_TIME",
    "CHARGING_CONTROL_LIMIT_END_TIME",
):
    require(settings, f"LineageSettings.System.{setting_key}", "ChargingControlSettings")

require(settings, "setVisible(scheduleEnabled);", "ChargingControlSettings schedule visibility")
require(settings, "result.add(CHARGING_CONTROL_RECHARGE_LEVEL_PREF);", "ChargingControl search index")
require(settings, "result.add(CHARGING_CONTROL_LIMIT_SCHEDULE_ENABLED_PREF);", "ChargingControl search index")
require(limit, "private static final int MIN_CHARGING_LIMIT = 70;",
        "ChargingLimitPreference range")
require(limit, "private static final int MAX_CHARGING_LIMIT = 100;",
        "ChargingLimitPreference range")
require(limit, "private static final int FALLBACK_CHARGING_LIMIT = 100;",
        "ChargingLimitPreference fallback")
require(limit, "sanitizeChargingLimit(mHealthInterface.getLimit())",
        "ChargingLimitPreference invalid limit guard")
require(limit, "final int safeValue = sanitizeChargingLimit(value);",
        "ChargingLimitPreference external refresh guard")
require(limit, "mSlider.removeOnSliderTouchListener(this);",
        "ChargingLimitPreference lifecycle")
require(limit, "if (!callChangeListener(newLimit))", "ChargingLimitPreference listener contract")
require(limit, "if (!setSetting(newLimit))", "ChargingLimitPreference backend failure rollback")
require(limit, "protected boolean setSetting", "ChargingLimitPreference backend result")
require(settings, "return mHealthInterface.setEnabled", "ChargingControl enabled result")
require(settings, "if (!mHealthInterface.setMode(chargingControlMode))",
        "ChargingControl mode result")

for uri_name in (
    "CHARGING_CONTROL_ENABLED_URI",
    "CHARGING_CONTROL_MODE_URI",
    "CHARGING_CONTROL_START_TIME_URI",
    "CHARGING_CONTROL_TARGET_TIME_URI",
    "CHARGING_CONTROL_LIMIT_URI",
    "CHARGING_CONTROL_RECHARGE_LEVEL_URI",
    "CHARGING_CONTROL_LIMIT_SCHEDULE_ENABLED_URI",
    "CHARGING_CONTROL_LIMIT_START_TIME_URI",
    "CHARGING_CONTROL_LIMIT_END_TIME_URI",
):
    require(settings, uri_name, "ChargingControlSettings external settings sync")

require(settings, "watch(CHARGING_CONTROL_ENABLED_URI,",
        "ChargingControlSettings external settings sync")
require(settings, "public void onSettingsChanged(final Uri contentUri)",
        "ChargingControlSettings external settings sync")
require(settings, "contentUri == null || !isAdded() || mHealthInterface == null",
        "ChargingControlSettings initial observer callback guard")
require(settings, "CHARGING_CONTROL_MODE_URI.equals(contentUri)",
        "ChargingControlSettings mode sync")
require(settings, "CHARGING_CONTROL_LIMIT_URI.equals(contentUri)",
        "ChargingControlSettings limit sync")
require(settings, "CHARGING_CONTROL_LIMIT_SCHEDULE_ENABLED_URI.equals(contentUri)",
        "ChargingControlSettings schedule sync")
require(settings, "mChargingControlRechargeLevelPref.setChargingLimit(limit);",
        "ChargingControlSettings linked recharge sync")
require(settings, "super.onSettingsChanged(contentUri);",
        "ChargingControlSettings PartsUpdater forwarding")

pref_start = settings.find("public boolean onPreferenceChange")
pref_end = settings.find("private void resetToDefaults()", pref_start)
if pref_start < 0 or pref_end < 0:
    raise SystemExit("Unable to isolate ChargingControlSettings.onPreferenceChange")
pref_change = settings[pref_start:pref_end]
if "mChargingControlRechargeLevelPref.setChargingLimit" in pref_change:
    raise SystemExit(
        "ChargingControlSettings must not optimistically update Recharge before limit commit"
    )

require(settings, "CHARGING_CONTROL_LIMIT_URI.equals(contentUri)",
        "ChargingControlSettings committed limit observer")
require(settings, "mChargingControlRechargeLevelPref.setChargingLimit(limit);",
        "ChargingControlSettings post-commit Recharge sync")

print("LineageParts Charging Control UI validation passed")
