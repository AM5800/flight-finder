package flight_finder.skyscanner

import org.json.JSONArray
import org.json.JSONObject

fun JSONArray.forEach(callback: (Int) -> Unit) {
  for (i in 0..this.length() - 1) callback(i)
}

fun JSONArray.forEachObject(callback: (JSONObject) -> Unit) {
  for (i in 0..this.length() - 1) callback(getJSONObject(i))
}

fun <T> JSONArray.map(callback: (Int, JSONArray) -> T): Collection<T> {
  val result = mutableListOf<T>()
  this.forEach { i ->
    result.add(callback(i, this))
  }
  return result
}
