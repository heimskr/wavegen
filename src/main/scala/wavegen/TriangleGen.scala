package wavegen

import chisel3._
import chisel3.util._

class TriangleGen(resolution: Int, startHigh: Boolean = false)(implicit clockFreq: Int) extends Module {
	require(2 <= clockFreq)
	require(1 < resolution)
	val outWidth = log2Ceil(resolution + 1)
	val clockWidth = log2Ceil(clockFreq + 1)

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val freq  = Input(UInt(clockWidth.W))
		val out   = Output(UInt(outWidth.W))
	})

	val rising = RegInit((!startHigh).B)

	val clocker = Module(new Clocker)
	clocker.io.enable := !io.pause

	val freq = io.freq * ((resolution << 1) + 1).U
	val steps = Wire(UInt(clockWidth.W))

	when (freq <= clockFreq.U) {
		clocker.io.freq := freq
		steps := 1.U
	} .otherwise {
		clocker.io.freq := clockFreq.U
		steps := freq / clockFreq.U
	}

	val out = RegInit(0.U(outWidth.W))

	when (clocker.io.tick) {
		when (rising) {
			when (out === resolution.U) {
				rising := false.B
				out := out - steps
			} .elsewhen(resolution.U < out +& steps) {
				out := resolution.U
			} .otherwise {
				out := out + steps
			}
		} .otherwise {
			when (out === 0.U) {
				rising := true.B
				out := out + steps
			} .elsewhen (out < steps) {
				out := 0.U
			} .otherwise {
				out := out - steps
			}
		}
	}

	io.out := out
}
