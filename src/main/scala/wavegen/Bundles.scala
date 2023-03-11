package wavegen

import chisel3._
import chisel3.util._

class NESButtons extends Bundle {
	val a      = Bool()
	val b      = Bool()
	val select = Bool()
	val start  = Bool()
	val up     = Bool()
	val down   = Bool()
	val left   = Bool()
	val right  = Bool()
}

object NESButtons {
	def apply() = new NESButtons
}
