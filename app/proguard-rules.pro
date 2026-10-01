# Sweep — ProGuard rules
# Minification is disabled for the default build; these rules are here for when
# you enable it. Keep the Play Billing and Ads SDK entry points.

-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keepattributes *Annotation*
