# Room Database rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# Keep Room DAOs and Entities
-keep class com.pokerlanka.pirith.data.local.** { *; }
-keep class com.pokerlanka.pirith.domain.model.** { *; }
