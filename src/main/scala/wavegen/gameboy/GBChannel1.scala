package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class GBChannel1(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {
		val dutyCycle    = Input(UInt(2.W))
		val lengthLoad   = Input(UInt(6.W))
		val volume       = Input(UInt(4.W))
		val volumeRising = Input(Bool())
		val period       = Input(UInt(3.W))
		val startFreq    = Input(UInt(11.W))
		val trigger      = Input(Bool())
		val lengthEnable = Input(Bool())
		val out          = Output(UInt(4.W))
	})

	val freq = Reg(UInt(8.W))

	val sweepClocker = Module(new Clocker)
	sweepClocker.io.enable := true.B
	sweepClocker.io.freq := 128.U

	val sweeper = Module(new GBSweeper)
	

	val clocker = Module(new Clocker)
	clocker.io.enable := true.B
	clocker.io.freq := freq
	
	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))
	
	val lengthTimer = RegInit(0.U(6.W))
}
