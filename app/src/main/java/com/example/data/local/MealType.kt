package com.example.data.local

enum class AttendanceCategory(val displayName: String, val emoji: String) {
    MEAL("Meals", "🍽️"),
    YOGA("Yoga", "🧘"),
    MEDITATION("Meditation", "🕯️")
}

enum class MealType(
    val displayName: String,
    val emoji: String,
    val category: AttendanceCategory = AttendanceCategory.MEAL
) {
    BREAKFAST("Breakfast", "🍳", AttendanceCategory.MEAL),
    LUNCH("Lunch", "🍛", AttendanceCategory.MEAL),
    DINNER("Dinner", "🍽️", AttendanceCategory.MEAL),
    YOGA("Yoga", "🧘", AttendanceCategory.YOGA),
    MEDITATION("Meditation", "🕯️", AttendanceCategory.MEDITATION);

    val isMeal: Boolean get() = category == AttendanceCategory.MEAL

    companion object {
        fun fromString(value: String): MealType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BREAKFAST
        }

        fun meals(): List<MealType> = entries.filter { it.isMeal }
    }
}
