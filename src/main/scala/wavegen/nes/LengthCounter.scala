package wavegen.nes

import chisel3._
import chisel3.util._

class LengthCounter(channelID: Int) extends Module {
	override val desiredName = "NESLengthCounter"

	val io = IO(new Bundle {
		val ticks     = Input(Ticks())
		val write     = Input(Bool())
		val halt      = Input(Bool())
		val registers = Input(NESRegisters())
		val loadValue = Input(UInt(5.W))
		val out       = Output(UInt(8.W))
	})

	val enableLength  = io.registers.$4015(channelID - 1)
	val lengthTable   = VecInit(Seq(10, 254, 20, 2, 40, 4, 80, 6, 160, 8, 60, 10, 14, 12, 26, 14, 12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30).map(_.U(8.W)))
	val lengthCounter = RegInit(0.U(8.W))

	val writeReg = RegInit(false.B)

	when (io.write) {
		writeReg := true.B
	}

	when (io.ticks.half) {
		when (!enableLength) {
			lengthCounter := 0.U
		} .elsewhen (io.write || writeReg) {
			lengthCounter := lengthTable(io.loadValue)
			writeReg      := false.B
		} .elsewhen (!io.halt && lengthCounter =/= 0.U) {
			lengthCounter := lengthCounter - 1.U
		}
	}

	io.out := lengthCounter
}
