package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class FrameSequencer(baseFreq: Int = GameBoy.cpuFreq) extends Module {
	val io = IO(new Bundle {
		val lengthCounter = Output(Bool())
		val envelope      = Output(Bool())
		val sweeper       = Output(Bool())
	})

	val clocker = Module(new StaticClocker(512, baseFreq))
	clocker.io.enable := true.B

	val (count, wrap) = Counter(0 to 7, clocker.io.tick)

	io.lengthCounter := !count(0)
	io.envelope      := count === 7.U
	io.sweeper       := count === 2.U || count === 6.U
}
