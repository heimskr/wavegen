package wavegen

import chisel3._
import chisel3.util._

class SquareGen(width: Int, waveformWidth: Int = 2)(implicit clockFreq: Int) extends Module {
	require(2 <= clockFreq)
	require(1 <= waveformWidth)
	val clockWidth = log2Ceil(clockFreq + 1)

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val freq  = Input(UInt(clockWidth.W))
		val max   = Input(UInt(width.W))
		val wave  = Input(UInt(waveformWidth.W))
		val out   = Output(UInt(width.W))
	})

	val clocker = Module(new DynamicClocker)
	clocker.io.enable := !io.pause
	clocker.io.freq.bits := io.freq
	clocker.io.freq.valid := true.B

	val (waveCounter, waveWrap) = Counter(0 until waveformWidth, clocker.io.tick && !io.pause)

	when (io.wave(waveCounter)) {
		io.out := io.max
	} .otherwise {
		io.out := 0.U
	}
}
