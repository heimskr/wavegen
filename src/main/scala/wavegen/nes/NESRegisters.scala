package wavegen.nes

import chisel3._
import chisel3.util._

class NESRegisters extends Bundle {
	val $4000 = UInt(8.W)
	val $4001 = UInt(8.W)
	val $4002 = UInt(8.W)
	val $4003 = UInt(8.W)
	val $4004 = UInt(8.W)
	val $4005 = UInt(8.W)
	val $4006 = UInt(8.W)
	val $4007 = UInt(8.W)
	val $4008 = UInt(8.W)
	val $400A = UInt(8.W)
	val $400B = UInt(8.W)
	val $400C = UInt(8.W)
	val $400E = UInt(8.W)
	val $400F = UInt(8.W)
	val $4010 = UInt(8.W)
	val $4011 = UInt(8.W)
	val $4012 = UInt(8.W)
	val $4013 = UInt(8.W)
	val $4015 = UInt(8.W)
	val $4017 = UInt(8.W)
}

object NESRegisters {
	def apply() = new NESRegisters
}
