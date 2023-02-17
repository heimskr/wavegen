// Credit: https://github.com/schoeberl/chisel-examples/blob/master/src/main/scala/util/Debounce.scala

package wavegen

import chisel3._
import chisel3.util._

class Debouncer(buttonCount: Int = 1, fac: Int = 100000000/100) extends Module {
	val io = IO(new Bundle {
		val in  = Input(Vec(buttonCount, Bool()))
		val out = Output(Vec(buttonCount, Bool()))
	})

	def sync(v: Bool) = RegNext(RegNext(v))

	def rising(v: Bool) = v & !RegNext(v)

	def tickGen(fac: Int) = {
		val reg = RegInit(0.U(log2Up(fac).W))
		val tick = reg === (fac - 1).U
		reg := Mux(tick, 0.U, reg + 1.U)
		tick
	}

	def filter(v: Bool, t: Bool) = {
		val reg = RegInit(0.U(3.W))
		when (t) {
			reg := Cat(reg(1, 0), v)
		}
		(reg(2) & reg(1)) | (reg(2) & reg(0)) | (reg(1) & reg(0))
	}

	io.in.zipWithIndex.foreach { case (btn, i) =>
		val btnSync = sync(btn)
		val tick = tickGen(fac)
		val btnDeb = Reg(Bool())

		when (tick) {
			btnDeb := btnSync
		}

		io.out(i) := rising(filter(btnDeb, tick))
	}
}
