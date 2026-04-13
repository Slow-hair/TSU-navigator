package com.example.tsu_navigator

enum class PlaceType {
    CAFE,
    CANTEEN,
    SHOP,
}

data class EatPlace(
    val id: String,
    val name: String,
    val type: PlaceType,
    val latitude: Double,
    val longitude: Double,
    val cuisine: String? = null,
    val priceLevel: Int = 2,
    val workingHours: String = "09:00-20:00",
)

object EatPlacesData {
    val places = listOf(
        EatPlace("1", "Буфет в двойке", PlaceType.CANTEEN, 56.468623, 84.945130, "Купить перекус", 1, "09:00-18:00"),
        EatPlace("2", "XO bakery", PlaceType.CAFE, 56.468623, 84.945130, "Кофеек", 2, "08:00-19:00"),
        EatPlace("3", "Сибирские блины", PlaceType.CAFE, 56.469309, 84.946670, "Блинчики", 1, "09:00-20:00"),
        EatPlace("4", "Ярче", PlaceType.SHOP, 56.473924, 84.944715, "Продукты, готовая еда", 1, "08:00-23:00"),
        EatPlace("5", "Starbooks", PlaceType.CAFE, 56.469609, 84.946133, "Кофе, выпечка", 2, "07:30-20:00"),
        EatPlace("6", "100ловая в ГК", PlaceType.CANTEEN, 56.469195, 84.946659, "Покушать полноценно", 1, "09:00-17:00"),
        EatPlace("7", "Минутка в ГК", PlaceType.CAFE, 56.469195, 84.946659, "Покушать полноценно", 2, "00:00-23:59"),
        EatPlace("8", "Сыр-Бор", PlaceType.CAFE, 56.470787, 84.946166, "Покушать полноценно", 2, "10:00-22:00"),
        EatPlace("9", "Rostic's", PlaceType.CAFE, 56.469132, 84.951294, "Бургеры", 2, "11:00-23:00"),
        EatPlace("10", "Белка", PlaceType.CAFE, 56.471144, 84.950208, "Кофеек", 1, "10:00-20:00")
    )
}