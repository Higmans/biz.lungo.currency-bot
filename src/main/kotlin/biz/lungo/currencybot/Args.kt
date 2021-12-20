package biz.lungo.currencybot

typealias Args = Array<String>

fun Args.get(name: String): String? {
    forEachIndexed { index, s ->
        if (name.equals(s, true)) {
            return if (this.size > index) {
                this[index + 1]
            } else {
                null
            }
        }
    }
    return null
}