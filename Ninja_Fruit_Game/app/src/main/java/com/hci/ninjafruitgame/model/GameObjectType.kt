package com.hci.ninjafruitgame.model

enum class GameObjectType(val value: Int) {
    TYPE_FRUIT(1),
    TYPE_BOMB(-1),
    TYPE_FREEZE(2),
    TYPE_EXPLODE(3),
    TYPE_GUARD(4)
}