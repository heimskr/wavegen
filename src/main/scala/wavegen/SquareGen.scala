package wavegen

import chisel3._
import chisel3.util._

class SquareGen(clockFreq: Int, width: Int, waveformWidth: Int = 2) extends Module {
	require(1 < clockFreq)
	val clockWidth = log2Ceil(clockFreq + 1)

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val freq  = Input(UInt(clockWidth.W))
		val max   = Input(UInt(width.W))
		val wave  = Input(UInt(waveformWidth.W))
		val out   = Output(UInt(width.W))
	})

	val on = RegInit(false.B)

	val clocker = Module(new Clocker(clockFreq))
	clocker.io.enable := !io.pause
	clocker.io.freq := io.freq << 1.U

	val (waveCounter, waveWrap) = Counter(0 until waveformWidth, clocker.io.tick && !io.pause)

	// when (clocker.io.tick) {
	// 	on := !on
	// }

	when (io.wave(waveCounter)) {
		io.out := io.max
	} .otherwise {
		io.out := 0.U
	}
}
