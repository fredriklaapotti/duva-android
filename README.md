# duva-android
Featherlight safety messaging platform - Android client\
The idea was to build an "always on" app that used geofencing for broadcasts of local emergency messages. Example uses would be schools and venues, where you'd need to reach everyone but you can't reach or don't have another broadcast system (like speakers).

The geofencing and messaging was relatively easy to solve but due to how Android changed battery uesage of apps the app was forced to use foreground mode. I believed that was a dealbreaker since it'd require staff and visitors to actively start the app when in the vicinity.

However, the basic premise of geofencing + messaging could be used for other ideas!

Stack:
- Android Studio
- Kotlin
- Firebase
