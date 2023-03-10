// Taken from Chisel3, licensed under the Apache 2.0 license.

package wavegen

import chisel3._
import chisel3.experimental.{requireIsChiselType, DataMirror, AutoCloneType}
import scala.collection.immutable.ListMap

// An example of how Record might be extended
// In this case, CustomBundle is a Record constructed from a Tuple of (String, Data)
//   it is a possible implementation of a programmatic "Bundle"
//   (and can by connected to MyBundle below)
final class CustomBundle(elts: Seq[(String, Data)]) extends Record with AutoCloneType {
	val elements = ListMap(elts.map {
		case (field, elt) =>
			requireIsChiselType(elt)
			field -> DataMirror.internal.chiselTypeClone(elt)
	}: _*)

	def apply(elt: String): Data = elements(elt)
}